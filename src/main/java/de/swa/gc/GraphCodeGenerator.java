package de.swa.gc;

import java.util.Vector;

import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.SemanticRelationship;
import de.swa.mmfg.builder.Flattener;

/** Graph Code Generator to calculate Graph Codes out of MMFGs
 * 
 * @author stefan_wagenpfeil
 */
public class GraphCodeGenerator  {
	private static final int LEAF_TYPE = 2;
	private static final int NODE_TYPE = 1;
	private static final int CHILD_RELATIONSHIP = 1;
	
	/** returns a Graph Code based on a MMFG **/
	public static GraphCode generate(MMFG m) {
		GraphCode gc = new GraphCode();
		Vector<String> dictionary = new Vector<String>();
		
		// Calculate the Graph Code Dictionary by the vocabulary terms of the MMFG
		Vector<Node> vocTerms = m.allNodes;
		for (Node n : vocTerms) {
			String term = n.getName();
			if (!term.equals("")) {
				if (term.startsWith("Sentence_")) continue;
				if (!dictionary.contains(term)) {
					dictionary.add(term);
				}
			}
		}
		gc.setDictionary(dictionary);
		
		// set the node values in the diagonal of the Graph Code
		for (Node n : vocTerms) {
			try {
				int nodeTypeValue = 1;
				gc.setValueForTerms(n.getName(), n.getName(), nodeTypeValue);

				if (n.getChildNodes().size() == 0) nodeTypeValue = LEAF_TYPE;
				else nodeTypeValue = NODE_TYPE;
				gc.setValueForTerms(n.getName(), n.getName(), nodeTypeValue);
			}
			catch (Exception ex) { 
//				System.out.println("GraphCodeGenerator vocTerms: " + ex.getMessage());
			}
		}
		
		// for each node in the MMFG, calculate relationship values
		for (Node n : vocTerms) {
			// Child Relationships
			for (Node child : n.getChildNodes()) {
				try {
					gc.setValueForTerms(n.getName(), child.getName(), CHILD_RELATIONSHIP);
				}
				catch (Exception x) {
					//x.printStackTrace();
				}
			}
			// Composition Relationships
			for (CompositionRelationship cr : n.getCompositionRelationships()) {
				try {
					gc.setValueForTerms(n.getName(), cr.getRelatedObject().getName(), cr.getType());
				}
				catch (Exception x) {
//					x.printStackTrace();
				}
			}
			
			// Semantic Relationships
			for (SemanticRelationship sr : n.getSemanticRelationships()) {

			}
		}
		
		try {
			for (MMFG mi : m.getCollectionElements()) {
				GraphCode gci = GraphCodeGenerator.generate(mi);
				gc.addGraphCode(gci);
			}
		}
		catch (Exception x) {
			System.out.println("GraphCodeGenerator " + x);
		}
				
		return gc;
	}
}
