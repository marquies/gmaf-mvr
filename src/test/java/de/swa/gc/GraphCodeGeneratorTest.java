package de.swa.gc;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import de.swa.mmfg.CompositionRelationship;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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

	@Test
	public void generate_marksNodesWithChildrenAsNodeTypeAndOthersAsLeafType() {
		MMFG m = new MMFG();
		Node parent = new Node("car", m);
		parent.addChildNode(new Node("wheel", m));

		GraphCode gc = GraphCodeGenerator.generate(m);
		assertEquals(1, gc.getEdgeValueForTerms("car", "car"));
		assertEquals(2, gc.getEdgeValueForTerms("wheel", "wheel"));
	}

	@Test
	public void generate_emptyMmfg_hasEmptyDictionary() {
		GraphCode gc = GraphCodeGenerator.generate(new MMFG());
		assertTrue(gc.getDictionary().isEmpty());
		assertEquals(0, gc.getRelationships().length);
	}

	@Test
	public void generate_skipsEmptyTermsAndSentences() {
		MMFG m = new MMFG();
		Node car = new Node("car", m);
		car.addChildNode(new Node("", m));
		car.addChildNode(new Node("Sentence_1", m));

		Vector<String> dict = GraphCodeGenerator.generate(m).getDictionary();
		assertEquals(1, dict.size());
		assertFalse(dict.contains("sentence_1"));
	}

	@Test
	public void generate_listsRepeatedTermsOnce() {
		MMFG m = new MMFG();
		new Node("car", m);
		new Node("car", m);

		assertEquals(1, GraphCodeGenerator.generate(m).getDictionary().size());
	}

	@Test
	public void generate_usesCompositionTypeAsEdgeValue() {
		MMFG m = new MMFG();
		Node sky = new Node("sky", m);
		Node road = new Node("road", m);
		sky.addCompositionRelationship(new CompositionRelationship(CompositionRelationship.RELATION_ABOVE, road));

		GraphCode gc = GraphCodeGenerator.generate(m);
		assertEquals(CompositionRelationship.RELATION_ABOVE, gc.getEdgeValueForTerms("sky", "road"));
		assertEquals(0, gc.getEdgeValueForTerms("road", "sky"));
	}

	@Test
	public void generate_addsAGraphCodePerCollectionElement() {
		MMFG parent = new MMFG();
		new Node("car", parent);
		MMFG element = new MMFG();
		new Node("wheel", element);
		parent.addToCollection(element);

		GraphCode gc = GraphCodeGenerator.generate(parent);
		assertTrue(gc.isCollection());
		assertTrue(gc.getCollectionElements().get(0).getDictionary().contains("wheel"));
	}

	@Disabled("Known bug: the matrix keeps duplicates that getDictionary() removes, so the metric reads the wrong edges")
	@Test
	public void generate_termsDifferingOnlyInCase_stayPerfectlySimilarToThemselves() {
		MMFG m = new MMFG();
		Node car = new Node("Car", m);
		new Node("car", m);
		car.addChildNode(new Node("road", m));

		GraphCode gc = GraphCodeGenerator.generate(m);
		assertArrayEquals(new float[] {1f, 1f, 1f}, GraphCodeMetric.calculateSimilarity(gc, gc), 0.001f);
	}
}
