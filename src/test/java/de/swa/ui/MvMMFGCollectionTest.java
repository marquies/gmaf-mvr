package de.swa.ui;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeGenerator;
import de.swa.gc.GraphCodeIO;
import de.swa.gc.processing.CollectionProcessor;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.builder.XMLEncodeDecode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

class MvMMFGCollectionTest {
    private static final String[] REDIRECTED_FIELDS = {"collectionPaths", "mmfgRepository", "graphCodeRepo", "selectedAsset"};

    @TempDir
    File tempDir;

    private final Map<String, Object> originalConfiguration = new HashMap<>();
    private File assets;
    private File mmfgs;
    private File graphCodes;
    private MvMMFGCollection collection;

    private static Field configurationField(String name) throws Exception {
        Field field = Configuration.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @BeforeEach
    void redirectConfiguration() throws Exception {
        assets = new File(tempDir, "collection");
        mmfgs = new File(tempDir, "mmfgs");
        graphCodes = new File(tempDir, "graphcodes");
        assets.mkdirs();
        mmfgs.mkdirs();
        graphCodes.mkdirs();

        Configuration configuration = Configuration.getInstance();
        for (String name : REDIRECTED_FIELDS) {
            originalConfiguration.put(name, configurationField(name).get(configuration));
        }
        configurationField("collectionPaths").set(configuration, new Vector<>(Arrays.asList(assets.getPath())));
        configurationField("mmfgRepository").set(configuration, mmfgs.getPath());
        configurationField("graphCodeRepo").set(configuration, graphCodes.getPath());
        // with no selection, init() would select through setSelectedAsset, which starts the global collection
        configurationField("selectedAsset").set(configuration, new File(assets, "selected"));

        collection = new MvMMFGCollection();
    }

    @AfterEach
    void restoreConfiguration() throws Exception {
        Configuration configuration = Configuration.getInstance();
        for (String name : REDIRECTED_FIELDS) {
            configurationField(name).set(configuration, originalConfiguration.get(name));
        }
    }

    private static MMFG mmfg(String fileName, String... terms) {
        MMFG m = new MMFG();
        m.getGeneralMetadata().setFileName(fileName);
        m.getGeneralMetadata().setFileReference(new File(fileName));
        for (String term : terms) m.addNode(new Node(term, m));
        return m;
    }

    private static GraphCode graphCode(String... terms) {
        GraphCode gc = new GraphCode();
        gc.setDictionary(new Vector<>(Arrays.asList(terms)));
        return gc;
    }

    private static Node addShot(MMFG m, String name, String... children) {
        Node shot = new Node(name, m);
        for (String child : children) shot.addChildNode(new Node(child, m));
        m.addNode(shot);
        return shot;
    }

    /** puts an asset and its stored MMFG into the configured folders **/
    private File store(MMFG m) throws Exception {
        String fileName = m.getGeneralMetadata().getFileName();
        File asset = new File(assets, fileName);
        Files.write(asset.toPath(), new byte[0]);
        Files.write(new File(mmfgs, fileName + ".mmfg").toPath(), new XMLEncodeDecode().flatten(m).getBytes("UTF-8"));
        return asset;
    }

    private Vector<MMFG> subgraphs() {
        Vector<MMFG> result = new Vector<>();
        for (MMFG m : collection.getCollection()) {
            String marker = m.getGeneralMetadata().getCameraModel();
            if (marker != null && marker.startsWith("Subgraph-")) result.add(m);
        }
        return result;
    }

    @Test
    void init_addsStoredAssets() throws Exception {
        store(mmfg("clip.mp4", "car"));

        collection.init();

        assertEquals(1, collection.getCollection().size());
        assertEquals("clip.mp4", collection.getCollection().get(0).getGeneralMetadata().getFileName());
    }

    @Test
    void init_ignoresOtherFileExtensions() throws Exception {
        Files.write(new File(assets, "notes.unknown").toPath(), new byte[0]);

        collection.init();

        assertTrue(collection.getCollection().isEmpty());
    }

    @Test
    void init_createsSubgraphForEachShot() throws Exception {
        MMFG clip = mmfg("clip.mp4");
        addShot(clip, "shot_1", "car");
        addShot(clip, "shot_2", "road");
        store(clip);

        collection.init();

        assertEquals(3, collection.getCollection().size());
        assertEquals(2, subgraphs().size());
    }

    @Test
    void init_subgraphDescribesItsShot() throws Exception {
        MMFG clip = mmfg("clip.mp4");
        addShot(clip, "shot_1", "car");
        store(clip);

        collection.init();

        MMFG subgraph = subgraphs().get(0);
        assertEquals("clip.mp4", subgraph.getGeneralMetadata().getFileName());
        assertEquals("Subgraph-shot_1-clip.mp4", subgraph.getGeneralMetadata().getCameraModel());
        assertNotEquals(collection.getCollection().get(0).getId(), subgraph.getId());

        GraphCode gc = GraphCodeGenerator.generate(subgraph);
        assertEquals(1, gc.getEdgeValueForTerms("shot_1", "car"));
        assertEquals(CompositionRelationship.RELATION_PART_OF, gc.getEdgeValueForTerms("car", "shot_1"));
    }

    @Test
    void init_withoutShots_createsNoSubgraphs() throws Exception {
        store(mmfg("clip.mp4", "car", "road"));

        collection.init();

        assertTrue(subgraphs().isEmpty());
    }

    @Test
    void init_onlyRunsOnce() throws Exception {
        store(mmfg("clip.mp4", "car"));

        collection.init();
        collection.init();

        assertEquals(1, collection.getCollection().size());
    }

    @Disabled("Known bug: subgraphs share the asset's file, so addToCollection replaces the asset in fileMap")
    @Test
    void init_keepsFileLookupOnTheFullAsset() throws Exception {
        MMFG clip = mmfg("clip.mp4");
        addShot(clip, "shot_1", "car");
        File asset = store(clip);

        collection.init();

        assertEquals(clip.getId(), collection.getMMFGForFile(asset).getId());
    }

    @Disabled("Known bug: shots are found with contains(\"shot\"), which also matches other terms")
    @Test
    void init_doesNotTreatScreenshotAsShot() throws Exception {
        MMFG clip = mmfg("clip.mp4");
        addShot(clip, "screenshot", "car");
        store(clip);

        collection.init();

        assertTrue(subgraphs().isEmpty());
    }

    @Test
    void addToCollection_makesMmfgAvailableByFileAndId() {
        MMFG m = mmfg("clip.mp4", "car");

        collection.addToCollection(m);

        assertSame(m, collection.getMMFGForFile(new File("clip.mp4")));
        assertSame(m, collection.getMMFGForId(m.getId()));
        assertEquals(0, collection.getIndexForAsset(m));
    }

    @Disabled("Known bug: Hashtable rejects null, while MMFGCollection.getMMFGForFile handles it")
    @Test
    void getMMFGForFile_acceptsNull() {
        assertDoesNotThrow(() -> collection.getMMFGForFile(null));
    }

    @Test
    void replaceMMFGInCollection_replacesExistingEntry() {
        MMFG original = mmfg("clip.mp4", "car");
        MMFG replacement = mmfg("clip.mp4", "road");
        collection.addToCollection(original);
        GraphCode cached = collection.getOrGenerateGraphCode(original);

        collection.replaceMMFGInCollection(replacement, new File("clip.mp4"));

        assertEquals(new Vector<>(Arrays.asList(replacement)), collection.getCollection());
        assertSame(replacement, collection.getMMFGForFile(new File("clip.mp4")));
        assertNotSame(cached, collection.getOrGenerateGraphCode(replacement));
    }

    @Disabled("Known bug: removing a null entry from a Hashtable throws, and the catch skips addToCollection")
    @Test
    void replaceMMFGInCollection_addsUnknownFile() {
        MMFG m = mmfg("new.mp4", "car");

        collection.replaceMMFGInCollection(m, new File("new.mp4"));

        assertTrue(collection.getCollection().contains(m));
    }

    @Disabled("Known bug: idMap keeps the replaced MMFG")
    @Test
    void replaceMMFGInCollection_forgetsOldId() {
        MMFG original = mmfg("clip.mp4", "car");
        collection.addToCollection(original);

        collection.replaceMMFGInCollection(mmfg("clip.mp4", "road"), new File("clip.mp4"));

        assertNull(collection.getMMFGForId(original.getId()));
    }

    @Disabled("Known bug: the .gc file of the replaced MMFG stays on disk and is preferred over regenerating")
    @Test
    void replaceMMFGInCollection_queriesUseTheNewGraphCode() {
        MMFG original = mmfg("clip.mp4", "car", "road");
        MMFG replacement = mmfg("clip.mp4", "sky", "sun");
        GraphCode query = graphCode("sky", "sun");
        collection.addToCollection(original);
        collection.getSimilarAssets(query);

        collection.replaceMMFGInCollection(replacement, new File("clip.mp4"));

        assertEquals(1f, collection.getSimilarAssets(query).get(0).getTempSimilarity()[0], 0.001f);
    }

    @Test
    void getOrGenerateGraphCode_cachesResult() {
        MMFG m = mmfg("clip.mp4", "car");
        assertSame(collection.getOrGenerateGraphCode(m), collection.getOrGenerateGraphCode(m));
    }

    @Test
    void getCollectionGraphCodes_returnsOnePerAsset() {
        collection.addToCollection(mmfg("first.mp4", "car"));
        collection.addToCollection(mmfg("second.mp4", "road"));

        assertEquals(2, collection.getCollectionGraphCodes().size());
    }

    /** matches all query terms, but none of its edges **/
    private MMFG allTermsNoEdges() {
        return mmfg("terms.mp4", "term_a", "term_b", "term_c");
    }

    /** matches two query terms and the query's edge **/
    private MMFG fewerTermsWithEdge() {
        MMFG m = mmfg("edges.mp4");
        addShot(m, "term_a", "term_b");
        return m;
    }

    private GraphCode queryWithEdge() {
        GraphCode query = graphCode("term_a", "term_b", "term_c");
        query.setValueForTerms("term_a", "term_b", 1);
        return query;
    }

    @Test
    void getSimilarAssets_ranksMatchingTermsFirst() {
        MMFG terms = allTermsNoEdges();
        collection.addToCollection(fewerTermsWithEdge());
        collection.addToCollection(terms);

        assertSame(terms, collection.getSimilarAssets(queryWithEdge()).get(0));
    }

    @Test
    void getRecommendedAssets_ranksMatchingEdgesFirst() {
        MMFG edges = fewerTermsWithEdge();
        collection.addToCollection(allTermsNoEdges());
        collection.addToCollection(edges);

        assertSame(edges, collection.getRecommendedAssets(queryWithEdge()).get(0));
    }

    @Test
    void processQuery_storesSimilarityOnResults() {
        MMFG m = mmfg("clip.mp4", "term_a", "term_b");
        collection.addToCollection(m);

        collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY);

        assertEquals(1f, m.getTempSimilarity()[0], 0.001f);
    }

