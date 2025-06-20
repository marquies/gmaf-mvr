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
		
		// Calculate DTW only for matching elements
		float similarityMatchingDTW = (float) computeMatchingDTW(tgc1, tgc2);

		return new float[]{similarity, similarityMatchingDTW, similarityDtW }; // Return the similarity values
	}


	// Berechnet Frobenius-Norm zwischen zwei Matrizen
	public static double frobeniusDistance_1(int[][] A, int[][] B) {
		double sum = 0.0;

		// Get the dimensions of both matrices
		int rowsA = A.length;
		int colsA = (rowsA > 0) ? A[0].length : 0;
		int rowsB = B.length;
		int colsB = (rowsB > 0) ? B[0].length : 0;
		double threshold = 1;


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

	// helper ---------------------------------------------------------------
	private static double frobeniusNorm(int[][] M) {
		double s = 0;
		for (int[] row : M)
			for (int v : row)
				s += v * v;
		return Math.sqrt(s);
	}

	private static boolean isZeroSlice(int[][] M) {
		for (int[] row : M)
			for (int v : row)
				if (v != 0) return false;
		return true;
	}

	// 1.  Normalised Frobenius distance ------------------------------------
	public static double frobeniusDistance_2(int[][] A, int[][] B) {

		// a) identical absence of data
		if (isZeroSlice(A) && isZeroSlice(B)) return 0.0;

		// b) one slice empty, the other not then maximal dissimilarity
		if (isZeroSlice(A) || isZeroSlice(B)) return 5;

		// c) regular case – energy–normalised distance in [0,1]
		double diffSq = 0;
		int maxRows = Math.max(A.length,  B.length);
		int maxCols = Math.max(A.length>0 ? A[0].length : 0,
				B.length>0 ? B[0].length : 0);

		for (int i = 0; i < maxRows; i++) {
			for (int j = 0; j < maxCols; j++) {
				int a = (i < A.length && j < A[0].length) ? A[i][j] : 0;
				int b = (i < B.length && j < B[0].length) ? B[i][j] : 0;
				double d = a - b;
				diffSq += d * d;
			}
		}

		double denom = frobeniusNorm(A) + frobeniusNorm(B);
		return (denom == 0) ? 0.0               // should never happen (caught above)
				: Math.sqrt(diffSq) / denom;
	}

	// Overloaded version with coverage penalty for sparse matching
	public static double frobeniusDistance_2(int[][] A, int[][] B, int totalTermsA, int totalTermsB, int matchingTerms) {
		double baseDistance = frobeniusDistance_2(A, B);
		
		// Calculate coverage penalty based on how much of the original data is being compared
		double coverageA = (double) matchingTerms / totalTermsA;
		double coverageB = (double) matchingTerms / totalTermsB;
		double averageCoverage = (coverageA + coverageB) / 2.0;
		
		// Apply coverage penalty: reduce similarity when coverage is low
		// Uses a quadratic penalty to more heavily penalize low coverage
		double coveragePenalty = averageCoverage * averageCoverage;
		
		// For distance measures, we want to increase distance when coverage is low
		// So we scale the base distance by the inverse of coverage penalty
		return baseDistance / Math.max(coveragePenalty, 0.1); // Prevent division by very small numbers
	}

	public static double hammingDistance(int[][] A, int[][] B) {
		int count = 0;
		for (int i = 0; i < A.length; i++) {
			for (int j = 0; j < A[0].length; j++) {
				if (A[i][j] != B[i][j]) count++;
			}
		}
		return count;
	}

	public static double l1Distance(int[][] A, int[][] B) {
		double sum = 0.0;
		// Get the minimum dimensions to avoid index out of bounds
		int rows = Math.min(A.length, B.length);
		for (int i = 0; i < rows; i++) {
			int cols = Math.min(A[i].length, B[i].length);
			for (int j = 0; j < cols; j++) {
				sum += Math.abs(A[i][j] - B[i][j]);
			}
		}
		return sum;
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
		dtw[0][0] = frobeniusDistance_2(seqA[0], seqB[0]);
		
		// Initialize first column
		for (int i = 1; i < n; i++) {
			dtw[i][0] = dtw[i - 1][0] + frobeniusDistance_2(seqA[i], seqB[0]);
		}
		
		// Initialize first row
		for (int j = 1; j < m; j++) {
			dtw[0][j] = dtw[0][j - 1] + frobeniusDistance_2(seqA[0], seqB[j]);
		}
		
		// Fill the DTW matrix
		for (int i = 1; i < n; i++) {
			for (int j = 1; j < m; j++) {
				double cost = frobeniusDistance_2(seqA[i], seqB[j]);
				dtw[i][j] = cost + Math.min(Math.min(dtw[i - 1][j], dtw[i][j - 1]), dtw[i - 1][j - 1]);
			}
		}

		//double pathLen        = n + m - 1;
		//double avgCost        = dtw[n-1][m-1] / pathLen;   // ∈ [0,1]
		//return 1.0 - avgCost;
		// Normalize the result by the maximum possible path length
		double maxPathLength = n + m - 1;
		// Ensure the similarity measure is bounded between 0.0 and 1.0
		double normalizedDistance = Math.min(1.0, dtw[n - 1][m - 1] / maxPathLength);
		return 1 - normalizedDistance; // Return a similarity measure (1.0 = identical, 0.0 = completely different)
	}

	/**
	 * Computes DTW similarity only for elements that appear in both TimeGraphCodes
	 * @param tgc1 First TimeGraphCode
	 * @param tgc2 Second TimeGraphCode
	 * @return Similarity score based on DTW of matching elements only
	 */
	public static double computeMatchingDTW(TimeGraphCode tgc1, TimeGraphCode tgc2) {
		Vector<String> dict1 = tgc1.getDictionary();
		Vector<String> dict2 = tgc2.getDictionary();
		
		// Find common terms between the dictionaries
		Vector<String> commonTerms = new Vector<>();
		int checkCount = 0;
		for (String term1 : dict1) {
			if (!term1.equals("root-asset")) { // && dict2.contains(term1)) {
				commonTerms.add(term1);
				if (dict2.contains(term1)) {
					checkCount++;
				}
			}
		}

		
		// If no common terms, return 0 similarity
		if (commonTerms.isEmpty() || checkCount == 0) {
			return 0.0;
		}
		
		// Calculate coverage penalty based on how many terms actually match
		int totalTerms1 = dict1.size() - 1; // Exclude "root-asset"
		int totalTerms2 = dict2.size() - 1; // Exclude "root-asset"
		double coverageA = (double) checkCount / totalTerms1;
		double coverageB = (double) checkCount / totalTerms2;
		double averageCoverage = (coverageA + coverageB) / 2.0;
		
		// Apply coverage penalty with exponential decay for low coverage
		// This heavily penalizes cases where only a few terms match
		double coveragePenalty = Math.pow(averageCoverage, 2.0); // Quadratic penalty
		
		// Early return with low similarity if coverage is too low (less than 20%)
		if (averageCoverage < 0.2) {
			return coveragePenalty * 0.1; // Maximum 2% similarity for very low coverage
		}
		
		// Get the interval lengths
		int intervals1 = tgc1.getIntervalLength();
		int intervals2 = tgc2.getIntervalLength();
		
		// Create new matrices with only matching elements
		int[][][] matchedSeqA = new int[intervals1][commonTerms.size()][commonTerms.size()];
		int[][][] matchedSeqB = new int[intervals2][commonTerms.size()][commonTerms.size()];
		
		// Fill the matrices with values for common terms
		for (int i = 0; i < commonTerms.size(); i++) {
			for (int j = 0; j < commonTerms.size(); j++) {
				String term1 = commonTerms.get(i);
				String term2 = commonTerms.get(j);
				
				// Fill matrix A
				for (int t = 0; t < intervals1; t++) {
					matchedSeqA[t][i][j] = tgc1.getValueAtTimePoint(term1, term2, t);
				}
				
				// Fill matrix B
				for (int t = 0; t < intervals2; t++) {
					matchedSeqB[t][i][j] = tgc2.getValueAtTimePoint(term1, term2, t);
				}
			}
		}
		
		// Compute DTW on the matched sequences and apply coverage penalty
		double baseSimilarity = computeDTW(matchedSeqA, matchedSeqB);
		return baseSimilarity * coveragePenalty;
	}
}
