package com.ustrzycki.unfoldingmaps.earthquakes;


import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.GeoJSONReader;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.geo.Location;
import de.fhpotsdam.unfolding.marker.AbstractShapeMarker;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.MultiMarker;
import de.fhpotsdam.unfolding.providers.Google;
import de.fhpotsdam.unfolding.providers.MBTilesMapProvider;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.utils.ScreenPosition;

import parsing.ParseFeed;
import processing.core.PApplet;

/**
 * EarthquakeCityMap An application with an interactive map displaying
 * earthquake data. Author: UC San Diego Intermediate Software Development MOOC
 * team
 * 
 * @author DariuszUstrzycki Date: October, 2015
 */

public class EarthquakeCityMap extends PApplet {

	// You can ignore this - It's to get rid of eclipse warnings
	private static final long serialVersionUID = 1L;

	// IF YOU ARE WORKING OFFILINE, change the value of this variable to true
	private static final boolean offline = false;

	/**
	 * This is where to find the local tiles, for working without an Internet
	 * connection
	 */
	public static String mbTilesString = "blankLight-1-3.mbtiles";

	// feed with magnitude 2.5+ Earthquakes
	private String earthquakesURL = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_week.atom";

	// The files containing city and country names with appropriate info
	private String cityFile = "city-data.json";
	private String countryFile = "countries-geo.json";

	// The map of the world
	private UnfoldingMap map;

	// Markers for each city, earthquake and country
	private List<Marker> cityMarkers;
	private List<Marker> quakeMarkers;
	private List<Marker> countryMarkers;

	// user's location
	private CustomLocationMarker userLocationMarker;

	private enum Mode {
		DEFAULT, CUSTOM_LOCATION, HISTORICAL
	};

	private Mode mapMode = Mode.DEFAULT;

	private CommonMarker lastSelected;
	private CommonMarker lastClicked;

	@Override
	public void setup() {
		// (1) Initializing canvas and map tiles
		size(900, 700, OPENGL);
		if (offline) {
			map = new UnfoldingMap(this, 100, 50, 650, 600, new MBTilesMapProvider(mbTilesString));
			earthquakesURL = "2.5_week.atom"; // The same feed, but saved August
												// 7, 2015
		} else {
			map = new UnfoldingMap(this, 178, 50, 650, 600, new Google.GoogleMapProvider());
		}

		MapUtils.createDefaultEventDispatcher(this, map); // creates an event
															// handler

		// (2) Reading in earthquake data and geometric properties
		// STEP 1: load country features and markers
		List<Feature> countries = GeoJSONReader.loadData(this, countryFile);
		countryMarkers = MapUtils.createSimpleMarkers(countries);

		// STEP 2: read in city data
		List<Feature> cities = GeoJSONReader.loadData(this, cityFile);
		cityMarkers = new ArrayList<Marker>();
		for (Feature city : cities) {
			cityMarkers.add(new CityMarker(city));
		}

		// STEP 3: read in earthquake RSS feed
		List<PointFeature> earthquakes = ParseFeed.parseEarthquake(this, earthquakesURL);
		quakeMarkers = new ArrayList<Marker>();

		for (PointFeature feature : earthquakes) {
			// check if LandQuake
			if (isLand(feature)) {
				quakeMarkers.add(new LandQuakeMarker(feature));
			}
			// OceanQuakes
			else {
				quakeMarkers.add(new OceanQuakeMarker(feature));
			}
		}

		// could be used for debugging
		printQuakes();

		// (3) Add markers to map
		// NOTE: Country markers are not added to the map. They are used
		// for their geometric properties

		map.addMarkers(quakeMarkers);
		map.addMarkers(cityMarkers);

		sortAndPrint(100);

	} // End setup

	@Override
	public void draw() {
		background(0);
		map.draw();
		addKey();
		addNearestQuakeMenu("Click this button to", "set your location and", "see the nearest quake");
	}

