package de.swa.experiment;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingMeasuresTest {
	private static final List<Boolean> HIT_MISS_HIT = Arrays.asList(true, false, true);

	@Test
	void precisionAt_dividesByK() {
		assertEquals(0.5, RankingMeasures.precisionAt(HIT_MISS_HIT, 2), 1e-9);
		assertEquals(2.0 / 3.0, RankingMeasures.precisionAt(HIT_MISS_HIT, 3), 1e-9);
	}

	@Test
	void precisionAt_countsMissingResultsAsMisses() {
		assertEquals(0.2, RankingMeasures.precisionAt(HIT_MISS_HIT.subList(0, 1), 5), 1e-9);
	}

	@Test
	void recallAt_dividesByRelevantResults() {
		assertEquals(0.25, RankingMeasures.recallAt(HIT_MISS_HIT, 4, 2), 1e-9);
		assertEquals(0.5, RankingMeasures.recallAt(HIT_MISS_HIT, 4, 3), 1e-9);
	}

	@Test
	void averagePrecisionAt_averagesOverRelevantResults() {
		assertEquals((1 + 2.0 / 3.0) / 3, RankingMeasures.averagePrecisionAt(HIT_MISS_HIT, 3, 3), 1e-9);
	}

	@Test
	void averagePrecisionAt_isLimitedByK() {
		assertEquals(0.5, RankingMeasures.averagePrecisionAt(HIT_MISS_HIT, 3, 2), 1e-9);
	}

	@Test
	void averagePrecisionAt_isOneForAPerfectTopK() {
		assertEquals(1.0, RankingMeasures.averagePrecisionAt(Arrays.asList(true, true), 100, 2), 1e-9);
	}

	@Test
	void averagePrecisionAt_equalsFullAveragePrecisionForLargeK() {
		assertEquals((1 + 2.0 / 3.0) / 4, RankingMeasures.averagePrecisionAt(HIT_MISS_HIT, 4, 1000), 1e-9);
	}

	@Test
	void measures_withoutRelevantResults_areZero() {
		assertEquals(0, RankingMeasures.recallAt(Collections.<Boolean>emptyList(), 0, 5), 1e-9);
		assertEquals(0, RankingMeasures.averagePrecisionAt(Collections.<Boolean>emptyList(), 0, 5), 1e-9);
	}
}
