package de.swa.mmfg;

import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.UUID;
import java.util.Vector;

/** data type to represent a MMFG node **/
public class Node {
	private String name;
	private Vector<Node> childNodes = new Vector<Node>();
	private Vector<Weight> weights = new Vector<Weight>();
	private Vector<TechnicalAttribute> technicalAttributes = new Vector<TechnicalAttribute>();
	private Vector<SemanticRelationship> semanticRelationships = new Vector<SemanticRelationship>();
	private Vector<CompositionRelationship> compositionRelationships = new Vector<CompositionRelationship>();
	private Vector<AssetLink> assetLinks = new Vector<AssetLink>();
	private String detectedBy;
	private MMFG fv;
	private Timerange timerange;
	
	public Node() {}
	public Node(String name, MMFG fv) {
		setName(name);
		this.fv = fv;
		fv.allNodes.add(this);
	}
	
	public Node(String name, MMFG fv, Timerange tr) {
		this(name, fv);
		fv.addTimeRange(tr);
		fv.allNodes.add(this);
		tr.addNode(this);
		timerange = tr;
	}
	
	public Node(String name, String value, MMFG fv) {
		setName(name);
		this.fv = fv;
		fv.allNodes.add(this);
		Node v = new Node(value, fv);
		addChildNode(v);
	}
	
	public Node(String name, String value, MMFG fv, Timerange tr) {
		this(name, value, fv);
		fv.addTimeRange(tr);
		fv.allNodes.add(this);
		tr.addNode(this);
		timerange = tr;
	}
	
	public void setFeatureVector(MMFG fv) {
		this.fv = fv;
		for (Node ni : childNodes) {
			if (!fv.allNodes.contains(ni)) fv.allNodes.add(ni);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String n) {
		name = n.trim();
		weights.add(new Weight(Context.getDefaultContext(), 0.5f));
	}
	
	public void addChildNode(Node n) {
		childNodes.add(n);
		if (!fv.allNodes.contains(n)) fv.allNodes.add(n);
	}
	
	public Vector<Node> getChildNodes() {
		return childNodes;
	}
	
	public void addWeight(Weight w) {
		weights.add(w);
	}
	
	public Vector<Weight> getWeights() {
		return weights;
	}
	
	public void addTechnicalAttribute(TechnicalAttribute ta) {
		technicalAttributes.add(ta);
	}
	
	public Vector<TechnicalAttribute> getTechnicalAttributes() {
		return technicalAttributes;
	}
	
	public void addSemanticRelationship(SemanticRelationship sr) {
		semanticRelationships.add(sr);
	}
	
	public Vector<SemanticRelationship> getSemanticRelationships() {
		return semanticRelationships;
	}
	
	public void addCompositionRelationship(CompositionRelationship cr) {
		compositionRelationships.add(cr);
	}
	
	public Vector<CompositionRelationship> getCompositionRelationships() {
		return compositionRelationships;
	}
	
	public void addAssetLink(AssetLink al) {
		assetLinks.add(al);
	}
	
	public Vector<AssetLink> getAssetLinks() {
		return assetLinks;
	}
	public String getDetectedBy() {
		return detectedBy;
	}
	public void setDetectedBy(String detectedBy) {
		this.detectedBy = detectedBy;
	}
	public Timerange getTimerange() {
		return timerange;
	}
	public void setTimerange(Timerange timerange) {
		this.timerange = timerange;
		timerange.addNode(this);
	}
}
