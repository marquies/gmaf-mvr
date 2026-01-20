package de.swa.mmfg;

import java.net.URL;
import java.util.Vector;

/** data type to represent semantic relationships **/
public class SemanticRelationship {
	private URL relatedNode;
	private String description;
	private Vector<Timerange> timeRange;
	
	public SemanticRelationship() {
		timeRange = new Vector<>();
	}
	
	public SemanticRelationship(URL relatedNode, String description) {
		this.relatedNode = relatedNode;
		this.description = description;
		this.timeRange = new Vector<>();
	}

	public URL getRelatedNode() {
		return relatedNode;
	}

	public void setRelatedNode(URL relatedNode) {
		this.relatedNode = relatedNode;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Vector<Timerange> getTimeRange() {
		return timeRange;
	}
	
	public void setTimeRange(Vector<Timerange> timeRange) {
		this.timeRange = timeRange;
	}
}
