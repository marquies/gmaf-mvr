package de.swa.gmaf.plugin;

import de.swa.mmfg.MMFG;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExifHandlerTest {

	private final ExifHandler handler = new ExifHandler();

	@Test
	public void canProcess_returnsTrueForSupportedImages() {
		assertTrue(handler.canProcess(".jpg"));
		assertTrue(handler.canProcess(".jpeg"));
		assertTrue(handler.canProcess(".png"));
		assertTrue(handler.canProcess(".tiff"));
		assertTrue(handler.canProcess(".gif"));
	}

	@Test
	public void canProcess_returnsFalseForUnsupportedExtensions() {
		assertFalse(handler.canProcess(".mp4"));
		assertFalse(handler.canProcess(".txt"));
	}

	@Test
	public void process_setsFileName_andDoesNotThrow() throws IOException {
		File temp = File.createTempFile("exif_test_", ".jpg");
		temp.deleteOnExit();
		ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", temp);
		MMFG m = new MMFG();
		handler.process(null, temp, new byte[0], m);
		assertEquals(temp.getName(), m.getGeneralMetadata().getFileName());
	}
}
