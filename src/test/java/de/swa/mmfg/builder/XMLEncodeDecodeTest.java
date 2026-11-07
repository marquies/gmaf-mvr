package de.swa.mmfg.builder;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thoughtworks.xstream.security.ForbiddenClassException;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.Location;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import de.swa.mmfg.Timerange;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class XMLEncodeDecodeTest {
	private final List<String> warnings = new Vector<String>();
	private final Logger logger = (Logger) LogManager.getLogger(XMLEncodeDecode.class);
	private final AbstractAppender appender = new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
		public void append(LogEvent event) {
			if (event.getLevel() == Level.WARN) warnings.add(event.getMessage().getFormattedMessage());
		}
	};

	@BeforeEach
	public void captureWarnings() {
		Configurator.setLevel(logger.getName(), Level.WARN);
		appender.start();
		logger.addAppender(appender);
	}

	@AfterEach
	public void releaseWarnings() {
		logger.removeAppender(appender);
		appender.stop();
	}

	private static String withElements(String elements) {
		String xml = new XMLEncodeDecode().flatten(new MMFG());
		int end = xml.lastIndexOf("</");
		return xml.substring(0, end) + elements + xml.substring(end);
	}

	/**
	 * known elements should parse without a warning
	 */
	@Test
	public void testKnownElementsDoNotWarn() {
		new XMLEncodeDecode().unflatten(withElements(""));

		assertTrue(warnings.isEmpty(), warnings.toString());
	}

	/**
	 * an unknown element should be skipped and named in a warning
	 */
	@Test
	public void testUnknownElementWarns() {
		MMFG mmfg = new XMLEncodeDecode().unflatten(withElements("<confidence>0.9</confidence>"));

		assertTrue(mmfg.getNodes().isEmpty());
		assertEquals(1, warnings.size());
		assertTrue(warnings.get(0).contains("'confidence'"), warnings.get(0));
	}

	/**
	 * a repeated unknown element should only warn once per parsed MMFG
	 */
	@Test
	public void testRepeatedUnknownElementWarnsOncePerParse() {
		String xml = withElements("<confidence>0.9</confidence><confidence>0.8</confidence><score>1</score>");

		new XMLEncodeDecode().unflatten(xml);
		assertEquals(2, warnings.size(), warnings.toString());

		new XMLEncodeDecode().unflatten(xml);
		assertEquals(4, warnings.size(), warnings.toString());
	}

	@Test
	public void roundTrip_preservesNodes() {
		MMFG m = new MMFG();
		new Node("alpha", m);
		new Node("beta", m);
		XMLEncodeDecode codec = new XMLEncodeDecode();
		String xml = codec.flatten(m);
		MMFG parsed = codec.unflatten(xml);
		assertNotNull(parsed);
		assertEquals(2, parsed.getAllNodes().size());
	}

	@Test
	public void getFileExtension_returnsXml() {
		XMLEncodeDecode codec = new XMLEncodeDecode();
		assertEquals("xml", codec.getFileExtension());
	}

	private static MMFG roundTrip(MMFG m) {
		XMLEncodeDecode codec = new XMLEncodeDecode();
		return codec.unflatten(codec.flatten(m));
	}

	@Test
	public void roundTrip_preservesIdAndMetadata() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("clip.mp4");
		m.getGeneralMetadata().setCameraModel("camera");

		MMFG parsed = roundTrip(m);
		assertEquals(m.getId(), parsed.getId());
		assertEquals("clip.mp4", parsed.getGeneralMetadata().getFileName());
		assertEquals("camera", parsed.getGeneralMetadata().getCameraModel());
	}

	@Test
	public void roundTrip_preservesChildNodesAndAttributes() {
		MMFG m = new MMFG();
		Node car = new Node("car", m);
		car.addChildNode(new Node("wheel", m));
		car.addTechnicalAttribute(new TechnicalAttribute(1, 2, 3, 4, 0, 0));
		m.addNode(car);

		Node parsed = roundTrip(m).getNodes().get(0);
		assertEquals("wheel", parsed.getChildNodes().get(0).getName());
		assertEquals(3, parsed.getTechnicalAttributes().get(0).getWidth());
	}

	@Test
	public void roundTrip_preservesCompositionType() {
		MMFG m = new MMFG();
		Node left = new Node("left", m);
		left.addCompositionRelationship(new CompositionRelationship(CompositionRelationship.RELATION_NEXT_TO, new Node("right", m)));
		m.addNode(left);

		assertEquals(CompositionRelationship.RELATION_NEXT_TO,
				roundTrip(m).getNodes().get(0).getCompositionRelationships().get(0).getType());
	}

	@Disabled("Known bug: relatedObject is transient, so stored MMFGs lose the target of each composition relationship")
	@Test
	public void roundTrip_preservesCompositionTarget() {
		MMFG m = new MMFG();
		Node left = new Node("left", m);
		left.addCompositionRelationship(new CompositionRelationship(CompositionRelationship.RELATION_NEXT_TO, new Node("right", m)));
		m.addNode(left);

		Node target = roundTrip(m).getNodes().get(0).getCompositionRelationships().get(0).getRelatedObject();
		assertNotNull(target);
		assertEquals("right", target.getName());
	}

	@Test
	public void roundTrip_preservesTimerangesAndLocations() throws Exception {
		MMFG m = new MMFG();
		m.addNode(new Node("car", m, new Timerange(new Date(1000), new Date(2000))));
		m.addLocation(new Location(Location.TYPE_ORIGINAL, new URL("file:///photo.jpg"), "photo.jpg"));

		MMFG parsed = roundTrip(m);
		assertEquals(new Date(2000), parsed.getNodes().get(0).getTimerange().getEnd());
		assertEquals("photo.jpg", parsed.getLocations().get(0).getName());
	}

	@Test
	public void unflatten_rejectsTypesOutsideTheModel() {
		assertThrows(ForbiddenClassException.class,
				() -> new XMLEncodeDecode().unflatten("<java.lang.ProcessBuilder/>"));
	}

	@Disabled("Known bug: since Node.fv became transient, XStream can't resolve references that run through fv in 8 of the 48 files")
	@Test
	public void unflatten_readsTheCommittedMmfgRepository() throws Exception {
		File[] files = new File("mmfgs").listFiles((dir, name) -> name.endsWith(".mmfg"));
		assertTrue(files.length > 0);

		XMLEncodeDecode codec = new XMLEncodeDecode();
		for (File f : files) {
			MMFG parsed = codec.unflatten(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
			assertNotNull(parsed.getId(), f.getName());
		}
	}

	@Test
	public void fileHasNoHeaderOrFooter() {
		XMLEncodeDecode codec = new XMLEncodeDecode();
		assertEquals("", codec.startFile());
		assertEquals("", codec.endFile());
	}

	@Disabled("Known bug: tempSimilarity is transient, so unflatten leaves it null")
	@Test
	public void unflatten_initializesSimilarities() {
		assertNotNull(roundTrip(new MMFG()).getTempSimilarity());
	}

	@Disabled("Known bug: Node.fv is transient, so unflattened nodes have no MMFG and addChildNode throws")
	@Test
	public void unflatten_returnsNodesThatAcceptChildren() {
		MMFG m = new MMFG();
		m.addNode(new Node("car", m));

		Node parsed = roundTrip(m).getNodes().get(0);
		Node wheel = new Node();
		wheel.setName("wheel");
		assertDoesNotThrow(() -> parsed.addChildNode(wheel));
	}
}
