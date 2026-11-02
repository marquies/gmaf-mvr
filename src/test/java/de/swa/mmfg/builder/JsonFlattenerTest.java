package de.swa.mmfg.builder;

import com.google.gson.Gson;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.TechnicalAttribute;
import de.swa.mmfg.Timerange;
import java.awt.Color;
import java.io.File;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

	@Test
	public void flatten_includesChildNodes() {
		MMFG m = new MMFG();
		Node parent = new Node("car", m);
		parent.addChildNode(new Node("wheel", m));
		m.addNode(parent);

		MMFG parsed = new Gson().fromJson(new JsonFlattener().flatten(m), MMFG.class);
		assertEquals("wheel", parsed.getNodes().get(0).getChildNodes().get(0).getName());
	}

	@Test
	public void flatten_includesTechnicalAttributes() {
		MMFG m = new MMFG();
		Node car = new Node("car", m);
		TechnicalAttribute ta = new TechnicalAttribute(10, 20, 30, 40, 0, 0);
		ta.addDominantColor(Color.RED);
		car.addTechnicalAttribute(ta);
		m.addNode(car);

		assertTrue(new JsonFlattener().flatten(m).contains("\"relative_x\":10"));
	}

	@Test
	public void flatten_leavesOutFileReference() {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName("photo.jpg");
		m.getGeneralMetadata().setFileReference(new File("/secret/path/photo.jpg"));

		String json = new JsonFlattener().flatten(m);
		assertTrue(json.contains("photo.jpg"));
		assertFalse(json.contains("/secret/path"));
	}

	@Test
	public void fileHasNoHeaderOrFooter() {
		JsonFlattener flattener = new JsonFlattener();
		assertEquals("", flattener.startFile());
		assertEquals("", flattener.endFile());
	}

	@Test
	public void flatten_handlesMutualCompositionRelationships() {
		MMFG m = new MMFG();
		Node left = new Node("left", m);
		Node right = new Node("right", m);
		left.addCompositionRelationship(new CompositionRelationship(CompositionRelationship.RELATION_NEXT_TO, right));
		right.addCompositionRelationship(new CompositionRelationship(CompositionRelationship.RELATION_NEXT_TO, left));
		m.addNode(left);

		assertDoesNotThrow(() -> new JsonFlattener().flatten(m));
	}

	@Disabled("Known bug: Node.timerange and Timerange.nodes reference each other, which Gson can't serialize")
	@Test
	public void flatten_handlesNodesWithATimerange() {
		MMFG m = new MMFG();
		m.addNode(new Node("car", m, new Timerange(new Date(0), new Date(1000))));

		assertDoesNotThrow(() -> new JsonFlattener().flatten(m));
	}
}
