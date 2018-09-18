package com.example.smartsuitcase;


import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

public class controller {
	private int value;
	private int value_default;
	private int value_max;
	private boolean auto_default = true;
	
	private RectF location = new RectF(0,0,1,1);
	private RectF slide = new RectF(0,0,1,1);
	private RectF grip = new RectF(0,0,1,1);
	
	
	private boolean mVisible = true;
	private boolean horizontal = true;
	
	private Paint pSlideDefault;
	private Paint pGripDefault;

	private float maxX;
	private float minX;
	private float maxY;
	private float minY;
	private float gripsize;
	
	private int pointer_id = -1;
	
	private static final float grip_pct = 0.2f;
	
	public controller() {
		this(true);
	}
	
	public controller(boolean bHorizontal) {
		this(bHorizontal, 1000);
	}

	public controller(boolean bHorizontal, int iMaxValue) {
		this(bHorizontal, iMaxValue, 0);
	}
	
	public controller(boolean bHorizontal, int iMaxValue, int iDefaultValue) {
		this(bHorizontal, iMaxValue, iDefaultValue, false);
	}
	
	public controller(boolean bHorizontal, int iMaxValue, int iDefaultValue, boolean bAutoDefault) {
		pSlideDefault = new Paint();
		pSlideDefault.setColor(Color.RED);
		
		pGripDefault = new Paint();
		pGripDefault.setColor(Color.GREEN);
		
		
		horizontal = bHorizontal;
		auto_default = bAutoDefault;
		value_default = iDefaultValue;
		value_max = iMaxValue;
		
		value = value_default;
	}
	
	public int getValue() {
		return value;
	}
	
	public void setValue(int i) {
		value = i;
		this.setLocation(this.location);
	}
	
	public int getDefault() {
		return value_default;
	}
	
	public void setDefault(int d) {
		value_default = d;
	}

	public int getMaxValue() {
		return value_max;
	}
	
	public void setMaxValue(int m) {
		value_max = m;
	}
	
	public void addToScore(int i) {
		this.setValue(value + i);
	}
	
	public RectF getLocation() {
		return location;
	}
	
	public boolean isVisible() {
		return mVisible;
	}
	
	public void setVisible(boolean bVisible) {
		mVisible = bVisible;
	}
	
	public void setLocation(RectF loc) {
		this.location = new RectF(loc);
		float pct = value / (float)value_max;
		if (horizontal) {
			gripsize = loc.width() * grip_pct;
			minX = gripsize;
			maxX = loc.width() - gripsize;
			float pos = (maxX-minX) * pct + minX;
			slide = new RectF(loc.left, loc.top + gripsize, loc.right, loc.bottom - gripsize);
			grip = new RectF(pos - gripsize, loc.top, pos + gripsize, loc.bottom);
		}
		else {
			gripsize = loc.width() * grip_pct;
			minY = gripsize;
			maxY = loc.height() - gripsize;
			float pos = (maxY - minY)*pct + minY;
			slide = new RectF(loc.left + gripsize, loc.top, loc.right - gripsize, loc.bottom);
			grip = new RectF(loc.left, pos - gripsize, loc.right, pos + gripsize);
		}
	}
	
	public void setDefault() {
		if (auto_default) {
			this.setValue(value_default);
		}
	}
	
	public void onTouch(MotionEvent e) {
		if (!mVisible) return;
		final int action = e.getAction();
        switch (action & MotionEvent.ACTION_MASK) {        	
        	case MotionEvent.ACTION_DOWN: {
        		//This is when no fingers are touching and we get a 3
        		final float x = e.getX();
        		final float y = e.getY();
        		if (!this.hitTest(x, y)) return;
        		this.pointer_id = e.getPointerId(0);
        		this.setValue(this.valueFromPosition(x, y));
        		break;
        	}
        	case MotionEvent.ACTION_POINTER_DOWN: {
        		//when we have a finger touching and we get another one
        		//then assign the pointer ID if we're on the slider
        		for (int i=0; i<e.getPointerCount(); i++) {
        			int ptrId = e.getPointerId(i);
            		final float x = e.getX(ptrId);
            		final float y = e.getY(ptrId);        			
        			if (!this.hitTest(x, y)) continue;
        			this.pointer_id = ptrId;
             		this.setValue(this.valueFromPosition(x, y));
        		}
        		break;
        	}
        	case MotionEvent.ACTION_MOVE: {
        		//Any movement from any finger generates this event
        		//Loop through and move the correct slider or de-register
        		//the pointer ID if we moved off of the slider
        		for (int i=0; i<e.getPointerCount(); i++) {
        			if (this.pointer_id == e.getPointerId(i)) {
                		final float x = e.getX(this.pointer_id);
                		final float y = e.getY(this.pointer_id);
                		if (!this.hitTest(x, y)) {  //We've left the control
                			this.pointer_id = -1;
                			this.setDefault();
                		}
                		else {
                			this.setValue(this.valueFromPosition(x, y));
                		}
        			}
        		}
        		break;
        	}
        	case MotionEvent.ACTION_CANCEL:
        	case MotionEvent.ACTION_UP: {
        		//Last finger has come off the screen, de-register all pointer IDs
        		this.pointer_id = -1;
        		this.setDefault();
        		break;
        	}
        	case MotionEvent.ACTION_POINTER_UP: {
        		//A finger came off the screen, but there are others left
        		//If it was from this slider then de-register
        		
                // Extract the index of the pointer that left the touch sensor
                final int pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) 
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        		final int pointerId = e.getPointerId(pointerIndex);
        		if (this.pointer_id == pointerId) {
        			this.pointer_id = -1;
        			this.setDefault();
        		}
        		break;
        	}
        }
	}
	
    public boolean hitTest(float x, float y) {
    	return this.location.contains(x, y);
    }
    
    private int valueFromPosition(float posX, float posY) {
    	float pct = 0.0f;
    	if (horizontal) {
    		if (posX < minX) posX = minX;
    		if (posX > maxX) posX = maxX;
    		pct = (posY - minX)/(maxX-minX);
    		//pct = (posX - this.location.left)/this.location.width();
    	}
    	else {
    		if (posY < minY) posY = minY;
    		if (posY > maxY) posY = maxY;
    		pct = (posY - minY)/(maxY-minY);
    		//pct = (posY - this.location.top) / this.location.height();
    	}
    	return (int)(pct*value_max);
    }
    
	public void draw(Canvas c) {
		if (mVisible) {
			c.drawRect(slide, pSlideDefault);
			c.drawRect(grip, pGripDefault);
			c.drawCircle(100, 100, 5, pSlideDefault);
		}
	}
	
}
