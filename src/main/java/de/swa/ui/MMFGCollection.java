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

import de.swa.gc.GraphCodeGenerator;
import de.swa.gc.GraphCodeIO;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.gc.processing.DefaultCollectionProcessor;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.gmaf.GMAF;
import de.swa.mmfg.GeneralMetadata;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import de.swa.gc.GraphCode;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.builder.FeatureVectorBuilder;
import de.swa.mmfg.builder.XMLEncodeDecode;
import org.apache.jena.rdf.model.Model;


/**
 * Created by Patrick Steinert on 27.12.23.
 */
public class MMFGCollection {
	public static boolean isQuery = false;
	/**
	 * singleton pattern access
	 **/
	private static MMFGCollection instance;
	/**
	 * session facade pattern access
	 **/
	private static Hashtable<String, MMFGCollection> sessions = new Hashtable<String, MMFGCollection>();
	private static boolean inited = false;
	private Vector<MMFG> collection = new Vector<MMFG>();
	private Hashtable<File, MMFG> fileMap = new Hashtable<File, MMFG>();
	private String name = "";
	private Model model = ModelFactory.createDefaultModel();
	private Hashtable<MMFG, GraphCode> graphCodeCache = new Hashtable<MMFG, GraphCode>();
	private Hashtable<UUID, MMFG> idMap = new Hashtable<UUID, MMFG>();
	private Vector<ProgressListener> progressListeners = new Vector<ProgressListener>();
	private Vector<RefreshListener> refreshListeners = new Vector<RefreshListener>();
	private GraphCode currentQuery;

	/**
	 * singleton pattern access
	 **/
/*	protected static synchronized MMFGCollection getInstance() {
		if (instance == null) {
			instance = new MMFGCollection();
			instance.init();
		}
		//instance.init();
		return instance;
	}
  */

	protected MMFGCollection() {
	}

	/**
	 * singleton pattern access
	 **/
	public static synchronized MMFGCollection getInstance() {
		if (instance == null) {
			instance = new MMFGCollection();
			instance.init();
		}
		//instance.init();
		return instance;
	}

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

