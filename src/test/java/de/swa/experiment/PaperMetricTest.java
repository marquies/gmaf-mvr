package de.swa.experiment;

import de.swa.gc.GraphCode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PaperMetricTest {

	private static GraphCode graphCode(String[] dictionary, int[][] matrix) {
		GraphCode gc = new GraphCode();
		gc.setDictionary(new Vector<String>(Arrays.asList(dictionary)));
		for (int i = 0; i < dictionary.length; i++) {
			for (int j = 0; j < dictionary.length; j++) gc.setValue(i, j, matrix[i][j]);
		}
		return gc;
	}

	/** GC_ex and GC_ex2 of figures 1 and 2 in the ICIVR 2021 paper **/
	@Test
	void calculate_reproducesTheWorkedExample() {
		GraphCode gcEx = graphCode(new String[] {"Person", "Head", "Human Being", "Individual", "Hat", "above"},
				new int[][] {{1, 3, 4, 4, 0, 0}, {0, 1, 0, 0, 3, 0}, {0, 0, 1, 0, 0, 0}, {0, 0, 0, 2, 0, 0},
						{0, 0, 0, 0, 2, 5}, {0, 0, 0, 0, 0, 6}});
		GraphCode gcEx2 = graphCode(new String[] {"above", "Dog", "Head", "Animal", "Hat"},
				new int[][] {{5, 0, 0, 0, 0}, {0, 1, 3, 4, 0}, {0, 0, 1, 0, 3}, {0, 0, 0, 2, 0}, {6, 0, 0, 0, 1}});

		assertArrayEquals(new float[] {0.5f, 2f / 6f, 1f / 6f}, PaperMetric.calculate(gcEx, gcEx2), 0.001f);
	}
}
