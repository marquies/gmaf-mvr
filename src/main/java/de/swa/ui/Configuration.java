package de.swa.ui;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Vector;

import de.swa.gmaf.api.GMAF_SessionFactory;
import de.swa.mmfg.MMFG;

/** data structure representing the config files of the GMAF **/
public class Configuration {
	private Configuration() {}
	
	private static Configuration instance;
	private Vector<String> collectionPaths;
	private String collectionName;
	private String graphCodeRepo;
	private String exportFolder;
	private String mmfgRepository;
	private String thumbNailFolder;
	private Vector<String> fileExtensions;
	private String uimode;
	private int maxNodes = 50, maxRecursions = 2;
	private String autoProcess;
	private String launchServer;
	private String semanticFactoryClass;
	private String collectionProcessor = "de.swa.gc.processing.DefaultCollectionProcessor";
	private String collectionConfig;
	private String queryInformationUI;
	private String rdfRepository;
	private String flowFolder;
	private int serverPort;
	private int restServicePort;
	private String serverName;
	private String context;
	private boolean showBoundingBox = true;

	private String collectionManager = "de.swa.ui.DefaultMMFGCollection";

	public static synchronized Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
			instance.reload();
			instance.check();
		}
		return instance;
	}

	private Vector<MMFG> selection = new Vector<MMFG>();

	public static void setInstance(Configuration mockConfig) {
		instance = mockConfig;
	}

	public Vector<MMFG> getSelectedItems() {
		return selection;
	}
	
	public void select(MMFG m) {
		selection.add(m);
	}
	
	public void unselect(MMFG m) {
		selection.remove(m);
	}
	
	private void check() {
		File f = new File(graphCodeRepo);
		if (!f.exists()) f.mkdirs();
		f = new File(mmfgRepository);
		if (!f.exists()) f.mkdirs();
		f = new File(rdfRepository);
		if (!f.exists()) f.mkdirs();
		f = new File(thumbNailFolder);
		if (!f.exists()) f.mkdirs();
		f = new File(exportFolder);
		if (!f.exists()) f.mkdirs();
		f = new File(flowFolder);
		if (!f.exists()) f.mkdirs();
		f = new File("temp");
		if (!f.exists()) f.mkdirs();
	}
	
	public void reload() {
		try {
			RandomAccessFile rf = new RandomAccessFile("conf" + File.separatorChar + "gmaf.config", "r");
			String line = "";
			String collectionName = "";
			String graphCodeRepo = "";
			String exportFolder = "";
			String mmfgRepository = "";
			Vector<String> collectionPaths = new Vector<String>();
			Vector<String> fileEx = new Vector<String>();
			String uimode = "";
			String maxNodes = "";
			String maxRecursions = "";
			String thumbNails = "";
			String autoProcess = "";
			String semExt = "";
			String launchServer = "";
			String collectionProc = "";
			String collectionConf = "";
			String queryEx = "";
			String rdfRepo = "";
			String serverPort = "8142";
			String flows = "";
			String srv = "localhost";
			String ctx = "gmaf";
			String password = "";
			String restServicePort = "8242";
			String collectionManager = "de.swa.ui.DefaultMMFGCollection";

			while ((line = rf.readLine()) != null) {
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				try {
					if (line.startsWith("collectionName")) {
						collectionName = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					if (line.startsWith("exportFolder")) {
						exportFolder = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("collectionPath")) {
						String paths = line.substring(line.indexOf("=") + 1, line.length()).trim();
						String[] str = paths.split(",");
						for (String s : str) {
							collectionPaths.add(s.trim());
						}
					}
					else if (line.startsWith("graphCodeRepository")) {
						graphCodeRepo = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("mmfgRepository")) {
						mmfgRepository = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("fileExtensions")) {
						String exs = line.substring(line.indexOf("=") + 1, line.length()).trim();
						String[] str = exs.split(",");
						for (String s : str) {
							fileEx.add(s.trim());
						}
					}
					else if (line.startsWith("uiMode")) {
						uimode = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("maxNodes")) {
						maxNodes = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("maxRecursions")) {
						maxRecursions = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("thumbnailFolder")) {
						thumbNails = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("autoProcess")) {
						autoProcess = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("semanticExtension")) {
						semExt = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("launchServer")) {
						launchServer = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("collectionProcessorConfig")) {
						collectionConf = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("collectionProcessor")) {
						collectionProc = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("queryExplainerClass")) {
						queryEx = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("rdfRepository")) {
						rdfRepo = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("serverPort")) {
						serverPort = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("restServicePort")) {
						restServicePort = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("processingFlowFolder")) {
						flows = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("serverName")) {
						srv = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("serverContext")) {
						ctx = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("api_key")) {
						password = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
					else if (line.startsWith("collectionManager")) {
						collectionManager = line.substring(line.indexOf("=") + 1, line.length()).trim();
					}
 				}
				catch (Exception x) {}
			}
			
			Configuration.getInstance().setConfig(collectionName, collectionPaths, graphCodeRepo, exportFolder, fileEx, mmfgRepository, uimode, maxNodes, maxRecursions, thumbNails, autoProcess, semExt, launchServer, collectionProc, collectionConf, queryEx, rdfRepo, serverPort, flows, srv, ctx, password, restServicePort, collectionManager);
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public void setConfig(String name, Vector<String> paths, String gcRepo, String export, Vector<String> fileEx, String mmfgRepo, String ui, String nodes, String recursions, String thumbNail, String auto, String semFact, String launch, String collectionProc, String collectionConf, String queryUI, String rdf, String serverPort, String flows, String serverName, String context, String password, String restServicePort, String collectionManager) {
		try {
			collectionName = name;
			collectionPaths = paths;
			graphCodeRepo = gcRepo;
			exportFolder = export;
			fileExtensions = fileEx;
			mmfgRepository = mmfgRepo;
			uimode = ui;
			maxNodes = Integer.parseInt(nodes);
			maxRecursions = Integer.parseInt(recursions);
			thumbNailFolder = thumbNail;
			autoProcess = auto;
			semanticFactoryClass = semFact;
			launchServer = launch;
			collectionProcessor = collectionProc;
			collectionConfig = collectionConf;
			queryInformationUI = queryUI;
			rdfRepository = rdf;
			flowFolder = flows;
			this.serverPort = Integer.parseInt(serverPort);
			this.serverName = serverName;
			this.context = context;
			this.restServicePort = Integer.parseInt(restServicePort);
			this.collectionManager = collectionManager;
			GMAF_SessionFactory.API_KEY = password;
		}
		catch (Exception x) {
			x.printStackTrace();
		}
	}
	
	public Vector<String> getCollectionPaths() {
		return collectionPaths;
	}
	
	public boolean showBoundingBox() {
		return showBoundingBox;
	}
	
	public void showBoundingBox(boolean b) {
		showBoundingBox = b;
	}
	
	public String getCollectionName() {
		return collectionName;
	}
	
	public String getGraphCodeRepository() {
		return graphCodeRepo;
	}
	
	public String getExportFolder() {
		return exportFolder;
	}
	
	public Vector<String> getFileExtensions() {
		return fileExtensions;
	}
	
	private File selectedAsset;
	private MMFG selectedMMFG;
	public void setSelectedAsset(File f) {
		selectedAsset = f;
		selection.clear();
		selectedMMFG = MMFGCollectionFactory.createOrGetCollection().getMMFGForFile(f);
		selection.add(selectedMMFG);
	}
	
	public MMFG getSelectedMMFG() {
		return selectedMMFG;
	}
	
	public File getSelectedAsset() {
		return selectedAsset;
	}
	
	public String getMMFGRepo() {
		return mmfgRepository;
	}
	
	public String getUIMode() {
		return uimode;
	}
	
	public int getMaxNodes() {
		return maxNodes;
	}
	
	public int getMaxRecursions() {
		return maxRecursions;
	}
	
	public String getThumbnailPath() {
		return thumbNailFolder;
	}
	
	public String getSemanticExtension() {
		return semanticFactoryClass;
	}
	
	public boolean isAutoProcess() {
		return autoProcess.equals("true");
	}
	
	public boolean launchServer() {
		return launchServer.equals("true");
	}
	
	public String getCollectionProcessorClass() {
		return collectionProcessor;
	}
	
	public String getCollectionProcessorConfigClass() {
		return collectionConfig;
	}
	
	public String getQueryUI() {
		return queryInformationUI;
	}
	
	public String getRDFRepo() {
		return rdfRepository;
	}

	public int getServerPort() {
		return serverPort;
	}
	
	public int getRestServicePort() {
		return restServicePort;
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public String getProcessingFlowFolder() {
		return flowFolder;
	}
	
	public String getContext() {
		return context;
	}

	public String getCollectionManager() {
		return collectionManager;
	}
}
