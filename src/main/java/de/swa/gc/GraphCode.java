package de.swa.gc;

import java.io.Serializable;
import java.util.Vector;

import com.google.gson.Gson;

/** Implementation of a Graph Code as a 2D representation of MMIR features 
 * 
 * @author stefan_wagenpfeil
 */

public class GraphCode implements Serializable {
	private static final long serialVersionUID = 1L;
	protected Vector<String> dictionary = new Vector<String>();

//	public void setCollectionElements(Vector<GraphCode> collectionElements) {
//		this.collectionElements = collectionElements;
//	}

	protected Vector<GraphCode> collectionElements = new Vector<GraphCode>();
	
	public GraphCode() {}

	protected int[][] matrix;

	/** the Graph Code Dictionary contains a list of feature vocabulary terms **/
	public Vector<String> getDictionary() {
		Vector<String> dict = new Vector<String>();
		for (String s : dictionary) {
			s = s.toLowerCase();
			if (s.length() == 1) continue;
			if (!dict.contains(s)) dict.add(s.toLowerCase());
		}
		return dict;
	}
	
	public int[][] getRelationships() {
		return matrix;
	}
	
	/** adds a Graph Code to a collection **/
	public void addGraphCode(GraphCode gc) {
		collectionElements.add(gc);
	}
	
	/** returns the elements of a Graph Code collection **/
	public Vector<GraphCode> getCollectionElements() {
		return collectionElements;
	}

	/** sets the dictionary of the Graph Code, i.e. the list of feature vocabulary terms **/
	public void setDictionary(Vector<String> d) {
		dictionary = new Vector<String>();
		for (String s : d) {
			s = s.toLowerCase();
			dictionary.add(s.toLowerCase());
		}
		matrix = new int[d.size()][d.size()];
	}
	
	/** returns, if the Graph Code is part of a collection or a single Graph Code **/
	public boolean isCollection() {
		return collectionElements.size() != 0;
	}

	/** returns the matrix value on position x and y **/
	public int getValue(int x, int y) {
		return matrix[x][y];
	}

	/** sets the matrix value of position x and y **/
	public void setValue(int x, int y, int v) {
		matrix[x][y] = v;
	}
	
	/** returns the matrix value for two feature vocabulary terms **/
	public int getEdgeValueForTerms(String term1, String term2) {
		int translated_x = dictionary.indexOf(term1.toLowerCase());
		int translated_y = dictionary.indexOf(term2.toLowerCase());
		try {
			return matrix[translated_x][translated_y];
		}
		catch (Exception ex) {
//			System.out.println("T1: " + term1 + " (" + translated_x + ")   T2: " + term2 + " (" + translated_y + ")");
			return 0;
		}
	}
	
	/** sets the matrix value for two feature vocabulary terms **/
	public void setValueForTerms(String term1, String term2, int val) {
		int idx_a = dictionary.indexOf(term1.toLowerCase());
		int idx_b = dictionary.indexOf(term2.toLowerCase());
		matrix[idx_a][idx_b] = val;
//		System.out.println(idx_a + " " + idx_b + " (" + term1 + "), (" + term2 + ") -> " + val);
	}
	
	/** returns a JSon representation of this Graph Cocde **/
	public String toString() {
		Gson gson = new Gson();
		String s = gson.toJson(this);
		return s;
	}
}
