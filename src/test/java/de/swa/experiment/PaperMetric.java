package de.swa.experiment;

import de.swa.gc.GraphCode;

import java.util.Vector;

/**
 * Reference implementation of the Graph Code metric as printed in Wagenpfeil et al., "Graph Codes -
 * 2D Projections of Multimedia Feature Graphs for Fast and Effective Retrieval", ICIVR 2021. It
 * follows the formulas literally, also where they read differently from GraphCodeMetric:
 * M_FR counts the second Graph Code's non-zero fields, and M_RT sums absolute differences.
 */
public final class PaperMetric {
	private PaperMetric() {}

	/** returns the triple (M_F, M_FR, M_RT) **/
	public static float[] calculate(GraphCode gcI, GraphCode gcJ) {
		Vector<String> dictI = gcI.getDictionary();
		Vector<String> intersection = new Vector<String>(dictI);
		intersection.retainAll(gcJ.getDictionary());

		int n = intersection.size();
		float mF = dictI.isEmpty() ? 0f : (float) n / dictI.size();
		int nonDiagonalFields = n * n - n;
		if (nonDiagonalFields == 0) return new float[] {mF, 0f, 0f};

		int edges = 0;
		int typeDifference = 0;
		for (String s : intersection) {
			for (String t : intersection) {
				if (s.equals(t)) continue;
				int valueI = gcI.getEdgeValueForTerms(s, t);
				int valueJ = gcJ.getEdgeValueForTerms(s, t);
				if (valueJ != 0) edges++;
				typeDifference += Math.abs(valueI - valueJ);
			}
		}
		return new float[] {mF, (float) edges / nonDiagonalFields, (float) typeDifference / nonDiagonalFields};
	}
}
