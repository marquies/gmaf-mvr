package de.swa.mmfg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.Date;
import java.util.Vector;

public class Timerange {
	private Date begin, end;

	@JsonIgnore
	private Vector<Node> nodes = new Vector<Node>();
	private boolean isUniversalTime = true;
	private boolean isRelativeTime = false;

	public Timerange(Date begin, Date end, boolean isRelativeTime) {
		this.begin = begin;
		this.end = end;
		this.isRelativeTime = isRelativeTime;
	}

	public boolean isUniversalTime() {
		return isUniversalTime;
	}

	public void setUniversalTime(boolean isUniversalTime) {
		this.isUniversalTime = isUniversalTime;
	}

	// TODO: What does it change? Should is greater / is smaller ignore date?
	public boolean isRelativeTime() {
		return isRelativeTime;
	}

	public void setRelativeTime(boolean isRelativeTime) {
		this.isRelativeTime = isRelativeTime;
	}

	public Timerange() {
		
	}

	public Timerange(Date begin, Date end) {
		this.begin = begin;
		this.end = end;
	}
	
	public Date getBegin() {
		return begin;
	}

	/**
	 * Set the beginning date of the timerange.
	 * @param begin
	 */
	public void setBegin(Date begin) {
		this.begin = begin;
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}
	public Vector<Node> getNodes() {
		return nodes;
	}
	public void setNodes(Vector<Node> nodes) {
		this.nodes = nodes;
	}
	public void addNode(Node n) {
		nodes.add(n);
	}

	public boolean isSmaller(Timerange ta) {
		Date otherStart = ta.getBegin();
		if (begin.before(otherStart)) return true;
		return false;
	}
	
	public boolean isGreater(Timerange ta) {
		Date otherEnd = ta.getEnd();
		if (end.after(otherEnd)) return true;
		return false;
	}


	public boolean isInside(Timerange toCompare) {
		if (toCompare == null) return false;
		Date otherStart = toCompare.begin;
		Date otherEnd = toCompare.end;

		if (otherStart == null || otherEnd == null) return false;

		return begin.before(otherStart) && end.after(otherEnd);
	}
}
