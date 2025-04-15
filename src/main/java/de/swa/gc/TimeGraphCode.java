package de.swa.gc;

import java.util.Vector;

/**
 * Created by Patrick Steinert on 14.04.25.
 */
public class TimeGraphCode extends GraphCode {

	protected int[][][] matrix;

	protected int intervalLength;

	public TimeGraphCode(int intervalLength) {
		this.intervalLength = intervalLength;
	}

	public void setDictionary(Vector<String> d) {
		dictionary = new Vector<String>();
		for (String s : d) {
			s = s.toLowerCase();
			dictionary.add(s.toLowerCase());
		}
		matrix = new int[intervalLength][d.size()][d.size()];
	}

	/** sets the matrix value for two feature vocabulary terms at a specific time point **/
	public void setValueForTerms(String term1, String term2, int val, int timePoint) {
		int idx_a = dictionary.indexOf(term1.toLowerCase());
		int idx_b = dictionary.indexOf(term2.toLowerCase());
		
		// Ensure timePoint is within bounds
		if (timePoint >= 0 && timePoint < intervalLength) {
			matrix[timePoint][idx_a][idx_b] = val;
		}
	}
	
	/** 
	 * Override the parent method to set value at time point 0 by default
	 * for backward compatibility
	 */
	@Override
	public void setValueForTerms(String term1, String term2, int val) {
		setValueForTerms(term1, term2, val, 0);
	}

	public void addTimeGraphCode(TimeGraphCode gci) {
	}

	public int getEdgeValueForTerms(String term1, String term2) {
		int translated_x = dictionary.indexOf(term1.toLowerCase());
		int translated_y = dictionary.indexOf(term2.toLowerCase());
		try {
			return matrix[0][translated_x][translated_y];
		}
		catch (Exception ex) {
//			System.out.println("T1: " + term1 + " (" + translated_x + ")   T2: " + term2 + " (" + translated_y + ")");
			return 0;
		}
	}

	public int getEdgeValueForTerms(String term1, String term2, int i) {
		int translated_x = dictionary.indexOf(term1.toLowerCase());
		int translated_y = dictionary.indexOf(term2.toLowerCase());
		try {
			return matrix[i][translated_x][translated_y];
		}
		catch (Exception ex) {
//			System.out.println("T1: " + term1 + " (" + translated_x + ")   T2: " + term2 + " (" + translated_y + ")");
			return 0;
		}
	}
}
