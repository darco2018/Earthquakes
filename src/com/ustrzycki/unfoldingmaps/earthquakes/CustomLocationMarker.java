package com.ustrzycki.unfoldingmaps.earthquakes;

import de.fhpotsdam.unfolding.geo.Location;
import processing.core.PApplet;
import processing.core.PGraphics;

public class CustomLocationMarker extends CommonMarker{

	static PApplet obj = new PApplet();
	public static final int RED = obj.color(255, 0, 0); // Processing's color method to generate an int that represents a color.  
	public static final int BROWN = obj.color(171, 70, 7);
	public static final int ORANGE = obj.color(255, 153, 0);
	public static final int YELLOW = obj.color(255, 255, 0);
	public static final int BLUE = obj.color(0, 0, 255);
	public static final int WHITE = obj.color(255, 255, 255);
	public static final int BLACK = obj.color(0, 0, 0);
	public static final int TITLE_BOX_YELLOW = obj.color(255, 230, 153);
	
	public static int REC_SIZE = 5;

	// Records whether this marker has been clicked (most recently)
	protected boolean clicked = false;
	
	public CustomLocationMarker(Location location) {
		super(location);
	}
	
	public CustomLocationMarker(Location location, java.util.HashMap<java.lang.String,java.lang.Object> properties) {
		super(location, properties);
	}
	
	// Getter method for clicked field
	@Override
	public boolean isClicked() {
		return clicked;
	}
	
	// Setter method for clicked field
	@Override
	public void setClicked(boolean state) {
		clicked = state;
	}
	
	// Common piece of drawing method for markers; 
	// Note that you should implement this by making calls 
	// drawMarker and showTitle, which are abstract methods 
	// implemented in subclasses
	@Override
	public void draw(PGraphics pg, float x, float y) {
		// For starter code just drawMarker(...)
		if (!hidden) {
			drawMarker(pg, x, y);
			if (selected) {
				showTitle(pg, x, y);  // You will implement this in the subclasses
			}
		}
	}
	@Override
	public void drawMarker(PGraphics pg, float x, float y){
		
		// Save previous drawing style
				pg.pushStyle();
				
				// IMPLEMENT: drawing rect for the custom location
				pg.fill(ORANGE);
															//pg.triangle(x, y-REC_SIZE, x-REC_SIZE, y+REC_SIZE, x+REC_SIZE, y+REC_SIZE);
				pg.rect(x-radius, y-radius, 2*radius, 2*radius);
				
				// Restore previous drawing style
				pg.popStyle();
		
	};
	@Override
	public void showTitle(PGraphics pg, float x, float y){
		
	};
}
