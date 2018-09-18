package com.example.smartsuitcase;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;

public class mspview extends SurfaceView implements SurfaceHolder.Callback {
	private boolean bInited = false;
	private ControlThread mThread;
	private Background background;
	private controller throttleL;
	private controller throttleR;
	private setup statusL;
	private setup statusR;
	//private Button buttonf;
	 
	private boolean doDraw = false;
	
    public mspview(Context context) {
        super(context);
     
        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        //Create our sliders controller
        throttleL = new controller(false, 1000, 500, true);
        throttleR = new controller(false, 1000, 500, true);
        
        
        //Create a paint and the status message
		Paint p = new Paint();
		p.setColor(Color.BLUE);
		p.setShadowLayer(5, 2, 2, Color.BLACK);
        statusL = new setup("Unconnected to Device", p);
        statusR = new setup("Unconnected to Device", p);
        
        //Create the control thread 
        mThread = new ControlThread(this);
        //Button buttonf= (Button)findViewById(R.id.button1);
        
        // ensure get key events
        setFocusable(true);
	
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	throttleL.onTouch(event);
    	throttleR.onTouch(event);
        return true;
    }
    
    public boolean getLeftThrottleFwd() {
    	return (throttleL.getValue() <= throttleL.getDefault());
    }
    
    public float getLeftThrottleDutyCycle() {
    	float pct = (float)throttleL.getValue() / (float)throttleL.getMaxValue();
    	return Math.abs(pct - 0.5f) * 2;
    }
    
    public boolean getRightThrottleFwd() {
    	return (throttleR.getValue() <= throttleR.getDefault());
    }
    
    public float getRightThrottleDutyCycle() {
    	float pct = (float)throttleR.getValue() / (float)throttleR.getMaxValue();
    	return Math.abs(pct - 0.5f) * 2; 
    }
    
    public void setEnabled(boolean b) {
		throttleL.setVisible(b);
		throttleR.setVisible(b);
    }
    ///////////////////////////////////////////////////
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.d("MSP430_CONTROL_VIEW", "Surface changed!");
    	if (!bInited) {
    		Log.d("MSP430_CONTROL_VIEW", "initing...");
			//load background
	        background = new Background(getResources(), R.drawable.background, this);
	        
	        //use 20% of the screen for the throttle:
	        int pos = (int)(.2*width);
	        
	        //Place our sliders and status message
	        throttleL.setLocation(new RectF(0, 0, pos, height/2));
	        throttleR.setLocation(new RectF(width-pos, 0, width, height/2));
	        statusL.setLocation(new Rect(pos + 10, 0, width-pos-10, height/2));
	        statusR.setLocation(new Rect(pos + 10, height/2, width-pos-10, height));
	        statusR.setValue("Click points button");
	        bInited = true;
    	}
	    if (!mThread.isAlive()) {
	    	startNewThread();
	    }
        doDraw = true;
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("MSP430_CONTROL_VIEW", "Surface created!");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("MSP430_CONTROL_VIEW", "Surface destroyed!");
		doDraw = false;
		if (mThread.isAlive()) {
			mThread.setRunning(false);
	    }
	}
	
	private void startNewThread() {
        mThread = new ControlThread(this);
        mThread.setRunning(true);
        mThread.start();
	}

	//control loop
	class ControlThread extends Thread {
		private boolean mRun = false;
		private SurfaceHolder mSurfaceHolder;
		
		public ControlThread(SurfaceView view) {
			mSurfaceHolder = view.getHolder();
		}
		
		public void setRunning(boolean bRunning) {
			mRun = bRunning;
		}

        @Override
        public void run() {
        	Log.d("MSP430_CONTROL_VIEW_THREAD", "Starting...");
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                    	if (doDraw) {
                    		background.draw(c);
                    		throttleL.draw(c);
                    		throttleR.draw(c);
                    		if (throttleL.isVisible()) {
                    			String pctR =  (int)(getRightThrottleDutyCycle() * 100) + "%";
                    			String pctL =  (int)(getLeftThrottleDutyCycle() * 100) + "%";
                    			statusL.setValue("Left: " + (getLeftThrottleFwd() ? "FWD" : "REV") + " @ " + pctL);
                    			statusR.setValue("Right: " + (getRightThrottleFwd() ? "FWD" : "REV") + " @ " + pctR);
                    		}
                    		statusL.draw(c);
                    		statusR.draw(c);
                    	}
                    }
                }
                catch (Exception ignore) {
                	Log.d("DRAW", ignore.toString());
                }
                finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
            Log.d("MSP430_TANK_CONTROL_VIEW_THREAD", "Stopping...");
        }
	}


	
	
}

