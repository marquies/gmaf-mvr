package de.swa.gc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import de.swa.ui.Configuration;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
	public void getUnion_doesNotSumSharedEdges() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc1.setValueForTerms("term_a", "term_b", 1);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc2.setValueForTerms("term_a", "term_b", 1);
		java.util.Vector<GraphCode> list = new java.util.Vector<>();
		list.add(gc1);
		list.add(gc2);
		GraphCode union = GraphCodeCollection.getUnion(list);
		assertEquals(1, union.getEdgeValueForTerms("term_a", "term_b"));
	}

	@Test
	public void getUnion_keepsEdgesAcrossThreeCodes() {
		GraphCode gc1 = new GraphCode();
		gc1.setDictionary(new java.util.Vector<String>() {{ add("term_a"); add("term_b"); }});
		gc1.setValueForTerms("term_a", "term_b", 5);
		GraphCode gc2 = new GraphCode();
		gc2.setDictionary(new java.util.Vector<String>() {{ add("term_b"); add("term_c"); }});
		gc2.setValueForTerms("term_b", "term_c", 7);
		GraphCode gc3 = new GraphCode();
		gc3.setDictionary(new java.util.Vector<String>() {{ add("term_d"); }});
		java.util.Vector<GraphCode> list = new java.util.Vector<>();
		list.add(gc1);
		list.add(gc2);
		list.add(gc3);
		GraphCode union = GraphCodeCollection.getUnion(list);
		assertEquals(4, union.getDictionary().size());
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

	private static Vector<String> terms(String... terms) {
		return new Vector<>(Arrays.asList(terms));
	}

	private static GraphCode graphCode(String... terms) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(terms(terms));
		return gc;
	}

	private static Vector<GraphCode> repeat(GraphCode gc, int times) {
		Vector<GraphCode> v = new Vector<>();
		for (int i = 0; i < times; i++) v.add(gc);
		return v;
	}

	/** calculateGCStop caches its result in a static field **/
	@BeforeEach
	@AfterEach
	public void resetCachedStopWords() throws Exception {
		Field gcstop = GraphCodeCollection.class.getDeclaredField("gcstop");
		gcstop.setAccessible(true);
		gcstop.set(null, null);
	}

	@Test
	public void getUnion_ofConflictingEdges_keepsLastNonZeroValue() {
		GraphCode first = graphCode("term_a", "term_b");
		first.setValueForTerms("term_a", "term_b", 5);
		GraphCode second = graphCode("term_a", "term_b");
		second.setValueForTerms("term_a", "term_b", 7);

		GraphCode union = GraphCodeCollection.getUnion(new Vector<>(Arrays.asList(first, second)));
		assertEquals(7, union.getEdgeValueForTerms("term_a", "term_b"));
	}

	@Test
	public void subtract_keepsEdgesBetweenRemainingTerms() {
		GraphCode gc = graphCode("term_a", "term_b", "term_c");
		gc.setValueForTerms("term_a", "term_b", 4);

		GraphCode result = GraphCodeCollection.subtract(gc, graphCode("term_c"));
		assertEquals(4, result.getEdgeValueForTerms("term_a", "term_b"));
	}

	@Test
	public void getSummaryGraphCode_withoutElements_returnsSameGraphCode() {
		GraphCode gc = graphCode("term_a", "term_b");
		assertSame(gc, GraphCodeCollection.getSummaryGraphCode(gc, 3));
	}

	@Test
	public void getSummaryGraphCode_picksElementMostSimilarToTheOthers() {
		GraphCode root = new GraphCode();
		root.addGraphCode(graphCode("term_a", "term_b"));
		root.addGraphCode(graphCode("term_a", "term_b", "term_c"));
		root.addGraphCode(graphCode("term_x", "term_y"));

		assertEquals(terms("term_a", "term_b"), GraphCodeCollection.getSummaryGraphCode(root, 1).getDictionary());
	}

	@Test
	public void getSummaryGraphCode_withTopLargerThanCollection_usesAllElements() {
		GraphCode root = new GraphCode();
		root.addGraphCode(graphCode("term_a", "term_b"));
		root.addGraphCode(graphCode("term_a", "term_b", "term_c"));

		assertEquals(3, GraphCodeCollection.getSummaryGraphCode(root, 10).getDictionary().size());
	}

	@Disabled("Known bug: elements with equal rank are skipped because of the sum[j] < old_max check")
	@Test
	public void getSummaryGraphCode_keepsEquallyRankedElements() {
		GraphCode root = new GraphCode();
		root.addGraphCode(graphCode("term_a", "term_b"));
		root.addGraphCode(graphCode("term_c", "term_d"));

		assertEquals(4, GraphCodeCollection.getSummaryGraphCode(root, 2).getDictionary().size());
	}

	@Test
	public void calculateGCStop_selectsTermsInMoreThan70PercentOfCodes() {
		Vector<GraphCode> collection = repeat(graphCode("frequent", "boundary", "rare"), 1);
		collection.addAll(repeat(graphCode("frequent", "boundary"), 6));
		collection.addAll(repeat(graphCode("frequent", "other"), 1));
		collection.addAll(repeat(graphCode("unrelated", "other"), 2));

		// 10 codes: "frequent" appears in 8, "boundary" in exactly 7
		assertEquals(terms("frequent"), GraphCodeCollection.calculateGCStop(collection, false).getDictionary());
	}

	@Test
	public void getFeatureRelevantGraphCode_removesStopWords() {
		GraphCodeCollection.calculateGCStop(repeat(graphCode("frequent", "other"), 1), false);

		GraphCode relevant = GraphCodeCollection.getFeatureRelevantGraphCode(graphCode("frequent", "relevant"));
		assertEquals(terms("relevant"), relevant.getDictionary());
	}

	@Disabled("Known bug: the static cache returns the first result whatever collection is passed")
	@Test
	public void calculateGCStop_recalculatesForADifferentCollection() {
		GraphCodeCollection.calculateGCStop(repeat(graphCode("first", "other"), 2), false);

		GraphCode stop = GraphCodeCollection.calculateGCStop(repeat(graphCode("second", "other"), 2), false);
		assertTrue(stop.getDictionary().contains("second"));
	}

	@Disabled("Known bug: the threshold truncates to 0 for one asset, so all of its terms become stop words")
	@Test
	public void calculateGCStop_withOneAsset_hasNoStopWords() {
		GraphCode stop = GraphCodeCollection.calculateGCStop(repeat(graphCode("term_a", "term_b"), 1), false);
		assertTrue(stop.getDictionary().isEmpty());
	}

	@Disabled("Known bug: GraphCodeIO.read returns null for a missing file, which bypasses the fallback")
	@Test
	public void calculateGCStop_withoutCacheFile_returnsEmptyGraphCode() {
		File cache = new File(Configuration.getInstance().getGraphCodeRepository(), "GCStop.gc");
		assumeFalse(cache.exists());

		assertNotNull(GraphCodeCollection.calculateGCStop(new Vector<>(), true));
	}
}
