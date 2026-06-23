package de.swa.gmaf;

import de.swa.mmfg.MMFG;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PluginChainTest {

	@Test
	public void constructor_withEmptyPluginList_doesNotThrow() {
		new PluginChain(new java.util.Vector<>());
	}

	@Test
	public void process_withEmptyPlugins_setsGeneralMetadataAndLocations() throws Exception {
		PluginChain chain = new PluginChain(new java.util.Vector<>());
		MMFG m = new MMFG();
		File temp = createTempJpeg();
		chain.process(temp.toURI().toURL(), temp, new byte[0], m, 1, 10, ".jpg");
		assertEquals(temp.getName(), m.getGeneralMetadata().getFileName());
		assertNotNull(m.getLocations());
		assertEquals(2, m.getLocations().size());
	}

	private File createTempJpeg() throws IOException {
		File temp = File.createTempFile("gmaf_test_", ".jpg");
		temp.deleteOnExit();
		javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", temp);
		return temp;
	}
}
