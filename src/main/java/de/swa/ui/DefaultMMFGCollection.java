package de.swa.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.UUID;
import java.util.Vector;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeGenerator;
import de.swa.gc.GraphCodeIO;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.gc.processing.DefaultCollectionProcessor;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.gmaf.GMAF;
import de.swa.mmfg.GeneralMetadata;
import de.swa.mmfg.MMFG;
import org.slf4j.LoggerFactory;

/** data structure representing the collection of all MMFGs **/
public class DefaultMMFGCollection extends MMFGCollection {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(DefaultMMFGCollection.class);

	private Vector<MMFG> collection = new Vector<MMFG>();
	private Hashtable<File, MMFG> fileMap = new Hashtable<File, MMFG>();
	private String name = "";
	private Model model = ModelFactory.createDefaultModel();

	private static MMFGCollection instance;

	/** session facade pattern access **/
	private static Hashtable<String, MMFGCollection> sessions = new Hashtable<String, MMFGCollection>();

	private boolean inited = false;

	protected DefaultMMFGCollection() {
	}

	private Hashtable<UUID, MMFG> idMap = new Hashtable<UUID, MMFG>();
	private Vector<ProgressListener> progressListeners = new Vector<ProgressListener>();
	@Override
	public void addProgressListener(ProgressListener pl) { progressListeners.add(pl); }
	
	private Vector<RefreshListener> refreshListeners = new Vector<RefreshListener>();
	@Override
	public void addRefreshListener(RefreshListener re) { refreshListeners.add(re); }
	
	@Override
	public void refresh() {
		for (RefreshListener re : refreshListeners) re.refresh();
	}

	/** initializes the collection and the configuration **/
	@Override
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
				if (!f.exists()) {
					log.warn("Path {} does not exist", path);
					continue;
				}
				try {
					File[] fs = f.listFiles();
					int count = 0;
					for (File fi : fs) {
						count++;
						int progress = 100 * count / fs.length;
						String txt = "loading " + path + " > " + fi.getName() + " (" + count + " / " + fs.length + ")";
						log.info("{} -> {}", progress, txt);
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

	/** adds a MMFG to the collection **/
	@Override
	public void addToCollection(MMFG m) {
		collection.add(m);
		fileMap.put(m.getGeneralMetadata().getFileReference(), m);
		idMap.put(m.getGeneralMetadata().getId(), m);
	}

	/** returns the collection of MMFGs **/
	@Override
	public Vector<MMFG> getCollection() {
		return collection;
	}

	/** replaces an element of the collection **/
	@Override
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
		log.info("MMFG replaced in collection for file {}", f.getAbsolutePath());
	}

	/** returns the collection name **/
	@Override
	public String getName() {
		return name;
	}

	/** returns the MMFG for a given file **/
	@Override
	public MMFG getMMFGForFile(File f) {
		return fileMap.get(f);
	}

	@Override
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

	@Override
	public GraphCode getCurrentQuery() {
		return currentQuery;
	}

	/** returns a vector of all collection's Graph Codes **/
	@Override
	public Vector<GraphCode> getCollectionGraphCodes() {
		Vector<GraphCode> v = new Vector<GraphCode>();
		for (MMFG m : collection) {
			GraphCode gc = getOrGenerateGraphCode(m);
			v.add(gc);
		}
		return v;
	}

	/** returns or generates a Graph Code for a given MMFG **/
	@Override
	public GraphCode getOrGenerateGraphCode(MMFG mmfg) {
		if (graphCodeCache.containsKey(mmfg))
			return graphCodeCache.get(mmfg);
		GraphCode gc = GraphCodeGenerator.generate(mmfg);
		graphCodeCache.put(mmfg, gc);
		return gc;
	}

	/** returns similar assets based on a Graph Code query **/
	@Override
	public Vector<MMFG> getSimilarAssets(GraphCode gcQuery) {
		return processQuery(gcQuery, CollectionProcessor.SIMILARITY);
	}

	@Override
	public int getIndexForAsset(MMFG m) {
		return collection.indexOf(m);
	}

	@Override
	public Vector<MMFG> processQuery(GraphCode gcQuery, int type) {
		CollectionProcessor cp = new DefaultCollectionProcessor();
		try {
			String collectionProcessorClass = Configuration.getInstance().getCollectionProcessorClass();
			Class c = Class.forName(collectionProcessorClass);
			cp = (CollectionProcessor) c.newInstance();
		} catch (Exception ex) {
			log.warn("No collection processor defined.");
			log.warn("Using {} instead", cp.getClass());
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

	public static boolean isQuery = false;

	/** returns recommended assets based on a Graph Code query **/
	@Override
	public Vector<MMFG> getRecommendedAssets(GraphCode gcQuery) {
		return processQuery(gcQuery, CollectionProcessor.RECOMMENDATION);
	}

	private Hashtable<MMFG, GraphCode> graphCodeCache = new Hashtable<MMFG, GraphCode>();

	/** executes a query **/
	@Override
	public void query(GraphCode gcQuery) {
		currentQuery = gcQuery;

		processQuery(gcQuery, CollectionProcessor.SIMILARITY);
		for (RefreshListener re : refreshListeners) re.refresh();
	}

	@Override
	public Model getRDFModel() {
		return model;
	}

}
