package de.swa.mmfg;

import org.junit.jupiter.api.Test;
import java.net.MalformedURLException;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocationTest {

	@Test
	public void creatingLocationWithValidParameters_setsAllFieldsCorrectly() throws MalformedURLException {
		URL testUrl = new URL("http://example.com");
		Location location = new Location(Location.TYPE_ORIGINAL, testUrl, "OriginalLocation");

		assertEquals(Location.TYPE_ORIGINAL, location.getType());
		assertEquals(testUrl, location.getLocation());
		assertEquals("OriginalLocation", location.getName());
	}

	@Test
	public void settingLocationType_updatesTypeField() {
		Location location = new Location();
		location.setType(Location.TYPE_HIGHRES);

		assertEquals(Location.TYPE_HIGHRES, location.getType());
	}

	@Test
	public void settingLocation_updatesLocationField() throws MalformedURLException {
		Location location = new Location();
		URL testUrl = new URL("http://example.com");
		location.setLocation(testUrl);

		assertEquals(testUrl, location.getLocation());
	}

	@Test
	public void settingName_updatesNameField() {
		Location location = new Location();
		location.setName("UpdatedName");

		assertEquals("UpdatedName", location.getName());
	}

	@Test
	public void creatingLocationWithoutParameters_initializesWithNullValues() {
		Location location = new Location();

		assertEquals(0, location.getType());
		assertEquals(null, location.getLocation());
		assertEquals(null, location.getName());
	}

	@Test
	public void creatingLocationWithInvalidURL_throwsMalformedURLException() {
		assertThrows(MalformedURLException.class, () -> {
			new Location(Location.TYPE_ORIGINAL, new URL("htp://invalid"), "InvalidURLLocation");
		});
	}
}