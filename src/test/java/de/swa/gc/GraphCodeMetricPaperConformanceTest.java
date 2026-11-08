package de.swa.gc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks GraphCodeMetric against the worked example of Wagenpfeil et al., "Graph Codes - 2D
 * Projections of Multimedia Feature Graphs for Fast and Effective Retrieval", ICIVR 2021, which
 * gives M_GC(GC_ex, GC_ex2) = (0.5, 0.33, 0.16).
 *
 * M_F follows the paper. M_FR and M_RT deliberately keep the definitions of the code: in the VOC
 * retrieval experiment with query by example Graph Codes, the paper's definitions ranked no better
 * (relations mAP 0.9191 vs 0.9188), and the paper's M_RT is a distance that needs an ascending order.
 * The query GC_ex has five edges: Person -> Head, Person -> Human Being, Person -> Individual,
 * Head -> Hat and Hat -> above. GC_ex2 has two of them, Head -> Hat with the same type 3 and
 * Hat -> above with type 6 instead of 5.
 */
class GraphCodeMetricPaperConformanceTest {
	private float[] similarity;

	private static GraphCode graphCode(String[] dictionary, int[][] matrix) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<String>(Arrays.asList(dictionary)));
		for (int i = 0; i < dictionary.length; i++) {
			for (int j = 0; j < dictionary.length; j++) gc.setValue(i, j, matrix[i][j]);
		}
		return gc;
	}

	@BeforeEach
	void calculateWorkedExample() {
		GraphCode gcEx = graphCode(new String[] {"Person", "Head", "Human Being", "Individual", "Hat", "above"},
				new int[][] {{1, 3, 4, 4, 0, 0}, {0, 1, 0, 0, 3, 0}, {0, 0, 1, 0, 0, 0}, {0, 0, 0, 2, 0, 0},
						{0, 0, 0, 0, 2, 5}, {0, 0, 0, 0, 0, 6}});
		GraphCode gcEx2 = graphCode(new String[] {"above", "Dog", "Head", "Animal", "Hat"},
				new int[][] {{5, 0, 0, 0, 0}, {0, 1, 3, 4, 0}, {0, 0, 1, 0, 3}, {0, 0, 0, 2, 0}, {6, 0, 0, 0, 1}});
		similarity = GraphCodeMetric.calculateSimilarity(gcEx, gcEx2);
	}

	@Test
	void featureMetric_matchesPaper() {
		assertEquals(0.5f, similarity[0], 0.01f);
	}

	/** the paper divides by the n * n - n non diagonal fields of the intersection, 2 / 6 = 0.33 **/
	@Test
	void featureRelationshipMetric_isTheShareOfTheQueryEdgesFoundInTheOtherGraphCode() {
		assertEquals(2f / 5f, similarity[1], 0.0001f);
	}

	/** the paper sums the absolute differences of the fields as a distance, 1 / 6 = 0.16 **/
	@Test
	void relationshipTypeMetric_isTheShareOfTheFoundEdgesWithAnEqualType() {
		assertEquals(1f / 2f, similarity[2], 0.0001f);
	}
}
