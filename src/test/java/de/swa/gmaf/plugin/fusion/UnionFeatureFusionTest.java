package de.swa.gmaf.plugin.fusion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.Timerange;
import de.swa.mmfg.builder.XMLEncodeDecode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Vector;

public class UnionFeatureFusionTest {

	private static Vector<MMFG> collectionOf(MMFG... mmfgs) {
		Vector<MMFG> collection = new Vector<MMFG>();
		for (MMFG m : mmfgs) collection.add(m);
		return collection;
	}

	private static MMFG mmfg(String... terms) {
		MMFG m = new MMFG();
		for (String term : terms) m.addNode(new Node(term, m));
		return m;
	}

	private static void fuse(MMFG target, MMFG... others) {
		new UnionFeatureFusion().optimize(target, collectionOf(others));
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

	@Test
	public void optimize_addsNodesOfOtherMmfgs() {
		MMFG target = mmfg("car");
		MMFG other = mmfg("tree");
		Node tree = other.getNodes().get(0);

		fuse(target, other);

		assertEquals(2, target.getNodes().size());
		assertSame(tree, target.getNodes().get(1));
		assertTrue(target.getAllNodes().contains(tree));
	}

	@Test
	public void optimize_addsNodesOfEveryMmfg() {
		MMFG target = mmfg("car");

		fuse(target, mmfg("tree"), mmfg("road"));

		assertEquals(3, target.getNodes().size());
	}

	@Test
	public void optimize_withEmptyCollection_changesNothing() {
		MMFG target = mmfg("car");

		fuse(target);

		assertEquals(1, target.getNodes().size());
	}

	@Disabled("Known bug: getNodesByTerm never returns null, so the check doesn't stop duplicates")
	@Test
	public void optimize_doesNotDuplicateExistingTerms() {
		MMFG target = mmfg("car");

		fuse(target, mmfg("car", "tree"));

		assertEquals(1, target.getNodesByTerm("car").size());
	}
}
