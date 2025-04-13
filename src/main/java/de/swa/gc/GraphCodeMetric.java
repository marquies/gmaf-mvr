package de.swa.gc;

import java.util.Vector;

/** Implementation of the Graph Code Metric
 * 
 * @author stefan_wagenpfeil
 */

public class GraphCodeMetric {
    /** calculates the metric triple for Graph Codes based on a given query **/
    public static float[] calculateSimilarity(GraphCode gcQuery, GraphCode gc) {
        float node_metric = 0f;
        float edge_metric = 0f;
        float edge_type_metric = 0f;
        
        // node metric checks matching vocabulary terms
        Vector<String> voc = gcQuery.getDictionary();
        if (voc.isEmpty()) {
            // If query dictionary is empty, all metrics are 0
            return new float[] {0f, 0f, 0f};
        }
        
        int sim = 0;
        for (String s : voc) {
            if (s.trim().equals("")) continue;
            Vector<String> otherDict = gc.getDictionary();
            for (String t : otherDict) {
                if (s.equals(t)) sim++;
            }
        }
        if (sim > voc.size()) sim = voc.size();
        node_metric = (float)sim / (float)voc.size();
        if (Float.isNaN(node_metric)) node_metric = 0f;
        
        // edge metric checks matching edges of the non diagonal fields
        // edge type metric checks for corresponding type values
        int num_of_non_zero_edges = 0;
        int edge_metric_count = 0;
        int edge_type = 0;
        for (int i = 0; i < voc.size(); i++) {
            for (int j = 0; j < voc.size(); j++) {
                if (i != j) {
                    if (gcQuery.getValue(i, j) != 0) {
                        num_of_non_zero_edges++;
                        try {
                            int gc_edge = gc.getEdgeValueForTerms(voc.get(i), voc.get(j));
                            if (gc_edge != 0) edge_metric_count++;
                            if (gc_edge == gcQuery.getValue(i, j)) edge_type++;
                        }
                        catch (Exception x) {
                            x.printStackTrace();
                        }
                    }
                }
            }
        }
        
        // Avoid division by zero
        edge_metric = num_of_non_zero_edges > 0 ? (float)edge_metric_count / (float)num_of_non_zero_edges : 0f;
        edge_type_metric = edge_metric_count > 0 ? (float)edge_type / (float)edge_metric_count : 0f;
        
        return new float[] {node_metric, edge_metric, edge_type_metric};
    }
}
