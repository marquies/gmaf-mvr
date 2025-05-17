package de.swa.gc.processing;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import de.swa.gc.GraphCodeMetric;
import de.swa.gc.GraphCodeStrategy;
import de.swa.gc.DefaultGraphCodeStrategy;

/**
 * Default implementation of the CollectionProcessor interface.
 * 
 * This class provides a basic implementation for processing a collection of GraphCodeMeta objects.
 * It uses a GraphCodeStrategy to calculate the similarity between the query graph code and each graph code in the collection.
 */
public class DefaultCollectionProcessor extends CollectionProcessor {
    private Vector<GraphCodeMeta> collection;
    private GraphCodeStrategy graphCodeStrategy;

    /**
     * Default constructor.
     * 
     * Initializes the processor with the default GraphCodeStrategy.
     */
    public DefaultCollectionProcessor() {
        // Default to the standard GraphCodeStrategy if none is provided
        this.graphCodeStrategy = new DefaultGraphCodeStrategy();
    }

    /**
     * Constructor with a custom GraphCodeStrategy.
     * 
     * Initializes the processor with the provided GraphCodeStrategy.
     * 
     * @param graphCodeStrategy the custom GraphCodeStrategy to use
     */
    public DefaultCollectionProcessor(GraphCodeStrategy graphCodeStrategy) {
        this.graphCodeStrategy = graphCodeStrategy;
    }

    /**
     * Sets the GraphCodeStrategy to use.
     * 
     * @param graphCodeStrategy the new GraphCodeStrategy to use
     */
    public void setGraphCodeStrategy(GraphCodeStrategy graphCodeStrategy) {
        this.graphCodeStrategy = graphCodeStrategy;
    }

    /**
     * Preloads the index with the given collection of GraphCodeMeta objects.
     * 
     * @param collection the collection of GraphCodeMeta objects to preload
     */
    public void preloadIndex(Vector<GraphCodeMeta> collection) {
        this.collection = collection;
    }

    /**
     * Executes the processing of the collection.
     * 
     * Calculates the similarity between the query graph code and each graph code in the collection using the GraphCodeStrategy.
     */
    public void execute() {
        for (GraphCodeMeta meta : collection) {
            // Use the strategy to calculate similarity instead of directly calling GraphCodeMetric
            float[] sim = graphCodeStrategy.calculateSimilarity(gcQuery, meta.getGraphcode());
            meta.setMetric(sim);
        }
    }

    /**
     * Returns the sorted list of GraphCodeMeta objects.
     * 
     * Sorts the collection based on the similarity metric and returns the sorted list.
     * 
     * @return the sorted list of GraphCodeMeta objects
     */
    public Vector<GraphCodeMeta> getResultList() {
        Collections.sort(collection, new Comparator<GraphCodeMeta>() {
            public int compare(GraphCodeMeta m1, GraphCodeMeta m2) {
                float[] metric_a = m1.getMetric();
                float[] metric_b = m2.getMetric();
//              System.out.println("A: " + metric_a[0] + " " + metric_a[1] + " " + metric_a[2]);
//              System.out.println("B: " + metric_b[0] + " " + metric_b[1] + " " + metric_b[2]);

                if (operation == SIMILARITY) {
                    // calculate numeric values to support java-compatible comparison
                    float a = metric_a[0] * 100000 + metric_a[1] * 100 + metric_a[2];
                    float b = metric_b[0] * 100000 + metric_b[1] * 100 + metric_b[2];
//                  System.out.println("-> S " + (b-a));
                    return (int) (b - a);
                } else if (operation == RECOMMENDATION) {
                    // calculate numeric values to support java-compatible comparison
                    float a = metric_a[1] * 100000 + metric_a[0] * 100 + metric_a[2];
                    float b = metric_b[1] * 100000 + metric_b[0] * 100 + metric_b[2];
//                  System.out.println("-> R " + (b-a));

                    return (int) (b - a);

                }
                return 0;
            };
        });
        return collection;
    }
}
