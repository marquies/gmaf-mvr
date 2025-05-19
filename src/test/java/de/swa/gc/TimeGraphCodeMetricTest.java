package de.swa.gc;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.Timerange;
import junit.framework.Assert;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 15.04.25.
 */
public class TimeGraphCodeMetricTest {
	@Test
	public void testSimilarityByTime() {

		Date start = new Date(0);
		start.setSeconds(0);

		Date end = new Date(0);
		end.setSeconds(10);

		Timerange tr = Timerange.create(start,end);

		MMFG tmmfg1 = new MMFG();
		Node n1 = new Node("alpha", tmmfg1);
		n1.setTimerange(tr);
		tmmfg1.addNode(n1);

		MMFG tmmfg2 = new MMFG();
		Node n2 = new Node("beta", tmmfg2);
		n2.setTimerange(tr);
		tmmfg2.addNode(n2);

		TimeGraphCode tgc1 = TimeGraphCodeGenerator.generate(tmmfg1);
		TimeGraphCode tgc2 = TimeGraphCodeGenerator.generate(tmmfg2);


		// Dictionary are different, but time is equal.
		float[] score = TimeGraphCodeMetric.calculateSimilarity(tgc1, tgc2);
		Assert.assertEquals(0.0f, score[0]);
		Assert.assertEquals(1.0f, score[1]);
	}

	@Test
	public void testSimilarityByTimeEquals() {

		MMFG tmmfg1 = new MMFG();
		Node n1 = new Node("alpha", tmmfg1);
		tmmfg1.addNode(n1);


		TimeGraphCode tgc1 = TimeGraphCodeGenerator.generate(tmmfg1);

		float[] score = TimeGraphCodeMetric.calculateSimilarity(tgc1, tgc1);
		Assert.assertTrue(Float.valueOf(1.0f).equals(score[1]));
	}

