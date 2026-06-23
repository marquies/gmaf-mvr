package de.swa.gc;

import java.io.File;
import java.util.Hashtable;
import java.util.Vector;

import de.swa.ui.MMFGCollection;
import de.swa.ui.Configuration;
import de.swa.ui.MMFGCollectionFactory;

/** this class contains operations for Graph Code Collections, i.e. connected recursive lists of Graph Codes
 * 
 * @author stefan_wagenpfeil
 */

public class GraphCodeCollection {
	/** returns the union of all Graph Codes of the collection **/
	public static GraphCode getCollectionGraphCode(GraphCode gc) {
		if (gc.getCollectionElements().size() == 0) return gc;
		else {
			return getUnion(gc.getCollectionElements());
		}
	}
	
	/** returns the Feature Relevant Graph Code based on statistical relevance of the collection's Graph Codes **/
	public static GraphCode getFeatureRelevantGraphCode(GraphCode gc1) {
		if (gcstop == null) calculateGCStop(MMFGCollectionFactory.createOrGetCollection().getCollectionGraphCodes(), false);
		return subtract(gc1, gcstop);
	}
	
	/** subtracts one Graph Code from the other **/
	public static GraphCode subtract(GraphCode gc1, GraphCode gc2) {
		GraphCode gc = new GraphCode();
		Vector<String> dictionary = new Vector<String>();
		
		for (String s : gc1.getDictionary()) {
			if (!gc2.getDictionary().contains(s)) dictionary.add(s);
		}

		gc.setDictionary(dictionary);
		for (String s : dictionary) {
			for (String t : dictionary) {
				int i = 0;
				try { i = gc1.getEdgeValueForTerms(s, t); } catch (Exception x) {}
				gc.setValueForTerms(s, t, i);
			}
		}
		return gc;
	}
	
	private static GraphCode gcstop = null;
	/** calculates the Graph Code of Stop-Words (i.e. irrelevant terms within the collection) **/
	public static GraphCode calculateGCStop(Vector<GraphCode> collection, boolean fromCache) {
		if (gcstop != null) return gcstop;
		
		if (fromCache) {
			try {
				GraphCode gc = GraphCodeIO.read(new File(Configuration.getInstance().getGraphCodeRepository() + File.separatorChar + "GCStop.gc"));
				gcstop = gc;
				return gcstop;
			}
			catch (Exception x) {
				return new GraphCode();
			}
		}
		
		gcstop = new GraphCode();
		Hashtable<String, Integer> termFrequency = new Hashtable<String, Integer>();
		for (GraphCode gc : collection) {
			for (String s : gc.getDictionary()) {
				if (termFrequency.containsKey(s)) {
					int freq = termFrequency.get(s);
					freq ++;
					termFrequency.put(s, freq);
				}
				else termFrequency.put(s,  1);
			}
		}

		Vector<String> stopWords = new Vector<String>();
//		stopWords.add("face_1");
//		stopWords.add("face_2");
//		stopWords.add("flash photography");
		
		int threshold = (int)(collection.size() * 0.7); 	// 80% relevance
		for (String s : termFrequency.keySet()) {
			int freq = termFrequency.get(s);
			if (freq > threshold) stopWords.add(s);
		}
		
		gcstop.setDictionary(stopWords);
		return gcstop;
	}
	
	/** returns the union of Graph Codes **/
	public static GraphCode getUnion(Vector<GraphCode> gcs) {
		GraphCode gc = new GraphCode();
		Vector<String> dictionary = new Vector<String>();
		
		for (GraphCode gci : gcs) {
			Vector<String> dict_i = gci.getDictionary();
			for (String s : dict_i) {
				if (!dictionary.contains(s)) dictionary.add(s);
			}
		}
		
		gc.setDictionary(dictionary);
		
		// add edges
		for (GraphCode gci : gcs) {
			for (String s : dictionary) {
				for (String t : dictionary) {
					int current = gc.getEdgeValueForTerms(s, t);
					int i = 0;
					try { i = gci.getEdgeValueForTerms(s, t); } catch (Exception x) {}
					gc.setValueForTerms(s, t, current + i);
				}
			}
		}
		return gc;
	}
	
	/** returns a summary of Graph Codes based on a ranking. The parameter top indicates the Level Of Detail **/
	public static GraphCode getSummaryGraphCode(GraphCode gc, int top) {
		if (gc.getCollectionElements().size() == 0) return gc;
		
		int size = gc.getCollectionElements().size();
		if (size < top) top = size;
		
//		float[][][] ranks = new float[size][size][3];
		float[] sum = new float[size];

		// calculate feature rank matrix
		for (int i = 0; i < size; i++) {
			GraphCode GC_i = gc.getCollectionElements().get(i);
			float sum_i = 0.0f;
			for (int j = 0; j < size; j++) {
				GraphCode GC_j = gc.getCollectionElements().get(j);
				float[] sim = GraphCodeMetric.calculateSimilarity(GC_i, GC_j);
//				ranks[i][j] = sim;
				// summarize rank for each GC
				sum_i += sim[0] * 1000000 + sim[1] * 1000 + sim[2];
			}
			sum[i] = sum_i;
		}

		Vector<GraphCode> result = new Vector<GraphCode>();
		float old_max = Float.MAX_VALUE;
		for (int i = 0; i < top; i++) {
			float localMax = 0.0f;
			int idx = -1;
			for (int j = 0; j < size; j++) {
				if (sum[j] > localMax && sum[j] < old_max) {
					localMax = sum[j];
					idx = j;
				}
			}
			try {
				result.add(gc.getCollectionElements().get(idx));
				old_max = localMax;	// skip this record next time
			}
			catch (Exception x) {
				System.out.println(x);
			}
		}
		
		return getUnion(result);
	}
}
