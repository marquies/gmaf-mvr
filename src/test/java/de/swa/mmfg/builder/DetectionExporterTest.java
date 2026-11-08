package de.swa.mmfg.builder;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DetectionExporterTest {

	@Test
	public void flatten_generatesDetectionXml() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		Node n = new Node("object", m);
		n.addTechnicalAttribute(new TechnicalAttribute(10, 20, 100, 50, 0, 0));
		m.addNode(n);

		DetectionExporter exporter = new DetectionExporter();
		String xml = exporter.flatten(m);
		assertNotNull(xml);
		assertTrue(xml.contains("<gmaf-data>"));
		assertTrue(xml.contains("<file>photo.jpg</file>"));
		assertTrue(xml.contains("<object>"));
		assertTrue(xml.contains("<term>object</term>"));
		assertTrue(xml.contains("<bounding-box>"));
		assertTrue(xml.contains("<x>10</x>"));
	}

	@Test
	public void startFileAndEndFile() {
		DetectionExporter exporter = new DetectionExporter();
		assertTrue(exporter.startFile().startsWith("<?xml"));
		assertEquals("</gmaf-collection>", exporter.endFile());
	}

	private static void assertWellFormed(String xml) {
		assertDoesNotThrow(() -> DocumentBuilderFactory.newInstance().newDocumentBuilder()
				.parse(new ByteArrayInputStream(xml.getBytes("UTF-8"))));
	}

	@Test
	public void flatten_includesChildNodes() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		Node car = new Node("car", m);
		car.addChildNode(new Node("wheel", m));
		m.addNode(car);

		assertTrue(new DetectionExporter().flatten(m).contains("<term>wheel</term>"));
	}

	@Test
	public void flatten_withoutBoundingBox_writesZeroes() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		m.addNode(new Node("car", m));

		String xml = new DetectionExporter().flatten(m);
		assertTrue(xml.contains("<x>0</x>"));
		assertTrue(xml.contains("<width>0</width>"));
	}

	@Test
	public void flatten_ignoresEmptyTechnicalAttributes() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		Node car = new Node("car", m);
		car.addTechnicalAttribute(new TechnicalAttribute(5, 6, 7, 8, 0, 0));
		car.addTechnicalAttribute(new TechnicalAttribute(0, 0, 0, 0, 0, 0));
		m.addNode(car);

		assertTrue(new DetectionExporter().flatten(m).contains("<x>5</x>"));
	}

	@Test
	public void flatten_prefersFileReferenceOverFileName() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("name.jpg");
		m.getGeneralMetadata().setFileReference(new File("reference.jpg"));

		assertTrue(new DetectionExporter().flatten(m).contains("<file>reference.jpg</file>"));
	}

	@Test
	public void flatten_restoresOriginalNameOfTempFiles() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("GMAF_TMP_photo.jpg_12345.jpg");

		assertTrue(new DetectionExporter().flatten(m).contains("<file>photo.jpg</file>"));
	}

	@Test
	public void completeFile_isWellFormed() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		m.addNode(new Node("car", m));

		DetectionExporter exporter = new DetectionExporter();
		assertWellFormed(exporter.startFile() + exporter.flatten(m) + exporter.endFile());
	}

	@Test
	public void unflatten_isNotSupported() {
		assertNull(new DetectionExporter().unflatten("<gmaf-data/>"));
	}

	@Disabled("Known bug: terms are written without XML escaping")
	@Test
	public void flatten_escapesSpecialCharactersInTerms() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		m.addNode(new Node("R&B <live>", m));

		assertWellFormed(new DetectionExporter().flatten(m));
	}
}
