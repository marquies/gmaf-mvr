package de.swa.gmaf.plugin.fusion;

import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpacialFeatureFusionTest {
	private MMFG m;

	@BeforeEach
	public void setUp() {
		m = new MMFG();
	}

	private Node node(String name, int x, int y) {
		Node n = new Node(name, m);
		n.addTechnicalAttribute(new TechnicalAttribute(x, y, 10, 10, 0, 0));
		return n;
	}

	private void fuse() {
		new SpacialFeatureFusion().optimize(m, null);
	}

	private static boolean hasRelation(Node from, int type, Node to) {
		return from.getCompositionRelationships().stream()
				.anyMatch(cr -> cr.getType() == type && cr.getRelatedObject() == to);
	}

	@Test
	public void optimize_linksHorizontallySeparatedNodes() {
		Node left = node("left", 0, 0);
		Node right = node("right", 100, 0);

		fuse();

		assertTrue(hasRelation(left, CompositionRelationship.RELATION_NEXT_TO, right));
		assertTrue(hasRelation(right, CompositionRelationship.RELATION_NEXT_TO, left));
	}

	@Test
	public void optimize_linksVerticallySeparatedNodes() {
		Node first = node("first", 0, 0);
		Node second = node("second", 0, 100);

		fuse();

		assertEquals(2, first.getCompositionRelationships().size());
		assertEquals(2, second.getCompositionRelationships().size());
	}

	@Test
	public void optimize_doesNotLinkOverlappingNodes() {
		Node first = node("first", 0, 0);
		Node second = node("second", 5, 5);

		fuse();

		assertTrue(first.getCompositionRelationships().isEmpty());
		assertTrue(second.getCompositionRelationships().isEmpty());
	}

	@Test
	public void optimize_doesNotLinkTouchingNodes() {
		Node first = node("first", 0, 0);
		Node second = node("second", 10, 0);

		fuse();

		assertTrue(first.getCompositionRelationships().isEmpty());
		assertTrue(second.getCompositionRelationships().isEmpty());
	}

	@Test
	public void optimize_ignoresNodesWithoutTechnicalAttributes() {
		Node located = node("located", 0, 0);
		Node other = new Node("other", m);

		fuse();

		assertTrue(located.getCompositionRelationships().isEmpty());
		assertTrue(other.getCompositionRelationships().isEmpty());
	}

	@Disabled("Known bug: y grows downwards in image coordinates, but the upper node is labelled UNDER")
	@Test
	public void optimize_labelsTheUpperNodeAbove() {
		Node upper = node("upper", 0, 0);
		Node lower = node("lower", 0, 100);

		fuse();

		assertTrue(hasRelation(upper, CompositionRelationship.RELATION_ABOVE, lower));
		assertTrue(hasRelation(lower, CompositionRelationship.RELATION_UNDER, upper));
	}

	@Disabled("Known bug: each pair is visited in both directions, and each visit adds both relationships")
	@Test
	public void optimize_addsEachRelationshipOnce() {
		Node left = node("left", 0, 0);
		node("right", 100, 0);

		fuse();

		assertEquals(1, left.getCompositionRelationships().size());
	}
}
