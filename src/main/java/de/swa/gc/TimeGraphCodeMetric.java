package de.swa.gc;

import java.util.Vector;

/**
 * Created by Patrick Steinert on 15.04.25.
 */
public class TimeGraphCodeMetric {

	public static float[] calculateSimilarity(TimeGraphCode tgc1, TimeGraphCode tgc2) {

		Vector<String> dict1 = tgc1.getDictionary();
		Vector<String> dict2 = tgc2.getDictionary();

		// Calculate the percentage of matching terms between the two dictionaries
		float similarity = 0.0f;
		
		// Count matching terms
		int matchCount = 0;
		for (String term1 : dict1) {
			for (String term2 : dict2) {
				if (term1.equals(term2)) {
					matchCount++;
					break; // Found a match for this term, move to the next one
				}
			}
		}
		
		// Calculate similarity as the ratio of matching terms to the size of the first dictionary
		// This gives us a value between 0 and 1
		if (dict1.size() > 0) {
			similarity = (float) matchCount / dict1.size();
		}

		float similarityDtW = (float) computeDTW(tgc1.matrix, tgc2.matrix);

		return new float[]{similarity, similarityDtW}; // Return the similarity;
	}


	// Berechnet Frobenius-Norm zwischen zwei Matrizen
	public static double frobeniusDistance(int[][] A, int[][] B) {
		double sum = 0.0;
		
		// Get the dimensions of both matrices
		int rowsA = A.length;
		int colsA = (rowsA > 0) ? A[0].length : 0;
		int rowsB = B.length;
		int colsB = (rowsB > 0) ? B[0].length : 0;
		
		// Calculate the maximum dimensions to create a padded view
		int maxRows = Math.max(rowsA, rowsB);
		int maxCols = Math.max(colsA, colsB);
		
		// Calculate Frobenius norm with zero padding for missing elements
		for (int i = 0; i < maxRows; i++) {
			for (int j = 0; j < maxCols; j++) {
				// Get value from A (0 if out of bounds)
				int valueA = (i < rowsA && j < colsA) ? A[i][j] : 0;
				// Get value from B (0 if out of bounds)
				int valueB = (i < rowsB && j < colsB) ? B[i][j] : 0;
				
				double diff = valueA - valueB;
				sum += diff * diff;
			}
		}
		return Math.sqrt(sum);
	}


	public static double computeDTW(int[][][] seqA, int[][][] seqB) {
		int n = seqA.length;
		int m = seqB.length;
		
		// Handle empty sequences
		if (n == 0 || m == 0) {
			return 1.0; // Return 1 similarity for empty sequences
		}
		
		// Create DTW matrix
		double[][] dtw = new double[n][m];
		
		// Initialize first cell
		dtw[0][0] = frobeniusDistance(seqA[0], seqB[0]);
		
		// Initialize first column
		for (int i = 1; i < n; i++) {
			dtw[i][0] = dtw[i - 1][0] + frobeniusDistance(seqA[i], seqB[0]);
		}
		
		// Initialize first row
		for (int j = 1; j < m; j++) {
			dtw[0][j] = dtw[0][j - 1] + frobeniusDistance(seqA[0], seqB[j]);
		}
		
		// Fill the DTW matrix
		for (int i = 1; i < n; i++) {
			for (int j = 1; j < m; j++) {
				double cost = frobeniusDistance(seqA[i], seqB[j]);
				dtw[i][j] = cost + Math.min(Math.min(dtw[i - 1][j], dtw[i][j - 1]), dtw[i - 1][j - 1]);
			}
		}
		
		// Normalize the result by the maximum possible path length
		double maxPathLength = n + m - 1;
		return 1.0 - (dtw[n - 1][m - 1] / maxPathLength); // Return a similarity measure (1.0 = identical, 0.0 = completely different)
	}
}
