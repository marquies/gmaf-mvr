package de.swa.mmfg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Vector;

/** data type to represent Composition Relationships **/

public class CompositionRelationship {
	public static final int RELATION_NEXT_TO = 11;
	public static final int RELATION_BEFORE = 12;
	public static final int RELATION_BEHIND = 13;
	public static final int RELATION_ABOVE = 14;
	public static final int RELATION_UNDER = 15;
	public static final int RELATION_PART_OF = 16;
	public static final int RELATION_ATTACHED_TO = 17;
	public static final int RELATION_RELATED_TO = 18;
	public static final int RELATION_DOING = 19;
	public static final int RELATION_PROPERTY = 20;
	public static final int RELATION_DESCRIPTION = 21;
	
	private int type;
	@JsonIgnore
	private transient Node relatedObject;
	private Vector<Timerange> timeRange;
	
	public CompositionRelationship() {
		timeRange = new Vector<>();
	}
	
	public CompositionRelationship(int type, Node n) {
		this.type = type;
		relatedObject = n;
		timeRange = new Vector<>();
	}
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public Node getRelatedObject() {
		return relatedObject;
	}
	public void setRelatedObject(Node relatedObject) {
		this.relatedObject = relatedObject;
	}
	
	public Vector<Timerange> getTimeRange() {
		return timeRange;
	}
	
	public void setTimeRange(Vector<Timerange> timeRange) {
		this.timeRange = timeRange;
	}
}
