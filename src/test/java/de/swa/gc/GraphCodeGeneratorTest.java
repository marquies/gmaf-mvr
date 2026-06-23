package de.swa.gc;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphCodeGeneratorTest {

	@Test
	public void generate_includesRootAndChildNodesInDictionary() {
		MMFG m = new MMFG();
		Node root = new Node("Root-Asset", m);
		Node child = new Node("car", m);
		root.addChildNode(child);
		m.addNode(root);

		GraphCode gc = GraphCodeGenerator.generate(m);
		Vector<String> dict = gc.getDictionary();
		assertNotNull(dict);
		assertTrue(dict.contains("car"));
		assertEquals(1, gc.getEdgeValueForTerms("Root-Asset", "car"));
	}

	@Test
	public void generate_setsNodeTypeValues() {
		MMFG m = new MMFG();
		Node root = new Node("Root-Asset", m);
		m.addNode(root);
		Node leaf = new Node("wheel", m);
		m.addNode(leaf);

		GraphCode gc = GraphCodeGenerator.generate(m);
		assertEquals(2, gc.getEdgeValueForTerms("Root-Asset", "Root-Asset"));
		assertEquals(2, gc.getEdgeValueForTerms("wheel", "wheel"));
	}
}
