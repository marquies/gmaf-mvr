package de.swa.gc;

import de.swa.mmfg.MMFG;

import java.io.File;

/**
 * Default implementation of GraphCodeStrategy using existing GraphCodeGenerator and GraphCodeIO.
 */
public class DefaultGraphCodeStrategy implements GraphCodeStrategy {
    private static final String FILE_EXTENSION = ".gc"; // As observed in MMFGCollection

    @Override
    public GraphCode generateGraphCode(MMFG mmfg) {
        if (mmfg == null) return null; // Or throw IllegalArgumentException
        return GraphCodeGenerator.generate(mmfg);
    }

    @Override
    public void saveGraphCode(GraphCode gc, File targetFile) {
        if (gc == null || targetFile == null) return; // Or throw IllegalArgumentException
        GraphCodeIO.write(gc, targetFile);
    }

    @Override
    public GraphCode loadGraphCode(File sourceFile) {
        if (sourceFile == null || !sourceFile.exists()) return null;
        return GraphCodeIO.read(sourceFile);
    }

    @Override
    public String getGraphCodeFileExtension() {
        return FILE_EXTENSION;
    }
    
    @Override
    public float[] calculateSimilarity(GraphCode gcQuery, GraphCode gc) {
        return GraphCodeMetric.calculateSimilarity(gcQuery, gc);
    }
}
