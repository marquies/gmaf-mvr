package de.swa.gc;

import de.swa.mmfg.MMFG;

import java.io.File;

/**
 * Strategy interface for generating, saving, and loading GraphCodes.
 */
public interface GraphCodeStrategy {
    /**
     * Generates a GraphCode from an MMFG.
     * @param mmfg The MMFG to process.
     * @return The generated GraphCode.
     */
    GraphCode generateGraphCode(MMFG mmfg);

    /**
     * Saves a GraphCode to a file.
     * @param gc The GraphCode to save.
     * @param targetFile The file to save the GraphCode to.
     */
    void saveGraphCode(GraphCode gc, File targetFile);

    /**
     * Loads a GraphCode from a file.
     * @param sourceFile The file to load the GraphCode from.
     * @return The loaded GraphCode, or null if the file doesn't exist or an error occurs.
     */
    GraphCode loadGraphCode(File sourceFile);

    /**
     * Gets the file extension used for storing GraphCodes managed by this strategy.
     * @return The file extension (e.g., ".gc").
     */
    String getGraphCodeFileExtension();
    
    /**
     * Calculates similarity between two GraphCodes.
     * @param gcQuery The query GraphCode.
     * @param gc The GraphCode to compare against the query.
     * @return A float array containing similarity metrics [node_metric, edge_metric, edge_type_metric].
     */
    float[] calculateSimilarity(GraphCode gcQuery, GraphCode gc);
}
