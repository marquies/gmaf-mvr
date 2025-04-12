package de.swa.gc;

import org.junit.jupiter.api.Test;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.*;

class GraphCodeMetricTest {

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
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
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
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
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
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
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
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(1.0f, similarity[1], 0.001f, "Edge metric should be 1.0 for identical edges");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 for different edge types");
    }
    
    @Test
    void calculateSimilarity_EmptyGraphs() {
        // Create two empty graph codes
        GraphCode gc1 = new GraphCode();
        GraphCode gc2 = new GraphCode();
        
        // Set empty dictionaries to avoid NaN
        gc1.setDictionary(new Vector<>());
        gc2.setDictionary(new Vector<>());
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
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
        
        float[] similarity = GraphCodeMetric.calculateSimilarity(gc1, gc2);
        
        assertEquals(1.0f, similarity[0], 0.001f, "Node metric should be 1.0 for identical vocabularies");
        assertEquals(0.0f, similarity[1], 0.001f, "Edge metric should be 0.0 for no edges");
        assertEquals(0.0f, similarity[2], 0.001f, "Edge type metric should be 0.0 for no edges");
    }
}
