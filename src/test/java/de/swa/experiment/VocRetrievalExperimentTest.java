package de.swa.experiment;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeMetric;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.gc.processing.DefaultCollectionProcessor;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.mmfg.CompositionRelationship;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.google.gson.GsonBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Retrieval experiment over PASCAL VOC annotations, see experiments/retrieval-experiment.md.
 * Only runs when -Dvoc.annotations is set, and writes its measurements to -Dvoc.report.
 */
class VocRetrievalExperimentTest {
	/** the ten classes of the PASCAL VOC evaluation in the ICIVR 2021 paper **/
	static final List<String> CLASSES = Arrays.asList("bicycle", "bus", "car", "cat", "cow", "dog", "horse",
			"motorbike", "person", "sheep");
	static final int MIN_RELEVANT = 10;
	static final int[] CUTOFFS = Arrays.stream(System.getProperty("voc.k", "5,10,20,50,100").split(","))
			.mapToInt(k -> Integer.parseInt(k.trim())).toArray();

	private static final class Query {
		final String id;
		final GraphCode graphCode;
		final Set<String> relevant;

		Query(String id, GraphCode graphCode, Set<String> relevant) {
			this.id = id;
			this.graphCode = graphCode;
			this.relevant = relevant;
		}
	}

	private interface Ranker {
		List<VocImage> rank(GraphCode query, Vector<VocImage> images);
	}

