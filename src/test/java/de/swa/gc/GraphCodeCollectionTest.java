package de.swa.gc;

import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GraphCodeCollectionTest {

	@Test
	public void getCollectionGraphCode_returnsSameGraphCode_whenNoCollectionElements() {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<String>() {{ add("a"); }});
		gc.setValueForTerms("a", "a", 1);
		GraphCode result = GraphCodeCollection.getCollectionGraphCode(gc);
		assertEquals(gc, result);
	}

	@Test
	public void getCollectionGraphCode_returnsUnion_whenCollectionElementsPresent() {
		GraphCode root = new GraphCode();
		GraphCode child1 = new GraphCode();
		child1.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		child1.setValueForTerms("term_a", "term_b", 1);
		GraphCode child2 = new GraphCode();
		child2.setDictionary(new java.util.Vector<String>() {{ add("term_b"); add("term_c"); }});
		child2.setValueForTerms("term_b", "term_c", 2);
		root.addGraphCode(child1);
		root.addGraphCode(child2);
		GraphCode union = GraphCodeCollection.getCollectionGraphCode(root);
		java.util.Vector<String> dict = union.getDictionary();
		assertTrue(dict.contains("term_a"));
		assertTrue(dict.contains("term_b"));
		assertTrue(dict.contains("term_c"));
	}

	@Test
	public void getUnion_mergesDictionariesAndEdges() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc1.setValueForTerms("term_a", "term_b", 5);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new java.util.Vector<String>() {{ add("term_b"); add("term_c"); }});
		gc2.setValueForTerms("term_b", "term_c", 7);
		java.util.Vector<GraphCode> list = new java.util.Vector<>();
		list.add(gc1);
		list.add(gc2);
		GraphCode union = GraphCodeCollection.getUnion(list);
		assertEquals(3, union.getDictionary().size());
		assertEquals(5, union.getEdgeValueForTerms("term_a", "term_b"));
		assertEquals(7, union.getEdgeValueForTerms("term_b", "term_c"));
	}

	@Test
	public void subtract_removesDictionaryTermsAndEdges() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); add("term_c"); }});
		gc1.setValueForTerms("term_a", "term_b", 1);
		gc1.setValueForTerms("term_b", "term_c", 1);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new java.util.Vector<String>() {{ add("term_b"); add("term_c"); }});
		GraphCode result = GraphCodeCollection.subtract(gc1, gc2);
		java.util.Vector<String> dict = result.getDictionary();
		assertTrue(dict.contains("term_a"));
		assertFalse(dict.contains("term_b"));
		assertFalse(dict.contains("term_c"));
	}

	@Test
	public void getSummaryGraphCode_returnsUnionOfTopElements() {
		GraphCode root = new GraphCode();
		GraphCode child = new GraphCode();
		child.setDictionary(new java.util.Vector<String>() {{ add("term_x"); }});
		child.setValueForTerms("term_x", "term_x", 10);
		root.addGraphCode(child);
		GraphCode summary = GraphCodeCollection.getSummaryGraphCode(root, 1);
		assertTrue(summary.getDictionary().contains("term_x"));
	}
}
