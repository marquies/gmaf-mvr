package de.swa.mmfg.builder;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
