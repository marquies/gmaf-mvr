package de.swa.gmaf.plugin.fusion;

import java.util.Vector;

import de.swa.gmaf.plugin.FeatureFusionStrategy;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

public class UnionFeatureFusion implements FeatureFusionStrategy {
	public void optimize(MMFG mmfg, Vector<MMFG> collection) {
		for (MMFG m : collection) {
			for (Node n : m.getNodes()) {
				if (mmfg.getNodesByTerm(n.getName()) != null) {
					mmfg.addNode(n);
				}
			}
			mmfg.addTimeRange(m.getTimeranges().get(0));
		}
	}
}
