package de.swa.gc;

import de.swa.mmfg.MMFG;

import java.io.File;
import java.util.Vector;

/**
 * Default implementation of GraphCodeStrategy using existing GraphCodeGenerator and GraphCodeIO.
 */
public class TimeGraphCodeStrategy implements GraphCodeStrategy {
    private static final String FILE_EXTENSION = ".gc"; // As observed in MMFGCollection

    @Override
    public GraphCode generateGraphCode(MMFG mmfg) {
        if (mmfg == null) return null; // Or throw IllegalArgumentException
        return TimeGraphCodeGenerator.generate(mmfg);
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
        // If either is not a TimeGraphCode, fall back to regular similarity calculation
        if (!(gcQuery instanceof TimeGraphCode) || !(gc instanceof TimeGraphCode)) {
            return GraphCodeMetric.calculateSimilarity(gcQuery, gc);
        }
        
//        TimeGraphCode timeGcQuery = (TimeGraphCode) gcQuery;
//        TimeGraphCode timeGc = (TimeGraphCode) gc;
//
//        // Get the interval length from both graph codes
//        int queryIntervalLength = timeGcQuery.getIntervalLength();
//        int gcIntervalLength = timeGc.getIntervalLength();
//
//        // If either has no time points, fall back to regular similarity at time point 0
//        if (queryIntervalLength <= 0 || gcIntervalLength <= 0) {
//            return GraphCodeMetric.calculateSimilarity(gcQuery, gc);
//        }
//
//        // Calculate similarity at each time point and average the results
//        float totalNodeMetric = 0f;
//        float totalEdgeMetric = 0f;
//        float totalEdgeTypeMetric = 0f;
//        int timePointsCompared = 0;
//
//        // Compare time points up to the minimum interval length
//        int minIntervalLength = Math.min(queryIntervalLength, gcIntervalLength);
//        for (int i = 0; i < minIntervalLength; i++) {
//            // Calculate metrics for this time point
//            float[] metrics = calculateTimePointSimilarity(timeGcQuery, timeGc, i);
//
//            // Add to totals
//            totalNodeMetric += metrics[0];
//            totalEdgeMetric += metrics[1];
//            totalEdgeTypeMetric += metrics[2];
//            timePointsCompared++;
//        }
//
//        // Calculate averages
//        float avgNodeMetric = timePointsCompared > 0 ? totalNodeMetric / timePointsCompared : 0f;
//        float avgEdgeMetric = timePointsCompared > 0 ? totalEdgeMetric / timePointsCompared : 0f;
//        float avgEdgeTypeMetric = timePointsCompared > 0 ? totalEdgeTypeMetric / timePointsCompared : 0f;


        float[] sim = TimeGraphCodeMetric.calculateSimilarity((TimeGraphCode) gcQuery, (TimeGraphCode) gc);

        return new float[] {sim[0], sim[1], 0.0f};
    }
    
    /**
     * Calculates similarity between two TimeGraphCodes at a specific time point.
     * 
     * @param gcQuery The query TimeGraphCode.
     * @param gc The TimeGraphCode to compare against.
     * @param timePoint The time point to compare at.
     * @return A float array containing similarity metrics [node_metric, edge_metric, edge_type_metric].
     */
    private float[] calculateTimePointSimilarity(TimeGraphCode gcQuery, TimeGraphCode gc, int timePoint) {
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
        
        // edge metric checks matching edges of the non diagonal fields at the specific time point
        // edge type metric checks for corresponding type values at the specific time point
        int num_of_non_zero_edges = 0;
        int edge_metric_count = 0;
        int edge_type = 0;
        for (int i = 0; i < voc.size(); i++) {
            for (int j = 0; j < voc.size(); j++) {
                if (i != j) {
                    // Get value at the specific time point
                    int queryValue = gcQuery.getValueAtTimePoint(i, j, timePoint);
                    if (queryValue != 0) {
                        num_of_non_zero_edges++;
                        try {
                            // Get the corresponding edge value at the same time point
                            int gc_edge = gc.getValueAtTimePoint(voc.get(i), voc.get(j), timePoint);
                            if (gc_edge != 0) edge_metric_count++;
                            if (gc_edge == queryValue) edge_type++;
                        }
                        catch (Exception x) {
                            // Ignore exceptions and continue
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