	public void addProgressListener(ProgressListener pl) {
		progressListeners.add(pl);
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

	public void addRefreshListener(RefreshListener re) {
		refreshListeners.add(re);
	}

	public void refresh() {
		for (RefreshListener re : refreshListeners) re.refresh();
	}

	/**
	 * initializes the collection and the configuration
	 **/
	public synchronized void init() {
		if (inited)
			return;
		inited = true;
		GMAF gmaf = new GMAF();
		name = Configuration.getInstance().getCollectionName();

		try {
			File rdfFolder = new File(Configuration.getInstance().getRDFRepo() + File.separatorChar);
			File[] rdfFiles = rdfFolder.listFiles();
			for (File rdf : rdfFiles) {
				if (rdf.isFile()) {
					InputStream in = RDFDataMgr.open(rdf.getPath());
					model.read(in, null);
				}
			}

			Vector<String> paths = Configuration.getInstance().getCollectionPaths();
			if (paths == null)
				return;
			Vector<String> fileExtensions = Configuration.getInstance().getFileExtensions();
			for (String path : paths) {
				File f = new File(path);
				try {
					File[] fs = f.listFiles();
					int count = 0;
					for (File fi : fs) {
						count++;
						int progress = 100 * count / fs.length;
						String txt = "loading " + path + " > " + fi.getName() + " (" + count + " / " + fs.length + ")";
						System.out.println(progress + " -> " + txt);
						for (ProgressListener pl : progressListeners) {
							pl.log(progress, txt);
						}

						try {
							String fileName = fi.getName();
							String ext = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
							if (fileExtensions.contains(ext)) {
								if (fi.getName().endsWith(".wapo")) {
									FileInputStream fsx = new FileInputStream(fi);
									byte[] bytes = fsx.readAllBytes();
									MMFG mmfg = gmaf.processAsset(bytes, fi.getName(), "system",
											Configuration.getInstance().getMaxRecursions(),
											Configuration.getInstance().getMaxNodes(), f.getName(), f);
									GeneralMetadata gm = new GeneralMetadata();
									gm.setFileName(fileName);
									gm.setFileReference(fi);
									mmfg.setGeneralMetadata(gm);
									addToCollection(mmfg);
								} else {
									MMFG mmfg = new MMFG();
									GeneralMetadata gm = new GeneralMetadata();
									gm.setFileName(fileName);
									gm.setFileReference(fi);
									mmfg.setGeneralMetadata(gm);

									File existingMMFG = new File(Configuration.getInstance().getMMFGRepo()
											+ File.separatorChar + fileName + ".mmfg");
									if (existingMMFG.exists()) {
										mmfg = loadFromMMFGFile(existingMMFG);
										if (mmfg.getGeneralMetadata() == null)
											mmfg.setGeneralMetadata(gm);
										if (mmfg.getGeneralMetadata().getFileReference() == null)
											mmfg.getGeneralMetadata().setFileReference(fi);
									}
									addToCollection(mmfg);
								}
								if (Configuration.getInstance().getSelectedAsset() == null)
									Configuration.getInstance().setSelectedAsset(fi);
							}
						} catch (Exception x) {
							x.printStackTrace();
						}
					}
				} catch (Exception x) {
					x.printStackTrace();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		inited = true;
	}

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

	/**
	 * adds a MMFG to the collection
	 **/
	public void addToCollection(MMFG m) {
		collection.add(m);
		fileMap.put(m.getGeneralMetadata().getFileReference(), m);
		idMap.put(m.getGeneralMetadata().getId(), m);
	}

	/**
	 * returns the collection of MMFGs
	 **/
	public Vector<MMFG> getCollection() {
		return collection;
	}

	/**
	 * replaces an element of the collection
	 **/
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

	/**
	 * returns the collection name
	 **/
	public String getName() {
		return name;
	}

	/**
	 * returns the MMFG for a given file
	 **/
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

	public GraphCode getCurrentQuery() {
		return currentQuery;
	}

	/**
	 * returns a vector of all collection's Graph Codes
	 **/
	public Vector<GraphCode> getCollectionGraphCodes() {
		Vector<GraphCode> v = new Vector<GraphCode>();
		for (MMFG m : collection) {
			GraphCode gc = MMFGCollection.getInstance().getOrGenerateGraphCode(m);
			v.add(gc);
		}
		return v;
	}

	/** returns or generates a Graph Code for a given MMFG **/
	public GraphCode getOrGenerateGraphCode(MMFG mmfg) {
		if (graphCodeCache.containsKey(mmfg))
			return graphCodeCache.get(mmfg);
		GraphCode gc = GraphCodeGenerator.generate(mmfg);
		graphCodeCache.put(mmfg, gc);
		return gc;
	}

	/** returns similar assets based on a Graph Code query **/
	public Vector<MMFG> getSimilarAssets(GraphCode gcQuery) {
		return processQuery(gcQuery, CollectionProcessor.SIMILARITY);
	}

	public int getIndexForAsset(MMFG m) {
		return collection.indexOf(m);
	}

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

		Vector<GraphCodeMeta> v = new Vector<GraphCodeMeta>();
		Hashtable<String, MMFG> queryCache = new Hashtable<String, MMFG>();

		for (MMFG m : collection) {
			GraphCode gc = null;
			File f = new File(Configuration.getInstance().getGraphCodeRepository() + File.separatorChar
					+ m.getGeneralMetadata().getFileName() + ".gc");
			if (graphCodeCache.containsKey(m))
				gc = graphCodeCache.get(m);
			else if (f.exists()) {
				gc = GraphCodeIO.read(f);
				graphCodeCache.put(m, gc);
			}
			if (gc == null) {
				gc = GraphCodeGenerator.generate(m);
				if (gc.getDictionary().size() > 1) {
					GraphCodeIO.write(gc, f);
					graphCodeCache.put(m, gc);
				}
			}

			String gcName = m.getGeneralMetadata().getFileName() + ".gc";
			GraphCodeMeta gcm = new GraphCodeMeta(gcName, gc);
			queryCache.put(gcName, m);
			v.add(gcm);
		}

		cp.setQueryObject(gcQuery);
		cp.preloadIndex(v);
		cp.execute();
		v = cp.getResultList();

		Vector<MMFG> tempCollection = new Vector<MMFG>();
		for (GraphCodeMeta gcm : v) {
			MMFG m = queryCache.get(gcm.getFileName());
			m.setTempSimilarity(gcm.getMetric());
			tempCollection.add(m);
		}
		collection = tempCollection;
		return tempCollection;
	}

	/**
	 * returns recommended assets based on a Graph Code query
	 **/
	public Vector<MMFG> getRecommendedAssets(GraphCode gcQuery) {
		return processQuery(gcQuery, CollectionProcessor.RECOMMENDATION);
	}
	
	/**
	 * executes a query
	 **/
	public void query(GraphCode gcQuery) {
		currentQuery = gcQuery;

		processQuery(gcQuery, CollectionProcessor.SIMILARITY);
		for (RefreshListener re : refreshListeners) re.refresh();
	}

	public Model getRDFModel() {
		return model;
	}
}
