package de.swa.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import de.swa.gc.GraphCode;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.builder.FeatureVectorBuilder;
import de.swa.mmfg.builder.XMLEncodeDecode;
import org.apache.jena.rdf.model.Model;

import java.io.File;
import java.nio.file.Files;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

/**
 * Created by Patrick Steinert on 27.12.23.
 */
public abstract class MMFGCollection {
	/**
	 * singleton pattern access
	 **/
	private static MMFGCollection instance;

	/** singleton pattern access **/
/*	protected static synchronized MMFGCollection getInstance() {
		if (instance == null) {
			instance = new MMFGCollection();
			instance.init();
		}
		//instance.init();
		return instance;
	}
  */
	/** session facade pattern access **/
	private static Hashtable<String, MMFGCollection> sessions = new Hashtable<String, MMFGCollection>();

	public static synchronized MMFGCollection getInstance(String session_id) {
		if (sessions.get(session_id) != null)
			return sessions.get(session_id);
		else {
//			MMFGCollection coll = new MMFGCollection();
//			coll.init();
//			sessions.put(session_id, coll);
//			return coll;

			sessions.put(session_id, getInstance());
			return instance;
		}
	}

	private static boolean inited = false;

	protected MMFGCollection() {
	}

//	protected static synchronized MvMMFGCollection getInstance(String session_id) {
//		if (sessions.get(session_id) != null)
//			return sessions.get(session_id);
//		else {
////			MMFGCollection coll = new MMFGCollection();
////			coll.init();
////			sessions.put(session_id, coll);
////			return coll;
//
//			sessions.put(session_id, getInstance());
//			return instance;
//		}
//	}

	public abstract void addProgressListener(ProgressListener pl);

	public abstract void addRefreshListener(RefreshListener re);

	public abstract void refresh();

	public abstract void init();

	public MMFG loadFromMMFGFile(File existingMMFG) {
		try {
			String content = "";
			List<String> lines = Files.readAllLines(existingMMFG.toPath());
			content = String.join("\n", lines);
			MMFG mmfg = FeatureVectorBuilder.unflatten(content, new XMLEncodeDecode());
			return mmfg;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
//		try {
//			RandomAccessFile rf = new RandomAccessFile(existingMMFG, "rw");
//			String line = "";
//			String content = "";
//			while ((line = rf.readLine()) != null) {
//				content += line + "\n";
//			}
//			MMFG mmfg = FeatureVectorBuilder.unflatten(content, new XMLEncodeDecode());
//			rf.close();
//			return mmfg;
//		} catch (Exception ex) {
//			ex.printStackTrace();
//		}
		return new MMFG();
	}

	/** adds a MMFG to the collection **/
	public void addToCollection(MMFG m) {
		collection.add(m);
		fileMap.put(m.getGeneralMetadata().getFileReference(), m);
		idMap.put(m.getGeneralMetadata().getId(), m);
	}

	/** returns the collection of MMFGs **/
	public Vector<MMFG> getCollection() {
		return collection;
	}

	/** replaces an element of the collection **/
	public void replaceMMFGInCollection(MMFG m, File f) {
		try {
			MMFG old = getMMFGForFile(f);
			graphCodeCache.remove(old);
			collection.remove(old);
			fileMap.remove(f);
			addToCollection(m);
		} catch (Exception x) {
			x.printStackTrace();
		}
		System.out.println("MMFG replaced in collection for file " + f.getAbsolutePath());
	}

	/** returns the collection name **/
	public String getName() {
		return name;
	}

	/** returns the MMFG for a given file **/
	public MMFG getMMFGForFile(File f) {
		return fileMap.get(f);
	}

	public MMFG getMMFGForId(UUID id) {
		if (idMap.containsKey(id)) return idMap.get(id);
		for (MMFG m : collection) {
			if (m.getId().equals(id)) {
				idMap.put(id, m);
				return m;
			}
		}
		return null;
	}

	private GraphCode currentQuery;

	public GraphCode getCurrentQuery() {
		return currentQuery;
	}

	/** returns a vector of all collection's Graph Codes **/
	public Vector<GraphCode> getCollectionGraphCodes() {
		Vector<GraphCode> v = new Vector<GraphCode>();
		for (MMFG m : collection) {
			GraphCode gc = MMFGCollection.getInstance().getOrGenerateGraphCode(m);
			v.add(gc);
		}
		return v;
	}

	public abstract Vector<MMFG> getCollection();

	public abstract void replaceMMFGInCollection(MMFG m, File f);

	public abstract String getName();

	public Vector<MMFG> processQuery(GraphCode gcQuery, int type) {
		CollectionProcessor cp = new DefaultCollectionProcessor();
		try {
			String collectionProcessorClass = Configuration.getInstance().getCollectionProcessorClass();
			Class c = Class.forName(collectionProcessorClass);
			cp = (CollectionProcessor) c.newInstance();
		} catch (Exception ex) {
			System.out.println("no collection processor defined.");
			System.out.println("using " + cp.getClass() + " instead");
		}
		cp.setOperation(type);


	public abstract MMFG getMMFGForId(UUID id);

	public abstract GraphCode getCurrentQuery();

	public abstract Vector<GraphCode> getCollectionGraphCodes();

	public abstract GraphCode getOrGenerateGraphCode(MMFG mmfg);

	public abstract Vector<MMFG> getSimilarAssets(GraphCode gcQuery);

	public abstract int getIndexForAsset(MMFG m);

	public abstract Vector<MMFG> processQuery(GraphCode gcQuery, int type);

	public abstract Vector<MMFG> getRecommendedAssets(GraphCode gcQuery);

	public abstract void query(GraphCode gcQuery);

	public abstract Model getRDFModel();
}
