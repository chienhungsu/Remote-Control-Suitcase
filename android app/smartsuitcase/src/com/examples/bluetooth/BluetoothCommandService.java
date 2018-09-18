package com.examples.bluetooth;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

	public class BluetoothCommandService {
		// Debugging
	    private static final String TAG = "BluetoothCommandService";
	    private static final boolean D = true;

	    // Unique UUID for this application
	    //private static final UUID MY_UUID = UUID.fromString("04c6093b-0000-1000-8000-00805f9b34fb");
	    
	    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

	    private final BluetoothAdapter mAdapter;
	    private final Handler mHandler;
	    private ConnectThread mConnectThread;
	    private ConnectedThread mConnectedThread;
	    private int mState;

	    
	    // Constants that indicate the current connection state
	    public static final int STATE_NONE = 0;       // none
	    public static final int STATE_LISTEN = 1;     // listening for connections
	    public static final int STATE_CONNECTING = 2; // initiating an outgoing connection
	    public static final int STATE_CONNECTED = 3;  // connect to a remote device
	    
	    // Constants that indicate command to computer
	    public static final int EXIT_CMD = -1;
	    public static final int VOL_UP = 1;
	    public static final int VOL_DOWN = 2;
	    public static final int MOUSE_MOVE = 3;
	    
	    
	    // Message types sent from the BluetoothChatService Handler
	    public static final int MESSAGE_STATE_CHANGE = 1;
	    public static final int MESSAGE_READ = 2;
	    public static final int MESSAGE_WRITE = 3;
	    public static final int MESSAGE_DEVICE_NAME = 4;
	    public static final int MESSAGE_TOAST = 5;
	    
	    
	    // Key names received from the BluetoothCommandService Handler
	    public static final String DEVICE_NAME = "device_name";
	    public static final String TOAST = "toast";
	    
	    
	    //Prepares a new BluetoothChat session.
	     //The UI Activity Context
	     // Handler send messages back to the UI Activity
	     
	    public BluetoothCommandService(Context context, Handler handler) {
	    	mAdapter = BluetoothAdapter.getDefaultAdapter();
	    	mState = STATE_NONE;
	    	//mConnectionLostCount = 0;
	    	mHandler = handler;
	    }
	    
	    
	     //Set the current state of the chat connection
	     //define the current connection state
	     
	    private synchronized void setState(int state) {
	        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
	        mState = state;

	        // Give the new state to the Handler so the UI Activity can update
	        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
	    }

	    
	     //Return the current connection state
	    public synchronized int getState() {
	        return mState;
	    }
	    
	    
	     //Start the chat service
	      //session in listening (server) mode. Called by onResume() 
	    public synchronized void start() {
	        if (D) Log.d(TAG, "start");

	        // Cancel any thread attempting to make a connection
	        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

	        // Cancel any thread currently running a connection
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	        setState(STATE_LISTEN);
	    }
	    
	    
	    //Start the ConnectThread to initiate a connection for device.
	    // The BluetoothDevice to connect
	     
	    public synchronized void connect(BluetoothDevice device) {
	    	if (D) Log.d(TAG, "connect to: " + device);

	        // Cancel any thread attempting to make a connection
	        if (mState == STATE_CONNECTING) {
	            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
	        }

	        // Cancel any thread currently running a connection
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	        // Start the thread to connect with the given device
	        mConnectThread = new ConnectThread(device);
	        mConnectThread.start();
	        setState(STATE_CONNECTING);
	    }
	    
	    
	     // Start the ConnectedThread to begin a Bluetooth connection
	    // The BluetoothSocket's connection was made
	     //The BluetoothDevice has been connected
	     
	    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
	        if (D) Log.d(TAG, "connected");
	        
	        // Cancel the thread that completed the connection
	        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

	        // Cancel any thread currently running a connection
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

	        // Start the thread to manage the connection and perform transmissions
	        mConnectedThread = new ConnectedThread(socket);
	        mConnectedThread.start();

	        // Send the name of the connected device back to the UI Activity
	        Message msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME);
	        Bundle bundle = new Bundle();
	        bundle.putString(DEVICE_NAME, device.getName());
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
        
	        setState(STATE_CONNECTED);
	    }
	    // stop thread
	    public synchronized void stop() {
	        if (D) Log.d(TAG, "stop");
	        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
	        
	        setState(STATE_NONE);
	    }
	    
	    
	     //Write to the ConnectedThread in an unsynchronized manner
	     // The bytes to write
	     // ConnectedThread#write(byte[])
	     
	    public void write(byte[] out) {
	        // Create temporary object
	        ConnectedThread r;
	        // Synchronize a copy of the ConnectedThread
	        synchronized (this) {
	            if (mState != STATE_CONNECTED) return;
	            r = mConnectedThread;
	        }
	        // Perform the write unsynchronized
	        r.write(out);
	    }
	    
	    public void write(int out) {
	    	// Create temporary object
	        ConnectedThread r;
	        // Synchronize a copy of the ConnectedThread
	        synchronized (this) {
	            if (mState != STATE_CONNECTED) return;
	            r = mConnectedThread;
	        }
	        // Perform the write unsynchronized
	        r.write(out);
	    }
	    
	    /**
	     * Indicate that the connection attempt failed and notify the UI Activity.
	     */
	    private void connectionFailed() {
	        setState(STATE_LISTEN);

	        // Send a failure message back to the Activity
	        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
	        Bundle bundle = new Bundle();
	        bundle.putString(TOAST, "Unable to connect device");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
	    }

	    
	     // Indicate that the connection was lost and notify the UI Activity.
	     
	    private void connectionLost() {

	        	setState(STATE_LISTEN);
		        // Send a failure message back to the Activity
		        Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
		        Bundle bundle = new Bundle();
		        bundle.putString(TOAST, "Device connection was lost");
		        msg.setData(bundle);
		        mHandler.sendMessage(msg);
//	        }
	    }
	    
	    private class ConnectThread extends Thread {
	        private final BluetoothSocket mmSocket;
	        private final BluetoothDevice mmDevice;

	        public ConnectThread(BluetoothDevice device) {
	            mmDevice = device;
	            BluetoothSocket tmp = null;

	            // Get a BluetoothSocket for a connection with the
	            // given BluetoothDevice
	            try {
	                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	                //tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
	            } catch (IOException e) {
	                Log.e(TAG, "create() failed", e);
	            }
	            mmSocket = tmp;
	        }

	        public void run() {
	            Log.i(TAG, "BEGIN mConnectThread");
	            setName("ConnectThread");

	            // Always cancel discovery because it will slow down a connection
	            mAdapter.cancelDiscovery();

	            // Send a failure message back to the Activity
	            Message msg = mHandler.obtainMessage(MESSAGE_TOAST);
	            String ss = "Attempting connection to " + mmSocket.getRemoteDevice().getName();
	            Bundle bundle = new Bundle();
	            bundle.putString(TOAST, ss);
	            msg.setData(bundle);
	            mHandler.sendMessage(msg);
	            
	            // Make a connection to the BluetoothSocket
	            try {
	                // This is a blocking call and will only return on a
	                // successful connection or an exception
	                mmSocket.connect();
	            } catch (IOException e) {
	            	
	            	connectionFailed();
	                // Close the socket
	                try {
	                    mmSocket.close();
	                } catch (IOException e2) {
	                    Log.e(TAG, "unable to close() socket during connection failure", e2);
	                }
	                // Start the service over to restart listening mode
	                BluetoothCommandService.this.start();
	                return;
	            }
	            
	            
	            // Reset the ConnectThread
	            synchronized (BluetoothCommandService.this) {
	                mConnectThread = null;
	            }

	            // Start the connected thread
	            connected(mmSocket, mmDevice);
	        }

	        public void cancel() {
	            try {
	                mmSocket.close();
	            } catch (IOException e) {
	                Log.e(TAG, "close() of connect socket failed", e);
	            }
	        }
	    }

	   
	     //thread runs during a connection with a remote device.
	     //handles incoming and outgoing transmissions.
	     
	    private class ConnectedThread extends Thread {
	        private final BluetoothSocket mmSocket;
	        private final InputStream mmInStream;
	        private final OutputStream mmOutStream;

	        public ConnectedThread(BluetoothSocket socket) {
	            Log.d(TAG, "create ConnectedThread");
	            mmSocket = socket;
	            InputStream tmpIn = null;
	            OutputStream tmpOut = null;

	            // Get the BluetoothSocket input and output streams
	            try {
	                tmpIn = socket.getInputStream();
	                tmpOut = socket.getOutputStream();
	            } catch (IOException e) {
	                Log.e(TAG, "temp sockets not created", e);
	            }

	            mmInStream = tmpIn;
	            mmOutStream = tmpOut;
	        }

	        public void run() {
	            Log.i(TAG, "BEGIN mConnectedThread");
	            byte[] buffer = new byte[1024];
	            
	            // Keep listening to the InputStream while connected
	            while (true) {
	                try {
	                	// Read from the InputStream
	                    int bytes = mmInStream.read(buffer);

	                    // Send the obtained bytes to the UI Activity
	                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                            .sendToTarget();
	                } catch (IOException e) {
	                    Log.e(TAG, "disconnected", e);
	                    connectionLost();
	                    break;
	                }
	            }
	        }

	        //write to output stream
	        public void write(byte[] buffer) {
	            try {
	                mmOutStream.write(buffer);
	              
	            } catch (IOException e) {
	                Log.e(TAG, "Exception during write", e);
	            }
	        }
	        
	        public void write(int out) {
	        	try {
	                mmOutStream.write(out);
	            } catch (IOException e) {
	                Log.e(TAG, "Exception during write", e);
	            }
	        }

	        public void cancel() {
	            try {
	            	mmOutStream.write(EXIT_CMD);
	                mmSocket.close();
	            } catch (IOException e) {
	                Log.e(TAG, "close() of connect socket failed", e);
	            }
	        }
	    }
	}