	private void sortAndPrint(int numToPrint) {

		System.out.println("The largest eartquakes by sortAndPrint method: ");

		// 1. creates a new array from the list of earthquake markers
		Object[] arr = quakeMarkers.toArray(); // returns the elements in the
												// List as an array of Objects

		// 2. sort the array of earthquake markers in reverse order of their
		// magnitude (highest to lowest)
		// sort in the natural ordering
		Arrays.sort(arr);

		// convert the array back into a list
		List<Object> newQuakesList = Arrays.asList(arr);
		// reverse the list
		Collections.reverse(newQuakesList);

		// print out the top numToPrint earthquakes
		// if number of quakes is smaller than numToPrint, just print all the
		// earthquakes

		int counter = 1;
		for (Object quake : newQuakesList) {

			if (counter <= numToPrint)
				System.out.println(((EarthquakeMarker) quake).getTitle());

			counter++;
		}
	}

	/**
	 * Event handler that gets called automatically when the mouse moves.
	 */
	@Override
	public void mouseMoved() {

		// clear the last selection immediately after mouse leaves a marker's
		// area
		if (lastSelected != null) { // lastSelected is a private CommonMarker
			lastSelected.setSelected(false);
			lastSelected = null;
		}

		selectMarkerIfHover(quakeMarkers);
		selectMarkerIfHover(cityMarkers);
	}

	// If there is a marker under the cursor, and lastSelected is null
	// set the lastSelected to be the first marker found under the cursor
	// Make sure you do not select two markers.
	//
	private void selectMarkerIfHover(List<Marker> markers) {

		for (Marker marker : markers) {

			if (marker.isInside(map, mouseX, mouseY)) {

				if (lastSelected != null)
					lastSelected.setSelected(false); // remove the selection
														// from the previously
														// selected marker in
														// the city/quake
														// markers

				marker.setSelected(true);
				lastSelected = (CommonMarker) marker;
				break;
			}
		}
	}

	private boolean clickOnCusLocButton() {

		boolean isInsideButton = false;

		if (mouseX > 80 && mouseX < 115 && mouseY > 370 && mouseY < 405) {

			// System.out.println("mouseX: " + mouseX + "mouseY: " + mouseY );
			isInsideButton = true;
		} else
			isInsideButton = false;

		System.out.println("isInsideButton: " + isInsideButton);

		return isInsideButton;
	}

	private void showOnlyOneMarker(Marker markerToShow, ScreenPosition position) {

		hideMarkers();

		Location loc = map.getLocation(position);
		markerToShow.setLocation(loc);
		markerToShow.setHidden(false);
		map.addMarker(userLocationMarker);

	}

	/**
	 * The event handler for mouse clicks It will display an earthquake and its
	 * threat circle of cities Or if a city is clicked, it will display all the
	 * earthquakes where the city is in the threat circle
	 */
	@Override
	public void mouseClicked() {
		System.out.println("\nCLICK!!!");

		boolean foundNewSelection = false;

		// look for a new selection only after unhiding took place >>
		// lastClicked = null;
		if (lastClicked == null) {
			foundNewSelection = checkMarkersForClick(cityMarkers);

			if (!foundNewSelection)
				foundNewSelection = checkMarkersForClick(quakeMarkers);
		}

		if (foundNewSelection) {
			hideMarkers();
		} else {

			if (lastClicked != null) {
				unhideAllMarkers();
				// after unhiding remove clickSelection from the marker and from
				// the lastClicked
				lastClicked.setClicked(false);
				lastClicked = null;
			}
		}

		/////////////////////////// SELECT CUSTOM LOCATION IF THE APPROPRIATE
		/////////////////////////// BUTTON IS CLICKED/////////////////////

		if (clickOnCusLocButton() && (mapMode != Mode.CUSTOM_LOCATION)) {
			System.out.println("Setting CUSTOM_LOCATION");
			System.out.println("Calling nearestQuake() method 1st time");
			mapMode = Mode.CUSTOM_LOCATION;
			quakeNearCustomLocation();

		} else if (mapMode == Mode.CUSTOM_LOCATION && !clickOnCusLocButton()) {
			System.out.println("Calling nearestQuake() method again");
			quakeNearCustomLocation();
		}

	}

