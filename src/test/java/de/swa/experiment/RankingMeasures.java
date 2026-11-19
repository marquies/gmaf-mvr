package de.swa.experiment;

import java.util.List;

/** retrieval measures over a ranked list, given as one relevance flag per result **/
public final class RankingMeasures {
	private RankingMeasures() {}

	/** relevant results in the first k positions, divided by k **/
	public static double precisionAt(List<Boolean> ranked, int k) {
		return (double) hitsAt(ranked, k) / k;
	}

	/** relevant results in the first k positions, divided by all relevant results **/
	public static double recallAt(List<Boolean> ranked, int relevant, int k) {
		return relevant == 0 ? 0 : (double) hitsAt(ranked, k) / relevant;
	}

	/** sums the precision at each relevant position up to k, divided by min(relevant, k) **/
	public static double averagePrecisionAt(List<Boolean> ranked, int relevant, int k) {
		if (relevant == 0) return 0;
		int hits = 0;
		double sum = 0;
		for (int i = 0; i < Math.min(k, ranked.size()); i++) {
			if (ranked.get(i)) {
				hits++;
				sum += (double) hits / (i + 1);
			}
		}
		return sum / Math.min(relevant, k);
	}

	private static int hitsAt(List<Boolean> ranked, int k) {
		int hits = 0;
		for (int i = 0; i < Math.min(k, ranked.size()); i++) {
			if (ranked.get(i)) hits++;
		}
		return hits;
	}
}
