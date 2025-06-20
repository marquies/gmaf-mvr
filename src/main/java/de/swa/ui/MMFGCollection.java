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

import de.swa.gc.*;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.gc.processing.DefaultCollectionProcessor;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.gmaf.GMAF;
import de.swa.mmfg.GeneralMetadata;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.builder.FeatureVectorBuilder;
import de.swa.mmfg.builder.XMLEncodeDecode;

/**
 * Created by Patrick Steinert on 27.12.23.
 */
public class MMFGCollection {
    public static boolean isQuery = false;
    private static MMFGCollection instance;
    private static Hashtable<String, MMFGCollection> sessions = new Hashtable<String, MMFGCollection>();
    private boolean instanceInited = false;

    private Vector<MMFG> collection = new Vector<MMFG>();
    private Hashtable<File, MMFG> fileMap = new Hashtable<File, MMFG>();
    private String name = "";
    private Model model = ModelFactory.createDefaultModel();
    private Hashtable<MMFG, GraphCode> graphCodeCache = new Hashtable<MMFG, GraphCode>();
    private Hashtable<UUID, MMFG> idMap = new Hashtable<UUID, MMFG>();
    private Vector<ProgressListener> progressListeners = new Vector<ProgressListener>();
    private Vector<RefreshListener> refreshListeners = new Vector<RefreshListener>();
    private GraphCode currentQuery;
    protected GraphCodeStrategy graphCodeStrategy;

    protected MMFGCollection() {
    }

    /**
     * Sets the GraphCodeStrategy to be used by this collection.
     * This allows for customization of graph code generation, loading, and saving behavior.
     * @param strategy The strategy implementation to use
     */
    public void setGraphCodeStrategy(GraphCodeStrategy strategy) {
        this.graphCodeStrategy = strategy;
    }

    /**
     * Gets the current GraphCodeStrategy used by this collection.
     * @return The current GraphCodeStrategy
     */
    public GraphCodeStrategy getGraphCodeStrategy() {
        return this.graphCodeStrategy;
    }

    public static synchronized MMFGCollection getInstance() {
        if (instance == null) {
            instance = new MMFGCollection();
        }
        if (!instance.instanceInited) instance.init();
        return instance;
    }

    public static synchronized MMFGCollection getInstance(String session_id) {
        if (sessions.get(session_id) != null) {
            return sessions.get(session_id);
            //if (!sessionInstance.instanceInited) sessionInstance.init();
            //return sessionInstance;
        } else {
            //MMFGCollection newInstance = new MMFGCollection();
            //newInstance.init();
            sessions.put(session_id, instance);
            return instance;
        }
    }

    public void addProgressListener(ProgressListener pl) {
        progressListeners.add(pl);
    }

    public void addRefreshListener(RefreshListener re) {
        refreshListeners.add(re);
    }

    public void refresh() {
        for (RefreshListener re : refreshListeners) re.refresh();
    }