	private void quakeNearCustomLocation() {

		// if click inside button and custom marker is null, intilize custom
		// marker
		if (userLocationMarker == null) {
			System.out.println("Initializing userLocationMarker");
			userLocationMarker = new CustomLocationMarker(new Location(0, 0));
			lastClicked = userLocationMarker;
		} else {
			// if click in NOT inside the button and marker is not visible, show
			// it on the map hiding other markers
			if (userLocationMarker.isHidden()) {
				System.out.println("Showing the marker");
				showOnlyOneMarker(userLocationMarker, new ScreenPosition(mouseX, mouseY));

				distanceToCustomLocation(userLocationMarker);
			} else {
				unhideAllMarkers();
				userLocationMarker = null;
				mapMode = Mode.DEFAULT;
				System.out.println("Exiting Mode.CUSTOM_LOCATION");
			}
		}
	}

	private boolean checkMarkersForClick(List<Marker> markers) {

		for (Marker marker : markers) {

			if (marker.isInside(map, mouseX, mouseY)) {

				System.out.println("This click is inside a marker.");

				if (clickOnPreviousSelection(marker)) {
					return false;
				} else {
					((CommonMarker) marker).setClicked(true);
					lastClicked = (CommonMarker) marker;
					return true;
				}
			}
		}

		return false; // no new marker has been selected
	}

	private boolean clickOnPreviousSelection(Marker marker) {

		boolean previousButtonClicked = false;

		if (lastClicked != null && marker == lastClicked) {
			previousButtonClicked = true;
		} else {
			previousButtonClicked = false;
		}

		System.out.println("The previous button has been clicked: " + previousButtonClicked);
		return previousButtonClicked;

	}

	// loop over and unhide all markers
	private void unhideAllMarkers() {
		for (Marker marker : quakeMarkers) {
			marker.setHidden(false);
		}

		for (Marker marker : cityMarkers) {
			marker.setHidden(false);
		}

		if (userLocationMarker != null)
			userLocationMarker.setHidden(true);
	}

	private void hideMarkers() {

		for (Marker marker : quakeMarkers) {
			if (!(((CommonMarker) marker) == lastClicked)) { // don't hide the
																// selected
																// button
				marker.setHidden(true);
			}

			if (lastClicked instanceof CityMarker) {
				leaveThreateningQuakesVisible(marker);
			}
		}

		for (Marker marker : cityMarkers) {
			if (!(((CommonMarker) marker) == lastClicked)) { // don't hide the
																// selected
																// marker
				marker.setHidden(true);
			}

			if (lastClicked instanceof EarthquakeMarker) {
				leaveThreatenedCitiesVisible(marker);

				if (lastClicked instanceof OceanQuakeMarker) {
					addCityToThreatenedCities(marker);
				}
			}
		}
	}

	private void leaveThreatenedCitiesVisible(Marker cityMarker) {
		if ((distanceToClickedMarker(cityMarker) <= threatCircleRadius((EarthquakeMarker) lastClicked))) {
			cityMarker.setHidden(false);
		}
	}

	private void leaveThreateningQuakesVisible(Marker quakeMarker) {
		if ((distanceToClickedMarker(quakeMarker) <= threatCircleRadius((EarthquakeMarker) quakeMarker))) {
			quakeMarker.setHidden(false);
		}
	}

	private void addCityToThreatenedCities(Marker cityMarker) {

		if ((distanceToClickedMarker(cityMarker) <= threatCircleRadius((EarthquakeMarker) lastClicked))) {

			((OceanQuakeMarker) lastClicked).setMarkersMap(map);
			((OceanQuakeMarker) lastClicked).addThreatenedCity((CityMarker) cityMarker);
		}
	}

