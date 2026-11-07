package de.swa.mmfg.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