    public synchronized void init() {
        if (instanceInited)
            return;

        // Initialize the GraphCodeStrategy
        try {
            // First try to get the strategy class from Configuration
            String strategyClassName = Configuration.getInstance().getGraphCodeStrategyClass();
            if (strategyClassName != null && !strategyClassName.isEmpty()) {
                try {
                    Class<?> strategyClass = Class.forName(strategyClassName);
                    this.graphCodeStrategy = (GraphCodeStrategy) strategyClass.getDeclaredConstructor().newInstance();
                    System.out.println("Using GraphCodeStrategy from configuration: " + strategyClassName);
                } catch (Exception e) {
                    System.err.println("Error initializing GraphCodeStrategy from configuration: " + e.getMessage());
                    this.graphCodeStrategy = new DefaultGraphCodeStrategy();
                }
            } else {
                // Fall back to default if not specified in configuration
                this.graphCodeStrategy = new DefaultGraphCodeStrategy();
            }
        } catch (Exception e) {
            // Ensure we always have a strategy
            this.graphCodeStrategy = new DefaultGraphCodeStrategy();
        }

        GMAF gmaf = new GMAF();
        name = Configuration.getInstance().getCollectionName();

        try {
            File rdfFolder = new File(Configuration.getInstance().getRDFRepo() + File.separatorChar);
            File[] rdfFiles = rdfFolder.listFiles();
            if (rdfFiles != null) {
                for (File rdf : rdfFiles) {
                    if (rdf.isFile()) {
                        InputStream in = RDFDataMgr.open(rdf.getPath());
                        if (in != null) {
                            model.read(in, null);
                            in.close();
                        }
                    }
                }
            }

            Vector<String> paths = Configuration.getInstance().getCollectionPaths();
            if (paths == null)
                return;
            Vector<String> fileExtensions = Configuration.getInstance().getFileExtensions();
            for (String path : paths) {
                File f = new File(path);
                if (!f.exists()) {
                    System.out.println("path " + path + " does not exist");
                    continue;
                }
                try {
                    File[] fs = f.listFiles();
                    if (fs != null) {
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
                                String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
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
                                        fsx.close();
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
                    }
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
            loadGraphCodes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        instanceInited = true;
    }

    public MMFG loadFromMMFGFile(File existingMMFG) {
        try {
            List<String> lines = Files.readAllLines(existingMMFG.toPath());
            String content = String.join("\n", lines);
            return FeatureVectorBuilder.unflatten(content, new XMLEncodeDecode());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new MMFG();
    }

    public void addToCollection(MMFG m) {
        collection.add(m);
        if (m.getGeneralMetadata() != null && m.getGeneralMetadata().getFileReference() != null) {
            fileMap.put(m.getGeneralMetadata().getFileReference(), m);
        }
        if (m.getGeneralMetadata() != null) {
            idMap.put(m.getGeneralMetadata().getId(), m);
        }
    }

    public Vector<MMFG> getCollection() {
        return collection;
    }

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

    public String getName() {
        return name;
    }

	/**
	 * returns the MMFG for a given file
	 **/
	public MMFG getMMFGForFile(File f) {
		if (f == null) return new MMFG();
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

    public void setQuery(MMFG mmfg_query) {
        MMFGCollection.isQuery = true;
        if (this.graphCodeStrategy == null) {
            this.graphCodeStrategy = new DefaultGraphCodeStrategy();
        }
        currentQuery = this.graphCodeStrategy.generateGraphCode(mmfg_query);
        MMFGCollection.isQuery = false;
    }

    private void loadGraphCodes() {
        if (this.graphCodeStrategy == null) this.graphCodeStrategy = new DefaultGraphCodeStrategy();
        String gcPath = Configuration.getInstance().getGraphCodeRepository();
        File dir = new File(gcPath);
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }

        File[] gcFiles = dir.listFiles((d, name) -> name.endsWith(this.graphCodeStrategy.getGraphCodeFileExtension()));
        if (gcFiles == null) return;

        for (File f_gc : gcFiles) {
            GraphCode gc = this.graphCodeStrategy.loadGraphCode(f_gc);
            if (gc != null) {
                String gcFilename = f_gc.getName();
                String mmfgBaseFilename = gcFilename.substring(0, gcFilename.lastIndexOf(this.graphCodeStrategy.getGraphCodeFileExtension()));

                MMFG targetMMFG = findMMFGByBaseFilename(mmfgBaseFilename);
                if (targetMMFG != null) {
                    graphCodeCache.put(targetMMFG, gc);
                } else {
                    System.err.println("Could not map loaded GraphCode " + f_gc.getName() + " to an existing MMFG.");
                }
            }
        }
    }

    private MMFG findMMFGByBaseFilename(String baseFilename) {
        for (MMFG m : collection) {
            if (m.getGeneralMetadata() != null && m.getGeneralMetadata().getFileReference() != null) {
                String originalFileName = m.getGeneralMetadata().getFileReference().getName();
                if (baseFilename.equals(originalFileName)) {
                    return m;
                }
                // Check for MvMMFGCollection pattern: originalName_cameraModel
                if (m.getGeneralMetadata().getCameraModel() != null) {
                    String mvStyleName = originalFileName + "_" + m.getGeneralMetadata().getCameraModel();
                    if (baseFilename.equals(mvStyleName)) {
                        return m;
                    }
                }
            }
        }
        return null;
    }

    public void recalculateGraphCodes() {
        if (this.graphCodeStrategy == null) this.graphCodeStrategy = new DefaultGraphCodeStrategy();
        String gcPath = Configuration.getInstance().getGraphCodeRepository();
        File dir = new File(gcPath);
        if (!dir.exists()) {
            dir.mkdirs();
        } else {
            File[] gcFiles = dir.listFiles((d, name) -> name.endsWith(this.graphCodeStrategy.getGraphCodeFileExtension()));
            if (gcFiles != null) {
                for (File f_gc : gcFiles) {
                    f_gc.delete();
                }
            }
        }
        graphCodeCache.clear();

        for (MMFG m : collection) {
            getOrGenerateGraphCode(m);
        }
    }

    public Vector<GraphCode> getCollectionGraphCodes() {
        Vector<GraphCode> v = new Vector<GraphCode>();
        for (MMFG m : collection) {
            GraphCode gc = this.getOrGenerateGraphCode(m);
            v.add(gc);
        }
        return v;
    }

    public GraphCode getOrGenerateGraphCode(MMFG mmfg) {
        if (mmfg == null) return new GraphCode();
        if (graphCodeCache.containsKey(mmfg)) {
            return graphCodeCache.get(mmfg);
        }
        if (this.graphCodeStrategy == null) this.graphCodeStrategy = new DefaultGraphCodeStrategy();

        try {
            String baseFileName = null;
            if (mmfg.getGeneralMetadata() != null && mmfg.getGeneralMetadata().getFileReference() != null && mmfg.getGeneralMetadata().getFileReference().getName() != null) {
                baseFileName = mmfg.getGeneralMetadata().getFileReference().getName();
            }

            if (this instanceof MvMMFGCollection && mmfg.getGeneralMetadata() != null && mmfg.getGeneralMetadata().getCameraModel() != null) {
                // MvMMFGCollection might append camera model for subgraphs. This specific logic might be better in MvMMFGCollection's override.
                // For now, if it's an MvMMFGCollection and has a camera model, adjust baseFileName for persistence.
                String originalFileName = mmfg.getGeneralMetadata().getFileReference().getName();
                baseFileName = originalFileName + "_" + mmfg.getGeneralMetadata().getCameraModel();
            }

            if (baseFileName == null) {
                GraphCode gc = this.graphCodeStrategy.generateGraphCode(mmfg);
                if (gc != null) graphCodeCache.put(mmfg, gc);
                return gc != null ? gc : new GraphCode();
            }

            String gcPath = Configuration.getInstance().getGraphCodeRepository();
            File f_gc = new File(gcPath + File.separatorChar + baseFileName + this.graphCodeStrategy.getGraphCodeFileExtension());

            if (f_gc.exists()) {
                GraphCode gc = this.graphCodeStrategy.loadGraphCode(f_gc);
                if (gc != null) {
                    graphCodeCache.put(mmfg, gc);
                    return gc;
                }
            }

            GraphCode gc = this.graphCodeStrategy.generateGraphCode(mmfg);
            if (gc != null) {
                if (gc.getDictionary() != null && !gc.getDictionary().isEmpty()) {
                    this.graphCodeStrategy.saveGraphCode(gc, f_gc);
                }
                graphCodeCache.put(mmfg, gc);
                return gc;
            }
        } catch (Exception x) {
            System.err.println("Error in getOrGenerateGraphCode for MMFG: " + (mmfg.getGeneralMetadata() != null && mmfg.getGeneralMetadata().getFileReference() != null ? mmfg.getGeneralMetadata().getFileReference().getName() : mmfg.getId()) + ". Error: " + x.getMessage());
            GraphCode gc = this.graphCodeStrategy.generateGraphCode(mmfg); // Fallback to transient generation
            if (gc != null) graphCodeCache.put(mmfg, gc);
            return gc != null ? gc : new GraphCode();
        }
        return new GraphCode();
    }

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
            Class<?> c = Class.forName(collectionProcessorClass);
            cp = (CollectionProcessor) c.getDeclaredConstructor().newInstance();

            // If the processor is a DefaultCollectionProcessor, set the GraphCodeStrategy
            if (cp instanceof DefaultCollectionProcessor) {
                ((DefaultCollectionProcessor) cp).setGraphCodeStrategy(this.graphCodeStrategy);
            }
        } catch (Exception ex) {
            System.out.println("Using default collection processor: " + cp.getClass());
            // Ensure the default processor uses our GraphCodeStrategy
            if (cp instanceof DefaultCollectionProcessor) {
                ((DefaultCollectionProcessor) cp).setGraphCodeStrategy(this.graphCodeStrategy);
            }
        }
        cp.setOperation(type);

        Vector<GraphCodeMeta> v = new Vector<GraphCodeMeta>();
        Hashtable<String, MMFG> queryResultCache = new Hashtable<String, MMFG>();

        for (MMFG m : collection) {
            GraphCode gc = getOrGenerateGraphCode(m);
            if (gc != null && gc.getDictionary() != null && !gc.getDictionary().isEmpty()) {
                String gcMetaName = (m.getGeneralMetadata() != null && m.getGeneralMetadata().getFileReference() != null) ?
                        m.getGeneralMetadata().getFileReference().getName() :
                        m.getId().toString();

                if (this instanceof MvMMFGCollection && m.getGeneralMetadata() != null && m.getGeneralMetadata().getCameraModel() != null) {
                    gcMetaName = m.getGeneralMetadata().getFileName() + "_" + m.getGeneralMetadata().getCameraModel();
                }

                GraphCodeMeta gcm = new GraphCodeMeta(gcMetaName + this.graphCodeStrategy.getGraphCodeFileExtension(), gc);
                queryResultCache.put(gcm.getFileName(), m);
                v.add(gcm);
            } else {
                System.err.println("Skipping MMFG in query (null/empty GC): " + (m.getGeneralMetadata() != null ? m.getGeneralMetadata().getFileName() : m.getId()));
            }
        }

        cp.setQueryObject(gcQuery);
        cp.preloadIndex(v);
        cp.execute();
        v = cp.getResultList();

        Vector<MMFG> tempCollection = new Vector<MMFG>();
        for (GraphCodeMeta gcm : v) {
            MMFG m = queryResultCache.get(gcm.getFileName());
            if (m != null) {
                m.setTempSimilarity(gcm.getMetric());
                tempCollection.add(m);
            } else {
                System.err.println("Could not find MMFG for GraphCodeMeta: " + gcm.getFileName() + " in query cache.");
            }
        }
        return tempCollection;
    }

    public Vector<MMFG> getRecommendedAssets(GraphCode gcQuery) {
        return processQuery(gcQuery, CollectionProcessor.RECOMMENDATION);
    }

    public void query(GraphCode gcQuery) {
        currentQuery = gcQuery;
        Vector<MMFG> results = processQuery(gcQuery, CollectionProcessor.SIMILARITY);
        for (RefreshListener re : refreshListeners) re.refresh();
    }

    public Model getRDFModel() {
        return model;
    }
}
