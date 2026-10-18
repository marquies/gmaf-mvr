package de.swa.mmfg.builder;

import com.google.gson.Gson;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonFlattenerTest {

	@Test
	public void flatten_returnsJsonString() {
		MMFG m = new MMFG();
		new Node("test", m);
		JsonFlattener flattener = new JsonFlattener();
		String json = flattener.flatten(m);
		assertNotNull(json);
		assertTrue(json.contains("test"));
		assertEquals("json", flattener.getFileExtension());
	}

	@Test
	public void roundTrip_throughGson() {
		MMFG m = new MMFG();
		new Node("node1", m);
		JsonFlattener flattener = new JsonFlattener();
		String json = flattener.flatten(m);
		MMFG parsed = new Gson().fromJson(json, MMFG.class);
		assertNotNull(parsed);
		assertEquals(1, parsed.getAllNodes().size());
	}
}
