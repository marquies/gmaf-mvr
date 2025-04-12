package de.swa.gc;

import org.junit.jupiter.api.Test;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.*;

class GraphCodeMetricV2Test {

    /**
     * Improved version of calculateSimilarity that handles empty dictionaries correctly.
     * This is a proposed fix for the original implementation.
     */
    private static float[] calculateSimilarityV2(GraphCode gcQuery, GraphCode gc) {
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

    @Test
    void calculateSimilarity_ExactMatch() {
        // Create two identical graph codes
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add same vocabulary terms to both
        Vector<String> dict1 = new Vector<>();
        dict1.add("term1");
        dict1.add("term2");
        gc1.setDictionary(dict1);
        gc2.setDictionary(dict1);
        
        // Add same edges to both
        gc1.setValue(0, 1, 1); // Edge from term1 to term2 with type 1
        gc2.setValue(0, 1, 1); // Same edge in gc2
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(1.0f, similarity[1], 0.001f, "Edge metric should be 1.0 for identical edges");
        assertEquals(1.0f, similarity[2], 0.001f, "Edge type metric should be 1.0 for identical edge types");
    }
    
    @Test
    void calculateSimilarity_PartialNodeMatch() {
        // Create two graph codes with partially matching vocabulary
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add terms to gc1
        Vector<String> dict1 = new Vector<>();
        dict1.add("term1");
        dict1.add("term2");
        gc1.setDictionary(dict1);
        
        // Add only one matching term to gc2
        Vector<String> dict2 = new Vector<>();
        dict2.add("term1");
        dict2.add("different");
        gc2.setDictionary(dict2);
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        assertEquals(0.5f, similarity[0], 0.001f, "Node metric should be 0.5 for 50% vocabulary match");
    }
    
    @Test
    void calculateSimilarity_PartialEdgeMatch() {
        // Create two graph codes with same vocabulary but different edges
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add same terms to both
        Vector<String> dict = new Vector<>();
        dict.add("term1");
        dict.add("term2");
        dict.add("term3");
        gc1.setDictionary(dict);
        gc2.setDictionary(dict);
        
        // Add two edges to gc1
        gc1.setValue(0, 1, 1); // Edge from term1 to term2
        gc1.setValue(1, 2, 2); // Edge from term2 to term3
        
        // Add only one matching edge to gc2
        gc2.setValue(0, 1, 1); // Edge from term1 to term2 (same type)
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(0.5f, similarity[1], 0.001f, "Edge metric should be 0.5 for 50% edge match");
        assertEquals(1.0f, similarity[2], 0.001f, "Edge type metric should be 1.0 for matching edge types");
    }
    
    @Test
    void calculateSimilarity_DifferentEdgeTypes() {
        // Create two graph codes with same vocabulary and edges but different edge types
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add same terms to both
        Vector<String> dict = new Vector<>();
        dict.add("term1");
        dict.add("term2");
        gc1.setDictionary(dict);
        gc2.setDictionary(dict);
        
        // Add same edge but with different types
        gc1.setValue(0, 1, 1); // Edge from term1 to term2 with type 1
        gc2.setValue(0, 1, 2); // Edge from term1 to term2 with type 2
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(1.0f, similarity[1], 0.001f, "Edge metric should be 1.0 for identical edges");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 for different edge types");
    }
    
    @Test
    void calculateSimilarity_EmptyGraphs() {
        // Create two empty graph codes
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Set empty dictionaries
        gc1.setDictionary(new Vector<>());
        gc2.setDictionary(new Vector<>());
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        // When query graph is empty, all metrics should be 0
        assertEquals(0.0f, similarity[0], 0.001f, "Node metric should be 0.0 for empty graphs");
        assertEquals(0.0f, similarity[1], 0.001f, "Edge metric should be 0.0 for empty graphs");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 for empty graphs");
    }
    
    @Test
    void calculateSimilarity_NoEdges() {
        // Create two graph codes with same vocabulary but no edges
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add same terms to both
        Vector<String> dict = new Vector<>();
        dict.add("term1");
        dict.add("term2");
        gc1.setDictionary(dict);
        gc2.setDictionary(dict);
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(0.0f, similarity[1], 0.001f, "Edge metric should be 0.0 for no edges");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 for no edges");
    }
    
    @Test
    void calculateSimilarity_EmptyTarget() {
        // Test when query has terms but target is empty
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Add terms only to query
        Vector<String> dict1 = new Vector<>();
        dict1.add("term1");
        dict1.add("term2");
        gc1.setDictionary(dict1);
        
        // Leave target empty
        gc2.setDictionary(new Vector<>());
        
        float[] similarity = calculateSimilarityV2(gc1, gc2);
        
        // Since query has terms but target is empty, node metric should be 0
        assertEquals(0.0f, similarity[0], 0.001f, "Node metric should be 0.0 when target is empty");
        assertEquals(0.0f, similarity[1], 0.001f, "Edge metric should be 0.0 when target is empty");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 when target is empty");
    }
}
