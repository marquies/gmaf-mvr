package de.swa.ui;

import de.swa.gc.GraphCode;
import de.swa.gc.DefaultGraphCodeStrategy;
import de.swa.gc.GraphCodeStrategy;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.gc.processing.DefaultCollectionProcessor;
import de.swa.gc.processing.GraphCodeMeta;
import de.swa.gmaf.GMAF;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.GeneralMetadata;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;

import java.io.*;
import java.util.*;

/**
 * Created by Patrick Steinert on 27.12.23.
 */
public class MvMMFGCollection extends MMFGCollection {
    private Vector<MMFG> collection = new Vector<MMFG>();
    private Hashtable<File, MMFG> fileMap = new Hashtable<File, MMFG>();
    private Hashtable<MMFG, GraphCode> graphCodeCache = new Hashtable<MMFG, GraphCode>();
    private GraphCode currentQuery;
    private String name = "";
    private Model model = ModelFactory.createDefaultModel();

    private static MvMMFGCollection instance;

    private boolean inited = false;

    protected MvMMFGCollection() {
        super();
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
                            getRDFModel().read(in, null);
                            in.close();
                        }
                    }
                }
            }

            Vector<String> paths = Configuration.getInstance().getCollectionPaths();
            if (paths == null) {
                this.inited = true;
                return;
            }
            Vector<String> fileExtensions = Configuration.getInstance().getFileExtensions();
            for (String path : paths) {
                File f = new File(path);
                try {
                    File[] fs = f.listFiles();
                    if (fs != null) {
                        int count = 0;
                        for (File fi : fs) {
                            count++;
                            int progress = 100 * count / fs.length;
                            String txt = "loading " + path + " > " + fi.getName() + " (" + count + " / " + fs.length + ")";
                            System.out.println(progress + " -> " + txt);
                            for (ProgressListener pl : this.progressListeners) {
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
            Vector<MMFG> tmpSubgraphs = new Vector<MMFG>();

            Vector<MMFG> currentMMFGs = new Vector<>(this.collection);
            for (MMFG mmfg : currentMMFGs) {
                Vector<Node> nodes = mmfg.getNodes();
                for (Node node : nodes) {
                    if (node.getName() != null && node.getName().contains("shot")) {
                        MMFG subgraph = new MMFG();
                        GeneralMetadata gm = subgraph.getGeneralMetadata();
                        if (mmfg.getGeneralMetadata() != null) {
                            gm.setFileName(mmfg.getGeneralMetadata().getFileName());
                            gm.setFileReference(mmfg.getGeneralMetadata().getFileReference());
                        }
                        gm.setCameraModel("Subgraph-" + node.getName());
                        subgraph.setGeneralMetadata(gm);

                        Node scene = new Node(node.getName(), subgraph);
                        subgraph.addNode(scene);

                        Vector<Node> children = node.getChildNodes();
                        for (Node child : children) {
                            Node node1 = new Node(child.getName(), subgraph);
                            scene.addChildNode(node1);
                            CompositionRelationship cr = new CompositionRelationship(CompositionRelationship.RELATION_PART_OF, scene);
                            node1.addCompositionRelationship(cr);
                        }
                        tmpSubgraphs.add(subgraph);
                    }
                }
            }
            for (MMFG subgraph : tmpSubgraphs) {
                addToCollection(subgraph);
            }
            loadGraphCodes();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.inited = true;
    }

    @Override
    public void addToCollection(MMFG m) {
        if (m == null || m.getGeneralMetadata() == null) return;
        this.collection.add(m);
        if (m.getGeneralMetadata().getFileReference() != null) {
            this.fileMap.put(m.getGeneralMetadata().getFileReference(), m);
        }
        this.idMap.put(m.getGeneralMetadata().getId(), m);
    }

    @Override
    public Vector<MMFG> getCollection() {
        return this.collection;
    }

    @Override
    public void replaceMMFGInCollection(MMFG m, File f) {
        try {
            MMFG old = getMMFGForFile(f);
            if (old != null) {
                this.graphCodeCache.remove(old);
                this.collection.remove(old);
            }
            this.fileMap.remove(f);
            addToCollection(m);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    @Override
    public MMFG getMMFGForFile(File f) {
        return this.fileMap.get(f);
    }

    @Override
    public MMFG getMMFGForId(UUID id) {
        if (this.idMap.containsKey(id)) return this.idMap.get(id);
        for (MMFG m : this.collection) {
            if (m.getId().equals(id)) {
                this.idMap.put(id, m);
                return m;
            }
        }
        return null;
    }

    @Override
    public GraphCode getCurrentQuery() {
        return this.currentQuery;
    }

    @Override
    public void setQuery(MMFG mmfg_query) {
        MMFGCollection.isQuery = true;
//        if (getGraphCodeStrategy() == null) {
//            setGraphCodeStrategy(new DefaultGraphCodeStrategy());
//        }
        this.currentQuery = graphCodeStrategy.generateGraphCode(mmfg_query);
        MMFGCollection.isQuery = false;
    }

    @Override
    public GraphCode getOrGenerateGraphCode(MMFG mmfg) {
        if (mmfg == null) return new GraphCode();
        if (this.graphCodeCache.containsKey(mmfg)) {
            return this.graphCodeCache.get(mmfg);
        }

//        GraphCodeStrategy strategy = getGraphCodeStrategy();
//        if (strategy == null) {
//            strategy = new DefaultGraphCodeStrategy();
//            setGraphCodeStrategy(strategy);
//        }

        try {
            String baseFileName = null;
            GeneralMetadata gm = mmfg.getGeneralMetadata();

            if (gm != null && gm.getFileReference() != null && gm.getFileReference().getName() != null) {
                baseFileName = gm.getFileReference().getName();
                if (gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                    baseFileName = baseFileName + "_" + gm.getCameraModel();
                }
            } else if (gm != null && gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                baseFileName = gm.getCameraModel();
            }

            if (baseFileName == null) {
                GraphCode gc = graphCodeStrategy.generateGraphCode(mmfg);
                if (gc != null) this.graphCodeCache.put(mmfg, gc);
                return gc != null ? gc : new GraphCode();
            }

            String gcPath = Configuration.getInstance().getGraphCodeRepository();
            File f_gc = new File(gcPath + File.separatorChar + baseFileName + graphCodeStrategy.getGraphCodeFileExtension());

            if (f_gc.exists()) {
                GraphCode gc = graphCodeStrategy.loadGraphCode(f_gc);
                if (gc != null) {
                    this.graphCodeCache.put(mmfg, gc);
                    return gc;
                }
            }

            GraphCode gc = graphCodeStrategy.generateGraphCode(mmfg);
            if (gc != null) {
                if (gc.getDictionary() != null && !gc.getDictionary().isEmpty()) {
                    graphCodeStrategy.saveGraphCode(gc, f_gc);
                }
                this.graphCodeCache.put(mmfg, gc);
                return gc;
            }
        } catch (Exception x) {
            System.err.println("Error in MvMMFGCollection.getOrGenerateGraphCode for MMFG: " +
                    (mmfg.getGeneralMetadata() != null && mmfg.getGeneralMetadata().getFileName() != null ?
                            mmfg.getGeneralMetadata().getFileName() : (mmfg.getId() != null ? mmfg.getId().toString() : "Unknown MMFG")) +
                    (mmfg.getGeneralMetadata() != null && mmfg.getGeneralMetadata().getCameraModel() != null ? "_" + mmfg.getGeneralMetadata().getCameraModel() : "") +
                    ". Error: " + x.getMessage());
            GraphCode gc = graphCodeStrategy.generateGraphCode(mmfg);
            if (gc != null) this.graphCodeCache.put(mmfg, gc);
            return gc != null ? gc : new GraphCode();
        }
        return new GraphCode();
    }

    protected void loadGraphCodes() {
//        GraphCodeStrategy strategy = getGraphCodeStrategy();
//        if (strategy == null) {
//            System.err.println("GraphCodeStrategy not initialized in MvMMFGCollection. Cannot load graph codes.");
//            // Optionally, initialize it here as a fallback, though init() should handle it.
//            // strategy = new DefaultGraphCodeStrategy();
//            // setGraphCodeStrategy(strategy);
//            return;
//        }
        System.out.println("MvMMFGCollection: Loading graph codes using " + graphCodeStrategy.getClass().getSimpleName());
        // Iterate MvMMFGCollection's own collection
        for (MMFG mmfg : this.collection) { 
            if (mmfg != null) {
                // This will call the MvMMFGCollection's overridden getOrGenerateGraphCode 
                // due to polymorphism, which is correct.
                getOrGenerateGraphCode(mmfg);
            }
        }
        System.out.println("MvMMFGCollection: Finished loading graph codes. Cache size: " + this.graphCodeCache.size());
    }

    @Override
    public Vector<GraphCode> getCollectionGraphCodes() {
        Vector<GraphCode> v = new Vector<GraphCode>();
        // Iterate MvMMFGCollection's own collection
        for (MMFG m : this.collection) { 
            if (m != null) {
                // This will call the MvMMFGCollection's overridden getOrGenerateGraphCode
                GraphCode gc = getOrGenerateGraphCode(m);
                if (gc != null) { // Ensure GC is not null before adding
                    v.add(gc);
                }
            }
        }
        return v;
    }

    @Override
    public Vector<MMFG> processQuery(GraphCode gcQuery, int type) {

        CollectionProcessor cp = new DefaultCollectionProcessor();
        try {
            String collectionProcessorClass = Configuration.getInstance().getCollectionProcessorClass();
            Class<?> c = Class.forName(collectionProcessorClass);
            cp = (CollectionProcessor) c.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            System.out.println("MvMMFGCollection: No collection processor defined. Using " + cp.getClass() + " instead");
        }
        cp.setOperation(type);

        Vector<GraphCodeMeta> v = new Vector<>();
        Hashtable<String, MMFG> queryCache = new Hashtable<>(); 

        // Iterate MvMMFGCollection's own collection
        for (MMFG m : this.collection) { 
            if (m == null || m.getGeneralMetadata() == null) continue;

            // This will call MvMMFGCollection's overridden getOrGenerateGraphCode
            GraphCode gc = getOrGenerateGraphCode(m); 
            if (gc == null || gc.getDictionary() == null || gc.getDictionary().isEmpty()) {
                continue;
            }

            // Construct baseFileName consistent with MvMMFGCollection.getOrGenerateGraphCode
            String baseFileName = null;
            GeneralMetadata gm = m.getGeneralMetadata();
            if (gm.getFileReference() != null && gm.getFileReference().getName() != null) {
                baseFileName = gm.getFileReference().getName();
                if (gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                    baseFileName += "_" + gm.getCameraModel();
                }
            } else if (gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                baseFileName = gm.getCameraModel();
            }
            
            if (baseFileName == null) {
                 System.err.println("MvMMFGCollection.processQuery: Could not determine baseFileName for MMFG: " + (m.getId() != null ? m.getId().toString() : "(no id)"));
                continue;
            }
            String gcName = baseFileName + graphCodeStrategy.getGraphCodeFileExtension();

            GraphCodeMeta gcm = new GraphCodeMeta(gcName, gc);
            //gcm.setOriginatingMMFG(m);
            queryCache.put(gcName, m); 
            v.add(gcm);
        }

        cp.setQueryObject(gcQuery);
        cp.preloadIndex(v);
        cp.execute();
        Vector<GraphCodeMeta> resultMeta = cp.getResultList();

        Vector<MMFG> tempCollection = new Vector<>();
        for (GraphCodeMeta gcm_result : resultMeta) {
            MMFG m = queryCache.get(gcm_result.getFileName());
            m.setTempSimilarity(gcm_result.getMetric());
            tempCollection.add(m);
        }
        return tempCollection;
    }

    @Override
    public void query(GraphCode gcQuery) {
        this.currentQuery = gcQuery; // Sets this.currentQuery (MvMMFGCollection's field)
        
        // processQuery will call MvMMFGCollection's overridden version, operating on this.collection
        Vector<MMFG> results = processQuery(gcQuery, CollectionProcessor.SIMILARITY);
        
        // Update MvMMFGCollection's own collection with query results
        // This makes the query() method have a side effect on the collection state for UI refresh.
        this.collection = results; 
        
        for (RefreshListener re : this.refreshListeners) { // Use MvMMFGCollection's listeners
            re.refresh();
        }
    }

    @Override
    public void recalculateGraphCodes() {

        System.out.println("MvMMFGCollection: Recalculating all graph codes...");
        this.graphCodeCache.clear(); // Clear MvMMFGCollection's graphCodeCache
        
        File gcRepoDir = new File(Configuration.getInstance().getGraphCodeRepository());
        if (gcRepoDir.exists() && gcRepoDir.isDirectory()) {
            // Be cautious with deleting files. This will delete GCs that might be from MMFGCollection if they share the repo.
            // A more robust solution might involve checking if the GC file corresponds to an MMFG in *this* collection.
            // For now, mirroring parent's behavior but scoped to MvMMFGCollection's understanding.
            for (MMFG mmfg : this.collection) { // Iterate this.collection to find relevant GCs
                 if (mmfg == null || mmfg.getGeneralMetadata() == null) continue;

                String baseFileName = null;
                GeneralMetadata gm = mmfg.getGeneralMetadata();

                if (gm.getFileReference() != null && gm.getFileReference().getName() != null) {
                    baseFileName = gm.getFileReference().getName();
                    if (gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                        baseFileName = baseFileName + "_" + gm.getCameraModel();
                    }
                } else if (gm.getCameraModel() != null && !gm.getCameraModel().isEmpty()) {
                    baseFileName = gm.getCameraModel();
                }

                if (baseFileName != null) {
                    File f_gc = new File(gcRepoDir, baseFileName + graphCodeStrategy.getGraphCodeFileExtension());
                    if (f_gc.exists()) {
                        System.out.println("MvMMFGCollection: Deleting old graph code file: " + f_gc.getName());
                        f_gc.delete();
                    }
                }
            }
        }

        // Iterate MvMMFGCollection's own collection to regenerate GCs
        for (MMFG mmfg : this.collection) { 
            if (mmfg != null) {
                // This will call the MvMMFGCollection's overridden getOrGenerateGraphCode,
                // which will generate and save the new GC.
                getOrGenerateGraphCode(mmfg); 
            }
        }
        System.out.println("MvMMFGCollection: Finished recalculating graph codes. Cache size: " + this.graphCodeCache.size());
    }

//    @Override
//    public Vector<GraphCode> getCollectionGraphCodes() {
//        Vector<GraphCode> v = new Vector<GraphCode>();
//        for (MMFG m : this.collection) {
//            if (m != null) {
//                GraphCode gc = getOrGenerateGraphCode(m);
//                if (gc != null) {
//                    v.add(gc);
//                }
//            }
//        }
//        return v;
//    }

    /** Static factory method for MvMMFGCollection instance **/
    public static synchronized MvMMFGCollection getInstance() {
        if (instance == null) {
            instance = new MvMMFGCollection();
        }
        if (!instance.inited) { 
            instance.init();
        }
        return instance;
    }

    // getSimilarAssets and getRecommendedAssets are inherited from MMFGCollection.
    // They will call the overridden processQuery(gcQuery, type) from this MvMMFGCollection class,
    // which is the desired behavior. They return a new Vector<MMFG> and do not modify this.collection.
}
