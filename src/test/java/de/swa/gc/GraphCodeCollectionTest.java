package de.swa.gc;

import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
		child1.setDictionary(new Vector<String>() {{ add("a"); add("b"); }});
		child1.setValueForTerms("a", "b", 1);
		GraphCode child2 = new GraphCode();
		child2.setDictionary(new Vector<String>() {{ add("b"); add("c"); }});
		child2.setValueForTerms("b", "c", 2);
		root.addGraphCode(child1);
		root.addGraphCode(child2);
		GraphCode union = GraphCodeCollection.getCollectionGraphCode(root);
		Vector<String> dict = union.getDictionary();
		assertTrue(dict.contains("a"));
		assertTrue(dict.contains("b"));
		assertTrue(dict.contains("c"));
	}

	@Test
	public void getUnion_mergesDictionariesAndEdges() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new Vector<String>() {{ add("a"); add("b"); }});
		gc1.setValueForTerms("a", "b", 5);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new Vector<String>() {{ add("b"); add("c"); }});
		gc2.setValueForTerms("b", "c", 7);
		Vector<GraphCode> list = new Vector<>();
		list.add(gc1);
		list.add(gc2);
		GraphCode union = GraphCodeCollection.getUnion(list);
		assertEquals(3, union.getDictionary().size());
		assertEquals(5, union.getEdgeValueForTerms("a", "b"));
		assertEquals(7, union.getEdgeValueForTerms("b", "c"));
	}

	@Test
	public void subtract_removesDictionaryTermsAndEdges() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new Vector<String>() {{ add("a"); add("b"); add("c"); }});
		gc1.setValueForTerms("a", "b", 1);
		gc1.setValueForTerms("b", "c", 1);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new Vector<String>() {{ add("b"); add("c"); }});
		GraphCode result = GraphCodeCollection.subtract(gc1, gc2);
		Vector<String> dict = result.getDictionary();
		assertTrue(dict.contains("a"));
		assertFalse(dict.contains("b"));
		assertFalse(dict.contains("c"));
	}

	@Test
	public void getSummaryGraphCode_returnsUnionOfTopElements() {
		GraphCode root = new GraphCode();
		GraphCode child = new GraphCode();
		child.setDictionary(new Vector<String>() {{ add("x"); }});
		child.setValueForTerms("x", "x", 10);
		root.addGraphCode(child);
		GraphCode summary = GraphCodeCollection.getSummaryGraphCode(root, 1);
		assertTrue(summary.getDictionary().contains("x"));
	}
}
