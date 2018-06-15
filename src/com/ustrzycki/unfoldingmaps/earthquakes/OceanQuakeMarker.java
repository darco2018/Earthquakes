package com.ustrzycki.unfoldingmaps.earthquakes;

import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.utils.ScreenPosition;
import processing.core.PGraphics;

/** Implements a visual marker for ocean earthquakes on an earthquake map
 * 
 * @author UC San Diego Intermediate Software Development MOOC team
 * @author Your name here
 *
 */



public class OceanQuakeMarker extends EarthquakeMarker {
	
	int counter = 0;
	
	public OceanQuakeMarker(PointFeature quake) {
		super(quake);
		
		// setting field in earthquake marker
		isOnLand = false;
		
	}
	

	/** Draw the earthquake as a square */
	@Override
	public void drawEarthquake(PGraphics pg, float x, float y) {
		pg.rect(x-radius, y-radius, 2*radius, 2*radius);
		
		if (isClicked())
			drawLinesToCities(pg,  x, y);
		
	}
	
	private void drawLinesToCities(PGraphics pg, float x, float y){
		
		for(CityMarker city : threatenedCities){
			
			ScreenPosition position = city.getScreenPosition(getMarkersMap());
			
			pg.strokeWeight(2);			
			pg.line(x, y, position.x - 200, position.y - 50 );
			
		}
				pg.strokeWeight(0);		
	}
	
	
}
