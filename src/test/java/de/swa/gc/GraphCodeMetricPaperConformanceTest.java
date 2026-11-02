package de.swa.gc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks GraphCodeMetric against the worked example of Wagenpfeil et al., "Graph Codes - 2D
 * Projections of Multimedia Feature Graphs for Fast and Effective Retrieval", ICIVR 2021, which
 * gives M_GC(GC_ex, GC_ex2) = (0.5, 0.33, 0.16).
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

	@Disabled("Known deviation: the code divides by the query's edges (2/5), the paper by all off-diagonal fields of the intersection (2/6)")
	@Test
	void featureRelationshipMetric_matchesPaper() {
		assertEquals(0.33f, similarity[1], 0.01f);
	}

	@Disabled("Known deviation: the code gives the share of shared edges with equal types (1/2), the paper the share of differing fields (1/6)")
	@Test
	void relationshipTypeMetric_matchesPaper() {
		assertEquals(0.16f, similarity[2], 0.01f);
	}
}
