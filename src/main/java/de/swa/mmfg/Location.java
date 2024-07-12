package de.swa.mmfg;

import java.net.URL;

/** data type to represent file or URL locations **/
public class Location {
	public static int TYPE_ORIGINAL = 40;
	public static int TYPE_ORIGINAL_COPY = 41;
	public static int TYPE_HIGHRES = 42;
	public static int TYPE_LOWRES = 43;
	public static int TYPE_THUMBNAIL = 44;

	private int type;
	private URL location;
	private String name;

	public Location() {
	}

	public Location(int type, URL location, String name) {
		this.type = type;
		this.location = location;
		this.name = name;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public URL getLocation() {
		return location;
	}

	public void setLocation(URL location) {
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	
}
