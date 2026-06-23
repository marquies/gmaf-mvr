package de.swa.mmfg.builder;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.Test;

import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeatureVectorBuilderTest {

	@Test
	public void mergeIntoFeatureVector_movesChildNodesFromDeltaRootIntoBaseCurrentNode() {
		MMFG base = new MMFG();
		Node baseRoot = new Node("Root-Asset", base);
		base.setCurrentNode(baseRoot);
		Node baseChild = new Node("existing", base);
		baseRoot.addChildNode(baseChild);

		MMFG delta = new MMFG();
		Node deltaRoot = new Node("Root-Asset", delta);
		Node deltaChild = new Node("new", delta);
		deltaRoot.addChildNode(deltaChild);
		delta.addNode(deltaRoot);

		FeatureVectorBuilder.mergeIntoFeatureVector(base, delta);
		java.util.Vector<Node> children = baseRoot.getChildNodes();
		assertEquals(2, children.size());
		assertTrue(children.stream().anyMatch(n -> n.getName().equals("new")));
	}
}