	private double threatCircleRadius(EarthquakeMarker marker) {
		return marker.threatCircle();
	}

	/**
	 * @param a
	 *            marker
	 * @return the distance from the given marker to the last clicked marker
	 */
	private double distanceToClickedMarker(Marker marker) {
		return marker.getDistanceTo(lastClicked.getLocation());
	}

	// helper method to draw key in GUI
	private void addKey() {
		// you can use Processing's graphics methods here
		fill(255, 250, 240);

		int xbase = 25;
		int ybase = 50;

		rect(xbase, ybase, 150, 250);

		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Earthquake Key", xbase + 25, ybase + 25);

		fill(150, 30, 30);
		int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase - CityMarker.TRI_SIZE, tri_xbase - CityMarker.TRI_SIZE,
				tri_ybase + CityMarker.TRI_SIZE, tri_xbase + CityMarker.TRI_SIZE, tri_ybase + CityMarker.TRI_SIZE);

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
		text("City Marker", tri_xbase + 15, tri_ybase);

		text("Land Quake", xbase + 50, ybase + 70);
		text("Ocean Quake", xbase + 50, ybase + 90);
		text("Size ~ Magnitude", xbase + 25, ybase + 110);

		fill(255, 255, 255);
		ellipse(xbase + 35, ybase + 70, 10, 10);
		rect(xbase + 35 - 5, ybase + 90 - 5, 10, 10);

		fill(color(255, 255, 0));
		ellipse(xbase + 35, ybase + 140, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase + 35, ybase + 160, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase + 35, ybase + 180, 12, 12);

		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Shallow", xbase + 50, ybase + 140);
		text("Intermediate", xbase + 50, ybase + 160);
		text("Deep", xbase + 50, ybase + 180);

		text("Past hour", xbase + 50, ybase + 200);

