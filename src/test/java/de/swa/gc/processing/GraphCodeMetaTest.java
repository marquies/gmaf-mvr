package de.swa.gc.processing;

import de.swa.gc.GraphCode;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class GraphCodeMetaTest {

	@Test
	public void storesAndReturnsValues() {
		GraphCode gc = new GraphCode();
		GraphCodeMeta meta = new GraphCodeMeta("file.jpg", gc);
		assertEquals("file.jpg", meta.getFileName());
		assertSame(gc, meta.getGraphcode());
		meta.setFileName("other.jpg");
		assertEquals("other.jpg", meta.getFileName());
		meta.setMetric(new float[]{0.1f, 0.2f, 0.3f});
		assertEquals(0.1f, meta.getMetric()[0]);
	}
}
