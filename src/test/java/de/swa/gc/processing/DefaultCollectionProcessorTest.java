package de.swa.gc.processing;

import de.swa.gc.GraphCode;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultCollectionProcessorTest {

	@Test
	public void getResultList_sortsBySimilarity_descending() {
		DefaultCollectionProcessor processor = new DefaultCollectionProcessor();

		GraphCode query = new GraphCode();
		query.setDictionary(new Vector<String>() {{ add("a"); }});
		query.setValueForTerms("a", "a", 1);
		processor.setQueryObject(query);

		GraphCode candidate1 = new GraphCode();
		candidate1.setDictionary(new Vector<String>() {{ add("a"); }});
		candidate1.setValueForTerms("a", "a", 1);
		GraphCodeMeta meta1 = new GraphCodeMeta("candidate1", candidate1);

		GraphCode candidate2 = new GraphCode();
		candidate2.setDictionary(new Vector<String>() {{ add("a"); add("b"); }});
		candidate2.setValueForTerms("a", "a", 1);
		candidate2.setValueForTerms("b", "b", 1);
		GraphCodeMeta meta2 = new GraphCodeMeta("candidate2", candidate2);

		Vector<GraphCodeMeta> metas = new Vector<>();
		metas.add(meta1);
		metas.add(meta2);

		processor.setOperation(CollectionProcessor.SIMILARITY);
		processor.preloadIndex(metas);
		processor.execute();
		Vector<GraphCodeMeta> result = processor.getResultList();

		assertNotNull(result);
		assertEquals(2, result.size());
		assertEquals("candidate1", result.get(0).getFileName());
		assertEquals("candidate2", result.get(1).getFileName());
	}

	private static Vector<String> rankedNames(int operation, float[]... metrics) {
		Vector<GraphCodeMeta> metas = new Vector<>();
		for (int i = 0; i < metrics.length; i++) {
			GraphCodeMeta meta = new GraphCodeMeta("gc" + i, new GraphCode());
			meta.setMetric(metrics[i]);
			metas.add(meta);
		}
		DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
		processor.setOperation(operation);
		processor.preloadIndex(metas);
		Vector<String> names = new Vector<>();
		for (GraphCodeMeta meta : processor.getResultList()) names.add(meta.getFileName());
		return names;
	}

	@Test
	public void getResultList_ranksByEdgeTypesWhenFeaturesAndEdgesAreEqual() {
		Vector<String> ranked = rankedNames(CollectionProcessor.SIMILARITY,
				new float[] {1f, 1f, 0f}, new float[] {1f, 1f, 0.5f}, new float[] {1f, 1f, 1f});
		assertEquals("[gc2, gc1, gc0]", ranked.toString());
	}

	@Test
	public void getResultList_ranksByEdgesDifferingByLessThanAHundredth() {
		Vector<String> ranked = rankedNames(CollectionProcessor.SIMILARITY,
				new float[] {0.5f, 1f, 1f}, new float[] {1f, 0f, 0f}, new float[] {1f, 0.004f, 0f});
		assertEquals("[gc2, gc1, gc0]", ranked.toString());
	}

	@Test
	public void getResultList_ranksRecommendationsByEdgesFirst() {
		Vector<String> ranked = rankedNames(CollectionProcessor.RECOMMENDATION,
				new float[] {1f, 0.5f, 1f}, new float[] {0.5f, 1f, 0f}, new float[] {0.5f, 1f, 0.5f});
		assertEquals("[gc2, gc1, gc0]", ranked.toString());
	}

	/** metrics that differ by less than 1 in the combined score made the comparator non transitive **/
	@Test
	public void getResultList_sortsMetricsWithFractionalDifferencesWithoutFailing() {
		float[] steps = {0f, 0.5f, 1f};
		Vector<float[]> metrics = new Vector<>();
		for (int i = 0; i < 500; i++) metrics.add(new float[] {1f, steps[i % 3], steps[(i / 3) % 3]});
		Collections.shuffle(metrics, new Random(42));

		Vector<GraphCodeMeta> metas = new Vector<>();
		for (int i = 0; i < metrics.size(); i++) {
			GraphCodeMeta meta = new GraphCodeMeta("gc" + i, new GraphCode());
			meta.setMetric(metrics.get(i));
			metas.add(meta);
		}
		DefaultCollectionProcessor processor = new DefaultCollectionProcessor();
		processor.setOperation(CollectionProcessor.SIMILARITY);
		processor.preloadIndex(metas);
		Vector<GraphCodeMeta> result = processor.getResultList();

		for (int i = 1; i < result.size(); i++) {
			float[] before = result.get(i - 1).getMetric();
			float[] after = result.get(i).getMetric();
			assertTrue(before[1] > after[1] || before[1] == after[1] && before[2] >= after[2],
					"position " + i + " is out of order");
		}
	}
}
