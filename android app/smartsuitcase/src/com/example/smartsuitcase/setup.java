package com.example.smartsuitcase;



import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;

public class setup {
	private String text;
	private Paint p;
	private Rect location;
	private int mX = 0;
	private int mY = 0;
	private boolean bDirty = true;
	private String max_text;
	
	public setup(String sText, Paint pen) {
		this(sText, pen, sText);
	}
	
	public setup(String sText, Paint pen, String sMaxText) {
		text = sText;
		max_text = sMaxText;
		p = new Paint(pen);
		p.setTextAlign(Align.CENTER);
	}
	
	public void setValue(String sText) {
		text = sText;
		bDirty = true;
	}
	
	public void draw(Canvas c) {
		if (bDirty) {
			Rect bounds = new Rect(0,0,1,1);
			p.getTextBounds(text, 0, text.length(), bounds);
			mY = location.top + (location.height() + bounds.height()) / 2;
			//Log.d("LABEL_DRAW_DIRTY", "Text: " + text + "mX=" + mX + ", mY=" + mY);
			bDirty = false;
		}
		c.drawText(text, mX, mY, p);
	}
	
	public void setLocation(Rect loc) {
		location = new Rect(loc);
		fitToRect();
	}
	
	public void setColor(int iColor) {
		p.setColor(iColor);
	}
	
	private void fitToRect() {
		int iFontSize = 1;
		int h = Integer.MIN_VALUE;
		int w = Integer.MIN_VALUE;
		Rect bounds = new Rect(0,0,1,1);
		while ((h < location.height()) && (w < location.width())) {
			iFontSize++;
			p.setTextSize(iFontSize);
			p.getTextBounds(max_text, 0, max_text.length(), bounds);
			h = bounds.height();
			w = bounds.width();
		}
		p.setTextSize(iFontSize-1);
		p.getTextBounds(max_text, 0, max_text.length(), bounds);
		mX = location.centerX();
		mY = location.top + (location.height() + bounds.height()) / 2;
	}
	
}
