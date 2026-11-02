package de.swa.gmaf.plugin.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Timerange;
import de.swa.mmfg.builder.XMLEncodeDecode;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Vector;

public class UnionFeatureFusionTest {

	private static Vector<MMFG> collectionOf(MMFG... mmfgs) {
		Vector<MMFG> collection = new Vector<MMFG>();
		for (MMFG m : mmfgs) collection.add(m);
		return collection;
	}

	/**
	 * the first time range of a source MMFG should be carried over to the target
	 */
	@Test
	public void testCopiesFirstTimeRange() {
		MMFG source = new MMFG();
		Timerange first = new Timerange(new Date(0), new Date(10));
		source.addTimeRange(first);
		source.addTimeRange(new Timerange(new Date(10), new Date(20)));

		MMFG target = new MMFG();
		new UnionFeatureFusion().optimize(target, collectionOf(source));

		assertEquals(1, target.getTimeranges().size());
		assertSame(first, target.getTimeranges().get(0));
	}

	/**
	 * a source MMFG without time ranges, e.g. most stored MMFGs, should not fail
	 */
	@Test
	public void testSourceWithoutTimeRanges() {
		MMFG target = new MMFG();
		new UnionFeatureFusion().optimize(target, collectionOf(new MMFG()));

		assertTrue(target.getTimeranges().isEmpty());
	}

	/**
	 * XML without a timeranges element skips the field initializer when parsed,
	 * which leaves the time ranges of the source MMFG null
	 */
	@Test
	public void testSourceWithNullTimeRanges() {
		XMLEncodeDecode xml = new XMLEncodeDecode();
		MMFG source = xml.unflatten(xml.flatten(new MMFG()).replaceFirst("\\s*<timeranges/>", ""));
		assertNull(source.getTimeranges());

		MMFG target = new MMFG();
		new UnionFeatureFusion().optimize(target, collectionOf(source));

		assertTrue(target.getTimeranges().isEmpty());
	}
}
