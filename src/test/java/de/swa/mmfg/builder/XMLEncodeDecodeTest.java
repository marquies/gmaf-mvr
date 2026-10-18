package de.swa.mmfg.builder;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class XMLEncodeDecodeTest {

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
