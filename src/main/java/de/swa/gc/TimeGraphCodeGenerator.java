package de.swa.gc;

import de.swa.mmfg.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 14.04.25.
 */
public class TimeGraphCodeGenerator {
	private static final int LEAF_TYPE = 2;
	private static final int NODE_TYPE = 1;
	private static final int CHILD_RELATIONSHIP = 3;
	private static final int APPEAR_TOGETHER = 2;
	private static final int APPEAR_SEPARATELY = 1;

	/**
	 * returns a Time Graph Code based on a MMFG
	 **/
	public static TimeGraphCode generate(MMFG m) {
		// find min max time interval
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

		Date minTime = null;
		Date maxTime = null;
		Timerange minTimerange = findMinTimeRange(m);

		if (minTimerange != null) {
			minTime = minTimerange.getBegin();
			System.out.println(sdf.format(minTime));
		} else {
			minTime = new Date(0);
		}

		Timerange maxTimerange = findMaxTimeRange(m);
		if (maxTimerange != null) {
			maxTime = maxTimerange.getEnd();
			System.out.println(sdf.format(maxTime));
		} else {
			maxTime = new Date(0);
		}


		//gc.setMinTime(minTime);
		//gc.setMaxTime(maxTime);

		long diff = Objects.requireNonNull(maxTime).getTime() - Objects.requireNonNull(minTime).getTime();

		TimeGraphCode gc = new TimeGraphCode((int) (diff / 1000));
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

		// for each node in the MMFG, calculate relationship values
		for (Node n : vocTerms) {

			// Set the node values if the element is present at interval
			try {
				if (n.getTimerange() != null) {
					int nodeTypeValue = 1;
					Timerange timerange = n.getTimerange();
					
					// Calculate proper elapsed time points instead of using getSeconds()
					int[] timePoints = calculateTimePoints(timerange, minTime.getTime(), diff);
					int start = timePoints[0];
					int end = timePoints[1];
					
					for (int i = start; i <= end; i++) {
						gc.setValueForTerms(n.getName(), n.getName(), APPEAR_SEPARATELY, i);
					}

				}
			} catch (Exception x) {
				// Log the exception for debugging
				System.err.println("Error processing timerange for node " + n.getName() + ": " + x.getMessage());
			}


			// Same appearance with other nodes
			for (Node other : vocTerms) {

				if (n.getName().equals(other.getName())) {
					continue;
				}
				try {
					if (n.getTimerange() != null && other.getTimerange() != null) {
						// Both nodes have timeranges - use the overlapping period
						processTimerangeRelationship(gc, n.getName(), other.getName(),
								APPEAR_TOGETHER, n.getTimerange(), other.getTimerange(),
								minTime.getTime(), diff);
						// Neither has timerange - use default time point 0
						//gc.setValueForTerms(n.getName(), other.getName(), APPEAR_TOGETHER, 0);
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
			}

			// Child Relationships
			for (Node child : n.getChildNodes()) {
				try {
					if (n.getTimerange() != null && child.getTimerange() != null) {
						// Both nodes have timeranges - use the overlapping period
						processTimerangeRelationship(gc, n.getName(), child.getName(),
								CHILD_RELATIONSHIP, n.getTimerange(), child.getTimerange(),
								minTime.getTime(), diff);
					} else if (n.getTimerange() != null) {
						// Only parent has timerange
						int[] timePoints = calculateTimePoints(n.getTimerange(), minTime.getTime(), diff);
						for (int i = timePoints[0]; i <= timePoints[1]; i++) {
							gc.setValueForTerms(n.getName(), child.getName(), CHILD_RELATIONSHIP, i);
						}
					} else if (child.getTimerange() != null) {
						// Only child has timerange
						int[] timePoints = calculateTimePoints(child.getTimerange(), minTime.getTime(), diff);
						for (int i = timePoints[0]; i <= timePoints[1]; i++) {
							gc.setValueForTerms(n.getName(), child.getName(), CHILD_RELATIONSHIP, i);
						}
					} else {
						// Neither has timerange - use default time point 0
						gc.setValueForTerms(n.getName(), child.getName(), CHILD_RELATIONSHIP, 0);
					}
				} catch (Exception x) {
					//x.printStackTrace();
				}
			}

			// Composition Relationships
			for (CompositionRelationship cr : n.getCompositionRelationships()) {
				try {
					Node relatedNode = cr.getRelatedObject();

					if (!cr.getTimeRange().isEmpty()) {
						// Composition relationship has its own timeranges
						for (Timerange tr : cr.getTimeRange()) {
							if (n.getTimerange() != null) {
								// Both relationship and node have timeranges - use the overlapping period
								processTimerangeRelationship(gc, n.getName(), relatedNode.getName(),
										cr.getType(), n.getTimerange(), tr,
										minTime.getTime(), diff);
							} else {
								// Only relationship has timerange
								int[] timePoints = calculateTimePoints(tr, minTime.getTime(), diff);
								for (int i = timePoints[0]; i <= timePoints[1]; i++) {
									gc.setValueForTerms(n.getName(), relatedNode.getName(), cr.getType(), i);
								}
							}
						}
					} else if (n.getTimerange() != null && relatedNode.getTimerange() != null) {
						// No relationship timerange, but both nodes have timeranges
						processTimerangeRelationship(gc, n.getName(), relatedNode.getName(),
								cr.getType(), n.getTimerange(), relatedNode.getTimerange(),
								minTime.getTime(), diff);
					} else if (n.getTimerange() != null) {
						// Only source node has timerange
						int[] timePoints = calculateTimePoints(n.getTimerange(), minTime.getTime(), diff);
						for (int i = timePoints[0]; i <= timePoints[1]; i++) {
							gc.setValueForTerms(n.getName(), relatedNode.getName(), cr.getType(), i);
						}
					} else if (relatedNode.getTimerange() != null) {
						// Only target node has timerange
						int[] timePoints = calculateTimePoints(relatedNode.getTimerange(), minTime.getTime(), diff);
						for (int i = timePoints[0]; i <= timePoints[1]; i++) {
							gc.setValueForTerms(n.getName(), relatedNode.getName(), cr.getType(), i);
						}
					} else {
						// No timeranges available - use default time point 0
						gc.setValueForTerms(n.getName(), relatedNode.getName(), cr.getType(), 0);
					}
				} catch (Exception x) {
					//x.printStackTrace();
				}
			}

			// Semantic Relationships
			for (SemanticRelationship sr : n.getSemanticRelationships()) {
				// Similar implementation could be added for semantic relationships if needed
			}


		}

		try {
			for (MMFG mi : m.getCollectionElements()) {
				TimeGraphCode gci = TimeGraphCodeGenerator.generate(mi);
				gc.addTimeGraphCode(gci);
			}
		} catch (Exception x) {
			System.out.println("TimeGraphCodeGenerator " + x);
		}

		return gc;
	}

	/**
	 * Process a relationship between two timeranges, setting values for their overlap period
	 */
	private static void processTimerangeRelationship(TimeGraphCode gc, String sourceName,
													 String targetName, int relationshipType, Timerange sourceTimerange,
													 Timerange targetTimerange, long minTime, long totalDuration) {

		// Find the overlapping time period
		Date overlapStart = sourceTimerange.getBegin().after(targetTimerange.getBegin()) ?
				sourceTimerange.getBegin() : targetTimerange.getBegin();

		Date overlapEnd = sourceTimerange.getEnd().before(targetTimerange.getEnd()) ?
				sourceTimerange.getEnd() : targetTimerange.getEnd();

		// Only process if there is an actual overlap
		if (!overlapStart.after(overlapEnd)) {
			Timerange overlapTimerange = new Timerange();
			overlapTimerange.setBegin(overlapStart);
			overlapTimerange.setEnd(overlapEnd);

			int[] timePoints = calculateTimePoints(overlapTimerange, minTime, totalDuration);
			for (int i = timePoints[0]; i <= timePoints[1]; i++) {
				gc.setValueForTerms(sourceName, targetName, relationshipType, i);
			}
		}
	}

	/**
	 * Calculate the time point indices for the beginning and end of a timerange
	 *
	 * @param timerange     The timerange to calculate points for
	 * @param minTime       The minimum time (in milliseconds)
	 * @param totalDuration The total duration (in milliseconds)
	 * @return An array with [beginTimePoint, endTimePoint]
	 */
	private static int[] calculateTimePoints(Timerange timerange, long minTime, long totalDuration) {
		int[] result = new int[2];

		// Calculate begin time point - convert to elapsed seconds from start
		long beginTime = timerange.getBegin().getTime();
		long beginElapsedMs = beginTime - minTime;
		result[0] = (int) (beginElapsedMs / 1000); // Convert to seconds

		// Calculate end time point - convert to elapsed seconds from start  
		long endTime = timerange.getEnd().getTime();
		long endElapsedMs = endTime - minTime;
		result[1] = (int) (endElapsedMs / 1000); // Convert to seconds

		// Ensure we don't exceed the total duration in seconds
		int maxTimePoint = (int) (totalDuration / 1000);
		result[0] = Math.max(0, Math.min(result[0], maxTimePoint));
		result[1] = Math.max(0, Math.min(result[1], maxTimePoint));

		return result;
	}
	
	/**
	 * Convert a timestamp to elapsed seconds from the video start
	 * 
	 * @param timestamp The timestamp to convert
	 * @param videoStartTime The start time of the video (in milliseconds)
	 * @return Elapsed seconds from video start
	 */
	private static int getElapsedSeconds(Date timestamp, long videoStartTime) {
		if (timestamp == null) return 0;
		long elapsedMs = timestamp.getTime() - videoStartTime;
		return (int) Math.max(0, elapsedMs / 1000);
	}

	public static Timerange findMaxTimeRange(MMFG mmfg) {
		Date maxDate = new Date(0); // Initialize with earliest possible date
		Timerange maxTimerange = null;

		// Check MMFG's own timeranges
		if(mmfg.getTimeranges() != null) {
			for (Timerange timerange : mmfg.getTimeranges()) {
				if (timerange != null && timerange.getEnd() != null && timerange.getEnd().after(maxDate)) {
					maxDate = timerange.getEnd();
					maxTimerange = timerange;
				}
			}
		}

		// Check all nodes recursively
		for (Node node : mmfg.getNodes()) {
			Timerange nodeMax = findMaxTimeRangeInNode(node);
			if (nodeMax != null && nodeMax.getEnd() != null && nodeMax.getEnd().after(maxDate)) {
				maxDate = nodeMax.getEnd();
				maxTimerange = nodeMax;
			}
		}

		return maxTimerange;
	}

	private static Timerange findMaxTimeRangeInNode(Node node) {
		Date maxDate = new Date(0);
		Timerange maxTimerange = null;

		// Check node's own timerange
		if (node.getTimerange() != null && node.getTimerange().getEnd().after(maxDate)) {
			maxDate = node.getTimerange().getEnd();
			maxTimerange = node.getTimerange();
		}

		// Check composition relationships
		for (CompositionRelationship cr : node.getCompositionRelationships()) {
			for (Timerange tr : cr.getTimeRange()) {
				if (tr.getEnd().after(maxDate)) {
					maxDate = tr.getEnd();
					maxTimerange = tr;
				}
			}
		}

		// Check semantic relationships
		for (SemanticRelationship sr : node.getSemanticRelationships()) {
			for (Timerange tr : sr.getTimeRange()) {
				if (tr.getEnd().after(maxDate)) {
					maxDate = tr.getEnd();
					maxTimerange = tr;
				}
			}
		}

		// Recursively check child nodes
		for (Node child : node.getChildNodes()) {
			Timerange childMax = findMaxTimeRangeInNode(child);
			if (childMax != null && childMax.getEnd().after(maxDate)) {
				maxDate = childMax.getEnd();
				maxTimerange = childMax;
			}
		}

		return maxTimerange;
	}

	public static Timerange findMinTimeRange(MMFG mmfg) {
		Date minDate = new Date(Long.MAX_VALUE);
		Timerange minTimerange = null;

		// Check MMFG's own timeranges
		if (mmfg.getTimeranges() != null) {
			for (Timerange timerange : mmfg.getTimeranges()) {
				if (timerange != null && timerange.getBegin() != null && timerange.getBegin().before(minDate)) {
					minDate = timerange.getBegin();
					minTimerange = timerange;
				}
			}
		}

		// Check all nodes recursively
		for (Node node : mmfg.getNodes()) {
			Timerange nodeMin = findMinTimeRangeInNode(node);
			if (nodeMin != null && nodeMin.getBegin() != null && nodeMin.getBegin().before(minDate)) {
				minDate = nodeMin.getBegin();
				minTimerange = nodeMin;
			}
		}

		return minTimerange;
	}

	private static Timerange findMinTimeRangeInNode(Node node) {
		Date minDate = new Date(Long.MAX_VALUE);
		Timerange minTimerange = null;

		// Check node's own timerange
		if (node.getTimerange() != null && node.getTimerange().getBegin().before(minDate)) {
			minDate = node.getTimerange().getBegin();
			minTimerange = node.getTimerange();
		}

		// Check composition relationships
		for (CompositionRelationship cr : node.getCompositionRelationships()) {
			for (Timerange tr : cr.getTimeRange()) {
				if (tr.getBegin().before(minDate)) {
					minDate = tr.getBegin();
					minTimerange = tr;
				}
			}
		}

		// Check semantic relationships
		for (SemanticRelationship sr : node.getSemanticRelationships()) {
			for (Timerange tr : sr.getTimeRange()) {
				if (tr.getBegin().before(minDate)) {
					minDate = tr.getBegin();
					minTimerange = tr;
				}
			}
		}

		// Recursively check child nodes
		for (Node child : node.getChildNodes()) {
			Timerange childMin = findMinTimeRangeInNode(child);
			if (childMin != null && childMin.getBegin().before(minDate)) {
				minDate = childMin.getBegin();
				minTimerange = childMin;
			}
		}

		return minTimerange;
	}
}
