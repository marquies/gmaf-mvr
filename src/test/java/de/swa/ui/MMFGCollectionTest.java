package de.swa.ui;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeMetric;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.GeneralMetadata;
import de.swa.mmfg.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.Vector;

import static de.swa.gc.GraphCodeMetric.calculateSimilarity;
import static org.junit.jupiter.api.Assertions.*;

class MMFGCollectionTest {
    private MMFGCollection collection;
    private MMFG testMMFG;
    private File testFile;

    @BeforeEach
    void setUp() {
        collection = MMFGCollection.getInstance();
        testMMFG = new MMFG();
        GeneralMetadata gm = new GeneralMetadata();
        testFile = new File("test.jpg");
        gm.setFileReference(testFile);
        gm.setFileName("test.jpg");
        testMMFG.setGeneralMetadata(gm);
    }

    @Test
    void getInstance_ReturnsSameInstance() {
        MMFGCollection instance1 = MMFGCollection.getInstance();
        MMFGCollection instance2 = MMFGCollection.getInstance();
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    void getInstance_WithSession_ReturnsSameInstanceForSameSession() {
        String sessionId = "test-session";
        MMFGCollection instance1 = MMFGCollection.getInstance(sessionId);
        MMFGCollection instance2 = MMFGCollection.getInstance(sessionId);
        assertSame(instance1, instance2, "getInstance with same session ID should return the same instance");
    }

    @Test
    void addToCollection_AddsMMFGToCollection() {
        collection.addToCollection(testMMFG);
        Vector<MMFG> collectionVector = collection.getCollection();
        assertTrue(collectionVector.contains(testMMFG), "Collection should contain the added MMFG");
    }

    @Test
    void getMMFGForFile_ReturnsCorrectMMFG() {
        collection.addToCollection(testMMFG);
        MMFG retrieved = collection.getMMFGForFile(testFile);
        assertSame(testMMFG, retrieved, "getMMFGForFile should return the correct MMFG");
    }

    @Test
    void getMMFGForId_ReturnsCorrectMMFG() {
        collection.addToCollection(testMMFG);
        UUID id = testMMFG.getGeneralMetadata().getId();
        MMFG retrieved = collection.getMMFGForId(id);
        assertSame(testMMFG, retrieved, "getMMFGForId should return the correct MMFG");
    }

    @Test
    void replaceMMFGInCollection_ReplacesExistingMMFG() {
        collection.addToCollection(testMMFG);
        MMFG newMMFG = new MMFG();
        GeneralMetadata gm = new GeneralMetadata();
        gm.setFileReference(testFile);
        gm.setFileName("test.jpg");
        newMMFG.setGeneralMetadata(gm);

        collection.replaceMMFGInCollection(newMMFG, testFile);
        
        MMFG retrieved = collection.getMMFGForFile(testFile);
        assertSame(newMMFG, retrieved, "replaceMMFGInCollection should replace the existing MMFG");
        assertFalse(collection.getCollection().contains(testMMFG), "Old MMFG should be removed from collection");
    }

    @Test
    void getName_ReturnsCollectionName() {
        String name = collection.getName();
        assertNotNull(name, "Collection name should not be null");
    }

    @Test
    void getCollectionGraphCodes_ReturnsGraphCodesForCollection() {
        collection.addToCollection(testMMFG);
        Vector<GraphCode> graphCodes = collection.getCollectionGraphCodes();
        assertNotNull(graphCodes, "Collection graph codes should not be null");
        assertFalse(graphCodes.isEmpty(), "Collection graph codes should not be empty");
    }

    @Test
    void getOrGenerateGraphCode_ReturnsCachedGraphCode() {
        GraphCode firstCall = collection.getOrGenerateGraphCode(testMMFG);
        GraphCode secondCall = collection.getOrGenerateGraphCode(testMMFG);
        assertSame(firstCall, secondCall, "getOrGenerateGraphCode should return cached graph code on subsequent calls");
    }

    @Test
    void processQuery_ReturnsExpectedResults() {
        // Add test MMFG to collection
        collection.addToCollection(testMMFG);
        
        // Create a test query GraphCode
        GraphCode queryGraphCode = collection.getOrGenerateGraphCode(testMMFG);
        
        // Test different query types
        Vector<MMFG> results = collection.processQuery(queryGraphCode, 0); // Default type
        assertNotNull(results, "Query results should not be null");
        assertFalse(results.isEmpty(), "Query results should not be empty");
        
        // Test with different query type
        Vector<MMFG> recommendedResults = collection.processQuery(queryGraphCode, 1);
        assertNotNull(recommendedResults, "Recommended results should not be null");
    }

    @Test
    void processQuery_WithMultipleGraphCodes_ReturnsSortedResults() {
        // Create a fresh collection for this test
        MMFGCollection testCollection = new MMFGCollection();
        // Initialize the collection
        testCollection.init();
        
        // Create three different MMFGs
        MMFG exactMatch = new MMFG();
        GeneralMetadata gmExact = new GeneralMetadata();
        File exactFile = new File("exact_match.jpg");
        gmExact.setFileReference(exactFile);
        gmExact.setFileName("exact_match.jpg");
        exactMatch.setGeneralMetadata(gmExact);
        // Add some nodes that will match exactly
        Node exactNode1 = new Node("common_feature", "value1", exactMatch);
        Node exactNode2 = new Node("common_feature2", "value2", exactMatch);
        exactMatch.addNode(exactNode1);
        exactMatch.addNode(exactNode2);
        
        MMFG partialMatch = new MMFG();
        GeneralMetadata gmPartial = new GeneralMetadata();
        File partialFile = new File("partial_match.jpg");
        gmPartial.setFileReference(partialFile);
        gmPartial.setFileName("partial_match.jpg");
        partialMatch.setGeneralMetadata(gmPartial);
        // Add one matching node and one different node
        Node partialNode1 = new Node("common_feature", "value1", partialMatch);
        Node partialNode2 = new Node("different_node", "value", partialMatch);
        partialMatch.addNode(partialNode1);
        partialMatch.addNode(partialNode2);
        
        MMFG differentOne = new MMFG();
        GeneralMetadata gmDiff = new GeneralMetadata();
        File diffFile = new File("different.jpg");
        gmDiff.setFileReference(diffFile);
        gmDiff.setFileName("different.jpg");
        differentOne.setGeneralMetadata(gmDiff);
        // Add completely different features
        Node diffNode1 = new Node("very_different", "other_value", differentOne);
        Node diffNode2 = new Node("another_different", "value2", differentOne);
        differentOne.addNode(diffNode1);
        differentOne.addNode(diffNode2);

        try {
            // Add all MMFGs to collection
            testCollection.addToCollection(exactMatch);
            testCollection.addToCollection(partialMatch);
            testCollection.addToCollection(differentOne);
            
            // Create query using the exact match as template
            GraphCode queryGraphCode = testCollection.getOrGenerateGraphCode(exactMatch);
            
            // Process query
            Vector<MMFG> results = testCollection.processQuery(queryGraphCode, 0);
            
            // Verify results
            assertNotNull(results, "Query results should not be null");
            assertEquals(3, results.size(), "Should return all three results");
            
            // Print similarity scores for debugging
            for (MMFG result : results) {
                System.out.println("File: " + result.getGeneralMetadata().getFileName() + 
                                 " Similarity: " + result.getTempSimilarity()[0]);
            }
            
            // The exact match should be first (highest similarity)
            assertEquals("exact_match.jpg", results.get(0).getGeneralMetadata().getFileName(), 
                        "First result should be exact match");
            assertEquals("partial_match.jpg", results.get(1).getGeneralMetadata().getFileName(),
                        "Third result should be partial match");
            assertEquals("different.jpg", results.get(2).getGeneralMetadata().getFileName(),
                        "Second result should be the one with common feature");

            // Verify that the similarity scores are in descending order
            float previousSimilarity[] = new float[] {Float.MAX_VALUE,Float.MAX_VALUE,Float.MAX_VALUE};
            for (MMFG result : results) {

                float currentSimilarity[] = result.getTempSimilarity();
                int compare = compare(currentSimilarity, previousSimilarity);
                assertTrue(compare >= 0,
                          "Results should be ordered by descending similarity. Current: " + 
                          currentSimilarity + " Previous: " + previousSimilarity);
                previousSimilarity = currentSimilarity;
            }
        } finally {
            // Cleanup generated graph code files
            String graphCodeRepo = Configuration.getInstance().getGraphCodeRepository();
            String[] filesToDelete = {"different.jpg.gc", "exact_match.jpg.gc", "partial_match.jpg.gc"};
            for (String fileName : filesToDelete) {
                File gcFile = new File(graphCodeRepo + File.separatorChar + fileName);
                if (gcFile.exists()) {
                    gcFile.delete();
                }
            }
        }
    }
    private int compare(float[] metric_a, float[] metric_b) {

        // calculate numeric values to support java-compatible comparison
        float a = metric_a[1] * 100000 + metric_a[0] * 100 + metric_a[2];
        float b = metric_b[1] * 100000 + metric_b[0] * 100 + metric_b[2];

        return (int)(b - a);
    };

}
