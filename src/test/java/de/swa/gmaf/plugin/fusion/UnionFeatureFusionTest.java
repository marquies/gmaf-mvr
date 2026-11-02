package de.swa.gmaf.plugin.fusion;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnionFeatureFusionTest {

	private static MMFG mmfg(String... terms) {
		MMFG m = new MMFG();
		for (String term : terms) m.addNode(new Node(term, m));
		return m;
	}

	private static void fuse(MMFG target, MMFG... others) {
		new UnionFeatureFusion().optimize(target, new Vector<>(Arrays.asList(others)));
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