    @Test
    void processQuery_namesGraphCodeFilesAfterFileAndCamera() {
        MMFG m = mmfg("clip.mp4", "term_a", "term_b");
        m.getGeneralMetadata().setCameraModel("camera");
        collection.addToCollection(m);

        collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY);

        assertTrue(new File(graphCodes, "clip.mp4_camera.gc").exists());
    }

    @Test
    void processQuery_prefersStoredGraphCodeFile() {
        MMFG m = mmfg("clip.mp4", "car", "road");
        m.getGeneralMetadata().setCameraModel("camera");
        GraphCodeIO.write(graphCode("term_a", "term_b"), new File(graphCodes, "clip.mp4_camera.gc"));
        collection.addToCollection(m);

        collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY);

        assertEquals(1f, m.getTempSimilarity()[0], 0.001f);
    }

    @Test
    void processQuery_doesNotStoreGraphCodesWithFewerThanTwoTerms() {
        MMFG m = mmfg("clip.mp4", "car");
        collection.addToCollection(m);

        collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY);

        assertEquals(0, graphCodes.list().length);
    }

    @Disabled("Known bug: results are keyed by file name and camera model, so such MMFGs overwrite each other")
    @Test
    void processQuery_keepsAssetsWithTheSameFileAndCamera() {
        MMFG first = mmfg("clip.mp4", "car", "road");
        MMFG second = mmfg("clip.mp4", "sky", "sun");
        first.getGeneralMetadata().setCameraModel("camera");
        second.getGeneralMetadata().setCameraModel("camera");
        collection.addToCollection(first);
        collection.addToCollection(second);

        Vector<MMFG> results = collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY);

        assertEquals(2, new HashSet<>(results).size());
    }

    @Disabled("Known bug: a corrupt .gc file makes GraphCodeIO.read throw, which aborts the whole query")
    @Test
    void processQuery_survivesCorruptGraphCodeFile() throws Exception {
        MMFG m = mmfg("clip.mp4", "term_a", "term_b");
        m.getGeneralMetadata().setCameraModel("camera");
        Files.write(new File(graphCodes, "clip.mp4_camera.gc").toPath(), "{\"dictionary\":[".getBytes());
        collection.addToCollection(m);

        assertDoesNotThrow(() -> collection.processQuery(graphCode("term_a", "term_b"), CollectionProcessor.SIMILARITY));
    }

    @Test
    void query_storesQueryAndNotifiesListeners() {
        int[] refreshes = {0};
        GraphCode query = graphCode("term_a", "term_b");
        collection.addRefreshListener(() -> refreshes[0]++);
        collection.addToCollection(mmfg("clip.mp4", "term_a", "term_b"));

        collection.query(query);

        assertSame(query, collection.getCurrentQuery());
        assertEquals(1, refreshes[0]);
    }

    @Test
    void refresh_notifiesListeners() {
        int[] refreshes = {0};
        collection.addRefreshListener(() -> refreshes[0]++);

        collection.refresh();

        assertEquals(1, refreshes[0]);
    }
}
