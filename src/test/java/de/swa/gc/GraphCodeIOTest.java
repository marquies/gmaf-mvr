package de.swa.gc;

import com.google.gson.Gson;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphCodeIOTest {

	@Test
	public void asJson_andRead_roundTrip() {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc.setValueForTerms("term_a", "term_b", 3);
		String json = GraphCodeIO.asJson(gc);
		GraphCode parsed = new Gson().fromJson(json, GraphCode.class);
		assertEquals(2, parsed.getDictionary().size());
		assertEquals(3, parsed.getEdgeValueForTerms("term_a", "term_b"));
	}

	@Test
	public void writeAndRead_fileRoundTrip() throws Exception {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new java.util.Vector<String>() {{ add("term_x"); add("term_y"); }});
		gc.setValueForTerms("term_x", "term_y", 7);
		File temp = File.createTempFile("gc_test_", ".gc");
		temp.deleteOnExit();
		GraphCodeIO.write(gc, temp);
		assertTrue(temp.exists());
		GraphCode read = GraphCodeIO.read(temp);
		assertEquals(2, read.getDictionary().size());
		assertEquals(7, read.getEdgeValueForTerms("term_x", "term_y"));
	}

	@Test
	public void flatten_generatesJsonForMMFG() {
		MMFG m = new MMFG();
		new Node("Root-Asset", m);
		new Node("car", m);
		String json = new GraphCodeIO().flatten(m);
		assertNotNull(json);
		assertTrue(json.contains("car"));
	}
}