		fill(255, 255, 255);
		int centerx = xbase + 35;
		int centery = ybase + 200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx - 8, centery - 8, centerx + 8, centery + 8);
		line(centerx - 8, centery + 8, centerx + 8, centery - 8);

	}

	// Checks whether this quake occurred on land. If it did, it sets the
	// "country" property of its PointFeature to the country where it occurred
	// and returns true. Notice that the helper method isInCountry will
	// set this "country" property already. Otherwise it returns false.
	private boolean isLand(PointFeature earthquake) {

		// IMPLEMENT THIS: loop over all countries to check if location is in
		// any of them
		// If it is, add 1 to the entry in countryQuakes corresponding to this
		// country.
		for (Marker country : countryMarkers) {
			if (isInCountry(earthquake, country)) {
				return true;
			}
		}

		// not inside any country
		return false;
	}

	// prints countries with number of earthquakes
	private void printQuakes() {
		int totalWaterQuakes = quakeMarkers.size();
		for (Marker country : countryMarkers) {
			String countryName = country.getStringProperty("name");
			int numQuakes = 0;
			for (Marker marker : quakeMarkers) {
				EarthquakeMarker eqMarker = (EarthquakeMarker) marker;
				if (eqMarker.isOnLand()) {
					if (countryName.equals(eqMarker.getStringProperty("country"))) {
						numQuakes++;
					}
				}
			}
			if (numQuakes > 0) {
				totalWaterQuakes -= numQuakes;
				System.out.println(countryName + ": " + numQuakes);
			}
		}
		System.out.println("OCEAN QUAKES: " + totalWaterQuakes);
	}

	// helper method to test whether a given earthquake is in a given country
	// This will also add the country property to the properties of the
	// earthquake feature if
	// it's in one of the countries.
	// You should not have to modify this code
	private boolean isInCountry(PointFeature earthquake, Marker country) {
		// getting location of feature
		Location checkLoc = earthquake.getLocation();

		// some countries represented it as MultiMarker
		// looping over SimplePolygonMarkers which make them up to use
		// isInsideByLoc
		if (country.getClass() == MultiMarker.class) {

			// looping over markers making up MultiMarker
			for (Marker marker : ((MultiMarker) country).getMarkers()) {

				// checking if inside
				if (((AbstractShapeMarker) marker).isInsideByLocation(checkLoc)) {
					earthquake.addProperty("country", country.getProperty("name"));

					// return if is inside one
					return true;
				}
			}
		}

		// check if inside country represented by SimplePolygonMarker
		else if (((AbstractShapeMarker) country).isInsideByLocation(checkLoc)) {
			earthquake.addProperty("country", country.getProperty("name"));

			return true;
		}
		return false;
	}

	private void distanceToCustomLocation(Marker customMarker) {

		List<Entry<String, Float>> linkedList = new LinkedList<Entry<String, Float>>();

		float smallestDistance = (float) quakeMarkers.get(0).getDistanceTo(customMarker.getLocation());
		Marker nearestEarthquake = null;

		for (Marker quake : quakeMarkers) {

			String title = quake.getProperty("title").toString();
			float distance = (float) quake.getDistanceTo(customMarker.getLocation());
			Map.Entry<String, Float> entry = new AbstractMap.SimpleEntry<String, Float>(title, distance);
			linkedList.add(entry);

			if (distance < smallestDistance) {
				smallestDistance = distance;
				nearestEarthquake = quake;
			}
		}

		System.out.println("smallestDistance " + smallestDistance + "nearestEarthquake: "
				+ nearestEarthquake.getProperty("title").toString());

		// sort the list defining in a Comparator class how to compare the
		// distances
		Collections.sort(linkedList, new Comparator<Entry<String, Float>>() {

			@Override
			public int compare(Entry<String, Float> o1, Entry<String, Float> o2) {

				return o1.getValue().compareTo(o2.getValue());
			}
		});

		System.out.println("\n\n\n The earthquakes nearest to your custom location: \n");
		for (Entry<String, Float> entry : linkedList) {

			String key = entry.getKey();
			Float value = entry.getValue();
			System.out.printf("%5.0f km   %s%n", value, key);
		}

		showNearestQuakeOnMap(nearestEarthquake);
	}

	private void showNearestQuakeOnMap(Marker quake) {
		quake.setHidden(false);
	}

	private void addNearestQuakeMenu(String text1, String text2, String text3) {

		int xbase = 25;
		int ybase = 300;

		if (mapMode == Mode.DEFAULT) {

			fill(255, 250, 240);
			rect(xbase, ybase, 150, 120);

			fill(0);
			textAlign(LEFT, CENTER);
			textSize(12);
			text(text1, xbase + 8, ybase + 18);
			text(text2, xbase + 8, ybase + 32);
			text(text3, xbase + 8, ybase + 46);

			fill(CommonMarker.ORANGE);
			rect(xbase + 55, ybase + 70, 35, 35);
		} else if (mapMode == Mode.CUSTOM_LOCATION) {

			fill(CommonMarker.ORANGE);
			rect(xbase, ybase, 150, 120);

			fill(0);
			textAlign(LEFT, CENTER);
			textSize(12);
			text("Now click on the map.", xbase + 8, ybase + 18);
			text("Then click again to", xbase + 8, ybase + 52);
			text("return to the default", xbase + 8, ybase + 66);

		}

	}

	/*
	 * //System.out.println(lastClicked.getDistanceTo(Location loc));
	 * System.out.println("threatCircle: " + ((OceanQuakeMarker)
	 * lastClicked).threatCircle()); System.out.println("getScreenPosition: " +
	 * lastClicked.getScreenPosition(map)); System.out.println("getLocation: " +
	 * lastClicked.getLocation()); System.out.println("lastClicked: " +
	 * lastClicked.getProperties()); //System.out.println(
	 * lastClicked.drawMarker(pg, x, y);); //System.out.println(
	 * ((OceanQuakeMarker) lastClicked).drawEarthquake(pg, x, y);); } }
	 */
}