	private static GraphCode query(String a, String b, int relation) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<String>(Arrays.asList(a, b)));
		gc.setValueForTerms(a, a, 1);
		gc.setValueForTerms(b, b, 1);
		gc.setValueForTerms(a, b, relation);
		return gc;
	}

	private static GraphCode query(String term) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<String>(Arrays.asList(term)));
		gc.setValueForTerms(term, term, 1);
		return gc;
	}

	/** the ground truth comes from the bounding boxes alone, so it doesn't depend on the code under test **/
	private static boolean related(VocImage image, String a, String b, BiPredicate<VocImage.Box, VocImage.Box> relation) {
		for (VocImage.Box first : image.boxes) {
			for (VocImage.Box second : image.boxes) {
				if (first.label.equals(a) && second.label.equals(b) && relation.test(first, second)) return true;
			}
		}
		return false;
	}

	private static List<Query> relationQueries(Vector<VocImage> images, String name, int type,
			BiPredicate<VocImage.Box, VocImage.Box> relation) {
		List<Query> queries = new ArrayList<Query>();
		for (String a : CLASSES) {
			for (String b : CLASSES) {
				if (a.equals(b)) continue;
				Set<String> relevant = images.stream().filter(i -> related(i, a, b, relation)).map(i -> i.name)
						.collect(Collectors.toCollection(HashSet::new));
				if (relevant.size() >= MIN_RELEVANT) queries.add(new Query(a + " " + name + " " + b, query(a, b, type), relevant));
			}
		}
		return queries;
	}

	private static List<VocImage> rankBy(GraphCode query, Vector<VocImage> images, Comparator<float[]> order) {
		Map<VocImage, float[]> scores = new LinkedHashMap<VocImage, float[]>();
		for (VocImage image : images) scores.put(image, PaperMetric.calculate(query, image.graphCode));
		return scores.entrySet().stream().sorted((x, y) -> order.compare(x.getValue(), y.getValue()))
				.map(Map.Entry::getKey).collect(Collectors.toList());
	}

	/** GraphCodeMetric and DefaultCollectionProcessor, i.e. what the application runs **/
	private static List<VocImage> rankProduction(GraphCode query, Vector<VocImage> images) {
		Map<String, VocImage> byName = new LinkedHashMap<String, VocImage>();
		Vector<GraphCodeMeta> metas = new Vector<GraphCodeMeta>();
		for (VocImage image : images) {
			byName.put(image.name, image);
			metas.add(new GraphCodeMeta(image.name, image.graphCode));
		}
		CollectionProcessor processor = new DefaultCollectionProcessor();
		processor.setOperation(CollectionProcessor.SIMILARITY);
		processor.setQueryObject(query);
		processor.preloadIndex(metas);
		processor.execute();
		return processor.getResultList().stream().filter(m -> m.getMetric()[0] > 0)
				.map(m -> byName.get(m.getFileName())).collect(Collectors.toList());
	}

	private static List<VocImage> matchingTermsOnly(GraphCode query, List<VocImage> ranked) {
		return ranked.stream().filter(i -> GraphCodeMetric.calculateSimilarity(query, i.graphCode)[0] > 0)
				.collect(Collectors.toList());
	}

	private static Map<String, Object> measure(Ranker ranker, List<Query> queries, Vector<VocImage> images) {
		Map<String, Double> averagePrecision = new LinkedHashMap<String, Double>();
		double[][] atCutoff = new double[CUTOFFS.length][3];
		double precision = 0, recall = 0;
		for (Query q : queries) {
			List<Boolean> ranked = matchingTermsOnly(q.graphCode, ranker.rank(q.graphCode, images)).stream()
					.map(i -> q.relevant.contains(i.name)).collect(Collectors.toList());
			int relevant = q.relevant.size();
			averagePrecision.put(q.id, RankingMeasures.averagePrecisionAt(ranked, relevant, images.size()));
			for (int c = 0; c < CUTOFFS.length; c++) {
				atCutoff[c][0] += RankingMeasures.precisionAt(ranked, CUTOFFS[c]);
				atCutoff[c][1] += RankingMeasures.recallAt(ranked, relevant, CUTOFFS[c]);
				atCutoff[c][2] += RankingMeasures.averagePrecisionAt(ranked, relevant, CUTOFFS[c]);
			}
			precision += ranked.isEmpty() ? 0 : RankingMeasures.precisionAt(ranked, ranked.size());
			recall += RankingMeasures.recallAt(ranked, relevant, ranked.size());
		}
		Map<String, Map<String, Double>> atK = new LinkedHashMap<String, Map<String, Double>>();
		for (int c = 0; c < CUTOFFS.length; c++) {
			Map<String, Double> values = new LinkedHashMap<String, Double>();
			values.put("P", atCutoff[c][0] / queries.size());
			values.put("R", atCutoff[c][1] / queries.size());
			values.put("mAP", atCutoff[c][2] / queries.size());
			atK.put(String.valueOf(CUTOFFS[c]), values);
		}
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("queries", queries.size());
		result.put("mAP", averagePrecision.values().stream().mapToDouble(d -> d).average().orElse(0));
		result.put("precision", precision / queries.size());
		result.put("recall", recall / queries.size());
		result.put("atK", atK);
		result.put("averagePrecision", averagePrecision);
		return result;
	}

	/** M_F, then M_FR, then M_RT, as in the ordering of the paper's pseudocode **/
	private static Comparator<float[]> paperOrder(boolean mrtIsDistance) {
		return (a, b) -> {
			if (a[0] != b[0]) return Float.compare(b[0], a[0]);
			if (a[1] != b[1]) return Float.compare(b[1], a[1]);
			return mrtIsDistance ? Float.compare(a[2], b[2]) : Float.compare(b[2], a[2]);
		};
	}

	private static long millisForQueries(Ranker ranker, List<Query> queries, Vector<VocImage> images) {
		long start = System.nanoTime();
		for (Query q : queries) ranker.rank(q.graphCode, images);
		return (System.nanoTime() - start) / 1000000;
	}

	private static Map<String, Object> variants(Map<String, Ranker> rankers, List<Query> terms, List<Query> relations,
			Vector<VocImage> images) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		for (Map.Entry<String, Ranker> ranker : rankers.entrySet()) {
			Map<String, Object> variant = new LinkedHashMap<String, Object>();
			variant.put("terms", measure(ranker.getValue(), terms, images));
			variant.put("relations", measure(ranker.getValue(), relations, images));
			result.put(ranker.getKey(), variant);
		}
		return result;
	}

	@Test
	@EnabledIfSystemProperty(named = "voc.annotations", matches = ".+")
	void runExperiment() throws Exception {
		File annotations = new File(System.getProperty("voc.annotations"));
		Vector<VocImage> images = VocImage.load(annotations, false);

		List<Query> terms = new ArrayList<Query>();
		for (String label : CLASSES) {
			Set<String> relevant = images.stream().filter(i -> i.contains(label)).map(i -> i.name)
					.collect(Collectors.toCollection(HashSet::new));
			terms.add(new Query(label, query(label), relevant));
		}

		List<Query> relations = new ArrayList<Query>();
		relations.addAll(relationQueries(images, "above", CompositionRelationship.RELATION_ABOVE,
				(a, b) -> a.ymax < b.ymin));
		relations.addAll(relationQueries(images, "under", CompositionRelationship.RELATION_UNDER,
				(a, b) -> a.ymin > b.ymax));
		relations.addAll(relationQueries(images, "next_to", CompositionRelationship.RELATION_NEXT_TO,
				(a, b) -> a.xmax < b.xmin || a.xmin > b.xmax));
		assertFalse(relations.isEmpty());

		Map<String, Ranker> rankers = new LinkedHashMap<String, Ranker>();
		rankers.put("production", VocRetrievalExperimentTest::rankProduction);
		rankers.put("paper_mrt_descending", (q, i) -> rankBy(q, i, paperOrder(false)));
		rankers.put("paper_mrt_ascending", (q, i) -> rankBy(q, i, paperOrder(true)));

		Map<String, Object> report = new LinkedHashMap<String, Object>();
		report.put("images", images.size());
		Map<String, Integer> relevantPerClass = new LinkedHashMap<String, Integer>();
		for (Query q : terms) relevantPerClass.put(q.id, q.relevant.size());
		report.put("relevantPerClass", relevantPerClass);
		report.put("productionFusion", variants(rankers, terms, relations, images));
		report.put("correctedLabels", variants(rankers, terms, relations, VocImage.load(annotations, true)));

		// runtimes are hardware dependent and informational only, the paper's claim is linear growth
		Map<String, Long> runtime = new LinkedHashMap<String, Long>();
		for (int fraction = 1; fraction <= 4; fraction++) {
			Vector<VocImage> subset = new Vector<VocImage>(images.subList(0, images.size() * fraction / 4));
			runtime.put(String.valueOf(subset.size()), millisForQueries(rankers.get("production"), terms, subset));
		}
		report.put("runtimeMillisByCollectionSize", runtime);

		String json = new GsonBuilder().setPrettyPrinting().create().toJson(report);
		Files.write(new File(System.getProperty("voc.report", "target/voc-report.json")).toPath(), json.getBytes("UTF-8"));
	}
}
