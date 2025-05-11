package de.swa.gc;

import java.util.List;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 15.04.25.
 */
public class TimeGraphCodeMetric {

	public static float calculateSimilarity(TimeGraphCode tgc1, TimeGraphCode tgc2) {

		Vector<String> dict1 = tgc1.getDictionary();
		Vector<String> dict2 = tgc2.getDictionary();

		// Calculate the percentage of equal word in the two dictionaries

		float similarity = 0.0f;

		for (int i = 0; i < dict1.size(); i++) {
			if (dict1.get(i).equals(dict2.get(i))) {
				similarity += 1.0f;
			}
		}

		similarity = similarity / dict1.size();

		return similarity;



	}


	// Berechnet Frobenius-Norm zwischen zwei Matrizen
	public static double frobeniusDistance(int[][] A, int[][] B) {
		double sum = 0.0;
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < A[0].length; j++) {
				double diff = A[i][j] - B[i][j];
				sum += diff * diff;
			}
		}
		return Math.sqrt(sum);
	}


	public static double computeDTW(int[][][] seqA, int[][][] seqB) {
		int n = seqA.length;
		int m = seqB.length;

		double[][] dtw = new double[n][m];

		// Initialisierung
		dtw[0][0] = frobeniusDistance(seqA[0], seqB[0]);

		for (int i = 1; i < n; i++)
			dtw[i][0] = dtw[i - 1][0] + frobeniusDistance(seqA[i], seqB[0]);
		for (int j = 1; j < m; j++)
			dtw[0][j] = dtw[0][j - 1] + frobeniusDistance(seqA[0], seqB[j]);

		// Matrix füllen
		for (int i = 1; i < n; i++) {
			for (int j = 1; j < m; j++) {
				double cost = frobeniusDistance(seqA[i], seqB[j]);
				dtw[i][j] = cost + Math.min(Math.min(dtw[i - 1][j], dtw[i][j - 1]), dtw[i - 1][j - 1]);
			}
		}

		return dtw[n - 1][m - 1]; // Endwert: Minimale kumulierte Distanz
	}
}
