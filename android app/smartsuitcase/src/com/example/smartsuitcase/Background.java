package com.example.smartsuitcase;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.SurfaceView;

public class Background {
	private Bitmap bg;
	private Rect bg_frame;
	private Rect dest_frame;

	
	public Background(Resources res, int drawable, SurfaceView sv) {
		bg = BitmapFactory.decodeResource(res, drawable);
		bg_frame = new Rect(0,0,bg.getWidth(), bg.getHeight());
		dest_frame = new Rect(0,0,sv.getWidth(), sv.getHeight());

	}
	
	public void cleanup() {
		bg.recycle();
		bg = null;
	}
	
	public void draw(Canvas c) {
		// draw a new background and two rectangle for slide control
		c.drawBitmap(bg, bg_frame, dest_frame, null);
		
	}
}