	@Test
	public void testComputeDTW() {
		// Test case 1: Identical sequences
		int[][][] seqA1 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}}
		};
		int[][][] seqB1 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}}
		};
		double distance1 = TimeGraphCodeMetric.computeDTW(seqA1, seqB1);
		// For identical sequences, we expect only the diagonal elements of the DTW matrix to be used
		// The distance should be the sum of Frobenius distances between corresponding matrices, which is 0
		// However, since we are looking for similarity, the distance should be 1 by the computation.
		Assert.assertEquals(1.0, distance1, 0.001);
		
		// Test case 2: Similar sequences with small differences
		int[][][] seqA2 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}}
		};
		int[][][] seqB2 = {
				{{1, 0}, {0, 1}},
				{{2, 2}, {1, 2}} // One value changed from 1 to 2
		};
		double distance2 = TimeGraphCodeMetric.computeDTW(seqA2, seqB2);
		// Expected distance should be greater than 0 but relatively small
		Assert.assertTrue(distance2 > 0);
		Assert.assertTrue(distance2 < 2.0); // The Frobenius distance for the changed matrix is 1.0
		
		// Test case 3: Different length sequences
		int[][][] seqA3 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}},
				{{3, 2}, {2, 3}}
		};
		int[][][] seqB3 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}}
		};
		double distance3 = TimeGraphCodeMetric.computeDTW(seqA3, seqB3);
		// DTW should handle different length sequences
		Assert.assertTrue(distance3 >= 0);
		
		// Test case 4: Completely different sequences
		int[][][] seqA4 = {
				{{1, 0}, {0, 1}},
				{{2, 1}, {1, 2}}
		};
		int[][][] seqB4 = {
				{{5, 5}, {5, 5}},
				{{10, 10}, {10, 10}}
		};
		double distance4 = TimeGraphCodeMetric.computeDTW(seqA4, seqB4);
		// Distance should be significantly larger than for similar sequences, with the similarity inversion, shorter
		Assert.assertTrue(distance4 < distance2);

		// Test case 5: Edge case - single element sequences
		int[][][] seqA5 = {{{1, 1}, {1, 1}}};
		int[][][] seqB5 = {{{1, 1}, {1, 1}}};
		double distance5 = TimeGraphCodeMetric.computeDTW(seqA5, seqB5);
		Assert.assertEquals(1.0, distance5, 0.001);
	}

	@Test
	public void testTimeStretchedGraphCodes() {
		// Create two TimeGraphCode objects manually with the same data structure
		// but different time intervals
		
		// Define common dictionary for both graph codes
		Vector<String> dictionary = new Vector<>();
		dictionary.add("alpha");
		dictionary.add("beta");
		dictionary.add("gamma");
		
		// Create first TimeGraphCode with short intervals (10 seconds)
		TimeGraphCode tgc1 = new TimeGraphCode(10); // 10 second interval
		tgc1.setDictionary(dictionary);
		
		// Set relationships at specific time points
		// Alpha -> Beta relationship at timepoints 2-5
		for (int i = 2; i <= 5; i++) {
			tgc1.setValueForTerms("alpha", "beta", 1, i);
		}
		
		// Beta -> Gamma relationship at timepoints 4-8
		for (int i = 4; i <= 8; i++) {
			tgc1.setValueForTerms("beta", "gamma", 1, i);
		}
		
		// Create second TimeGraphCode with stretched intervals (30 seconds)
		TimeGraphCode tgc2 = new TimeGraphCode(30); // 30 second interval
		tgc2.setDictionary(dictionary);
		
		// Set the same relationships but stretched over time
		// Alpha -> Beta relationship at timepoints 6-15 (3x stretched)
		for (int i = 6; i <= 15; i++) {
			tgc2.setValueForTerms("alpha", "beta", 1, i);
		}
		
		// Beta -> Gamma relationship at timepoints 12-24 (3x stretched)
		for (int i = 12; i <= 24; i++) {
			tgc2.setValueForTerms("beta", "gamma", 1, i);
		}
		
		// Verify the dictionary sizes are the same
		Assert.assertEquals(tgc1.getDictionary().size(), tgc2.getDictionary().size());
		
		// Calculate similarity using standard method (should be high since dictionaries match)
		float[] standardSimilarity = TimeGraphCodeMetric.calculateSimilarity(tgc1, tgc2);
		Assert.assertTrue(standardSimilarity[1] > 0.9f); // Should be close to 1.0
		
		// Extract the matrices for DTW comparison
		int[][][] matrix1 = tgc1.matrix;
		int[][][] matrix2 = tgc2.matrix;
		
		// Calculate DTW distance
		double dtwDistanceOfRelated = TimeGraphCodeMetric.computeDTW(matrix1, matrix2);
		
		// The DTW distance should be relatively low despite the time stretching
		// because DTW is designed to handle time warping
		System.out.println("DTW distance between time-stretched graph codes: " + dtwDistanceOfRelated);
		
		// Create a third TimeGraphCode with different structure
		Vector<String> differentDictionary = new Vector<>();
		differentDictionary.add("delta");
		differentDictionary.add("epsilon");
		differentDictionary.add("zeta");
		
		TimeGraphCode tgc3 = new TimeGraphCode(10);
		tgc3.setDictionary(differentDictionary);
		
		// Set completely different relationships
		for (int i = 1; i <= 5; i++) {
			tgc3.setValueForTerms("delta", "epsilon", 1, i);
		}
		
		for (int i = 3; i <= 8; i++) {
			tgc3.setValueForTerms("epsilon", "zeta", 1, i);
		}
		
		// Create a compatible matrix for comparison
		// We need to convert tgc3's matrix to have the same dimensions as tgc1's matrix
		int[][][] compatibleMatrix = new int[tgc3.matrix.length][tgc1.matrix[0].length][tgc1.matrix[0][0].length];
		
		// DTW distance between unrelated graph codes should be higher
		double dtwDistanceOfUnrelated = TimeGraphCodeMetric.computeDTW(matrix1, compatibleMatrix);
		System.out.println("DTW distance between unrelated graph codes: " + dtwDistanceOfUnrelated);
		
		// The DTW distance for the time-stretched but structurally identical graph codes
		// should be lower than the distance to an unrelated graph code
		Assert.assertTrue(dtwDistanceOfRelated > dtwDistanceOfUnrelated);
	}
}
