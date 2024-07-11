package de.swa.mmfg;

import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

/** data type to represent the MMFG **/
public class MMFG {
	private Vector<Node> nodes = new Vector<Node>();
	private Vector<Timerange> timeranges = new Vector<Timerange>();
	private GeneralMetadata generalMetadata;
	private Security security;
	private Vector<Location> locations = new Vector<Location>();
	private Node currentNode;
	private transient float[] similarity = new float[] {0f, 0f, 0f};
	private transient float[] tempSimilarity = new float[] {0f, 0f, 0f};
	public Vector<Node> allNodes = new Vector<Node>();
	private Vector<MMFG> collectionElements = new Vector<MMFG>();
	
	/** constructor **/
	public MMFG () {
		GeneralMetadata gm = new GeneralMetadata();
		gm.setId(UUID.randomUUID());
		setGeneralMetadata(gm);
	}
	
	/** constructor, if a given MMFG should be copied **/
	public MMFG (MMFG m)
	{
		this.nodes = m.getNodes();
		this.generalMetadata = m.getGeneralMetadata();
		this.security = m.getSecurity();
		this.locations = m.getLocations();
		this.currentNode = m.getCurrentNode();
		this.similarity = m.getSimilarity();
		this.tempSimilarity = m.getTempSimilarity();
		GeneralMetadata gm = new GeneralMetadata();
		gm.setId(UUID.randomUUID());
		setGeneralMetadata(gm);
	}

	/** returns all nodes with a given term **/
	public Vector<Node> getNodesByTerm(String term) {
		Vector<Node> result = new Vector<Node>();
		for (Node n : nodes) {
			if (n.getName().equals(term)) {
				result.add(n);
				//Vector<TechnicalAttribute> ta = n.getTechnicalAttributes();
				//for (TechnicalAttribute t : ta) {
				//	System.out.println("TA: " + t.getWidth());
				//}
			}
			for (Node ni : n.getChildNodes()) {
				if (ni.getName().equals(term) && !result.contains(ni)) result.add(ni);
			}
		}
		return result;
	}
	
	/** returns all time ranges where the MMFG has features from **/
	public void addTimeRange(Timerange ta) {
		timeranges.add(ta);
	}
	
	/** returns the smallest time range of a feature **/
	public Timerange getBegin() {
		Date d1 = new Date(0);
		
		Timerange tx = new Timerange(d1, d1);
		for (Timerange ti : timeranges) {
			if (ti.isSmaller(tx)) tx = ti;
		}
		return tx;
	}
	
	/** returns the greates time range of a feature **/
	public Timerange getEnd() {
		Date d1 = new Date();
		Timerange tx = new Timerange(d1, d1);
		for (Timerange ti : timeranges) {
			if (ti.isGreater(tx)) tx = ti;
		}
		return tx;
	}
	
	/** returns a new or existing time range object at a specific date **/
	public Timerange getOrCreateTimerangeAt(Date d) {
		for (Timerange ti : timeranges) {
			if (ti.getBegin().before(d) && ti.getEnd().after(d)) return ti;
		}
		Timerange tx = new Timerange(d, d);
		return tx;
	}
	
	/** returns the UUID of the MMFG **/
	public UUID getId() {
		return getGeneralMetadata().getId();
	}
	
	/** adds a node **/

	public void addNode(Node n) {
		nodes.add(n);
		allNodes.add(n);
	}

	/** returns a list of all nodes **/
	public Vector<Node> getNodes() {
		return nodes;
	}

	/** returns the complete list of nodes **/
	public Vector<Node> getAllNodes() {
		return allNodes;
	}
	
	/** adds a sub-mmfg to the mmfg **/
	public void addToCollection(MMFG m) {
		collectionElements.add(m);
	}
	
	/** returns the sub-mmfgs **/
	public Vector<MMFG> getCollectionElements() {
		return collectionElements;
	}

	/** add a location of the represented multimedia object **/
	public void addLocation(Location loc) {
		for (Location l : locations) {
			if (l.getName().equals(loc.getName()))
				return;
		}
		locations.add(loc);
	}

	/** returns the location of the represented multimedia object **/
	public Vector<Location> getLocations() {
		return locations;
	}

	/** returns the general metadata object **/
	public GeneralMetadata getGeneralMetadata() {
		return generalMetadata;
	}

	/*+ sets the general metadata **/
	public void setGeneralMetadata(GeneralMetadata generalMetadata) {
		this.generalMetadata = generalMetadata;
	}

	/** returns the security information object **/
	public Security getSecurity() {
		return security;
	}

	/** sets the security information object **/
	public void setSecurity(Security security) {
		this.security = security;
	}

	/** returns the current node, which is processed **/
	public Node getCurrentNode() {
		return currentNode;
	}

	/** sets the current node for processing **/
	public void setCurrentNode(Node currentNode) {
		this.currentNode = currentNode;
	}

	public float[] getSimilarity() {
		return similarity;
	}

	public void setSimilarity(float[] similarity) {
		this.similarity = similarity;
	}

	public float[] getTempSimilarity() {
		return tempSimilarity;
	}

	public void setTempSimilarity(float[] similarity) {
		this.tempSimilarity = similarity;
	}
	
}
