package com.example.smartsuitcase;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
//import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
//import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.LinearLayout;
import android.widget.Toast;

import com.examples.bluetooth.BluetoothCommandService;
import com.examples.bluetooth.DeviceList;

public class MSP430 extends Activity {
	private mspview view;
	
	// Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
	// Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Blue tooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Blue tooth Command Service
    private BluetoothCommandService mCommandService = null;
    

    
    
    // Called when the activity is first created. 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Set full screen, no title:
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        		WindowManager.LayoutParams.FLAG_FULLSCREEN);
       
        
	
        
        view = new mspview(this);
        view.setEnabled(false);
        setContentView(view);
        //buttonView=view.inflate(this, R.layout.main, null);
        // Get local Blue tooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported so app doesn't work
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    
    
	@Override
	protected void onStart() {
		super.onStart();
		// If Bluetooth is not on then request that it be enabled.
        // setupCommand() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		}
		// or set up the command service
		else {
			if (mCommandService==null)
				setupCommand();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
		if (mCommandService != null) {
			if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
				mCommandService.start();
			}
		}
	}
    
	private void setupCommand() {
		// Initialize the BluetoothCommandService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
	}
    
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mCommandService != null)
			mCommandService.stop();
	}
    
	//control loop for bluetooth commands
	class ControlLooper implements Runnable {
		@Override
		public void run() {
			while (mCommandService.getState() == BluetoothCommandService.STATE_CONNECTED) {
				
			        
				
				float left = view.getLeftThrottleDutyCycle();
				float right = view.getRightThrottleDutyCycle();
				
				boolean fwdL = view.getLeftThrottleFwd();
				boolean fwdR = view.getRightThrottleFwd();
				
				int pwm_dc_left = (int)Math.round(left*32.0f); //5 bit resolution
				int pwm_dc_right = (int)Math.round(right*32.0f); //5 bit resolution
				//byte detect1=(byte)(detect & 0x80);
				byte bLeft = (byte)(pwm_dc_left & 0x1F);
				bLeft |= 0x40;
				if (fwdL) bLeft |= 0x20;
				
				byte bRight = (byte)(pwm_dc_right & 0x1F);
				if (fwdR) bRight |= 0x20;

				Log.d("controlLoop", "bLeft:" + bLeft);
				mCommandService.write(bLeft);
				
				try {Thread.sleep(20);}
				catch (Exception ignore) {}
				
				Log.d("controlLoop", "bRight:" + bRight);
				mCommandService.write(bRight);

				try {Thread.sleep(20);}
				catch (Exception ignore) {}
				
				//Log.d("controlLoop", "detect1:" + detect1);
				//mCommandService.write(detect1);
				
				//try {Thread.sleep(20);}
				//catch (Exception ignore) {}
			}
			
		}
	}
	
	private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
	}
	
	// The Handler gets information back from the BluetoothCommandService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	
            switch (msg.what) {
            case BluetoothCommandService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothCommandService.STATE_CONNECTED:
                	enableUi(true);
                	(new Thread(new ControlLooper())).start();
                    break;
                case BluetoothCommandService.STATE_CONNECTING:
                    break;
                case BluetoothCommandService.STATE_LISTEN:
                case BluetoothCommandService.STATE_NONE:
                    break;
                }
                break;
            case BluetoothCommandService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(BluetoothCommandService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                
                break;
            case BluetoothCommandService.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothCommandService.TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
            
        }
    };
    
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device then connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceList.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice 
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                
                
                // Attempt to connect to the device
                mCommandService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is enabled now, so set up a chat session
                setupCommand();
            } else {
                // error occured
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu u) {
		MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu,u);
        return true;
	}

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceList to see devices and do scan
        	Intent serverIntent = new Intent(this, DeviceList.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
    
	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				
				view.setEnabled(enable);
				//getWindow().addContentView(buttonView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
		     //             ViewGroup.LayoutParams.FILL_PARENT  ));
			}
		});
	}
	
	
}