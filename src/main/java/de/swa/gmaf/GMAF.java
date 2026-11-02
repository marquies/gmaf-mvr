package de.swa.gmaf;

import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Vector;

import de.swa.mmfg.Location;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** main Framework class of the GMAF **/
public class GMAF {
	private static final Logger log = LoggerFactory.getLogger(GMAF.class);

	/** process a Multimedia file **/
	public MMFG processAsset(File f) throws Exception {
		FileInputStream fs = new FileInputStream(f);
		byte[] bytes = fs.readAllBytes();
		MMFG fv = processAsset(bytes, f.getName(), "sw", 5, 50, f.getName(), f);
		return fv;
	}
	
	/** process an array of bytes **/
	public MMFG processAsset(byte[] bytes, String assetName, String userId, int maxRecursions, int maxNodes, String original_file, File original) {
		if (filterChain == null) filterChain = new PluginChain(getProcessingPlugins());
		String suffix = ".jpg";
		if (assetName.indexOf(".") > 0) suffix = assetName.substring(assetName.lastIndexOf("."), assetName.length());

		URL location = null;
//		URL location = TempURLProvider.provideTempUrl(bytes, suffix);
		File file = TempFileProvider.provideTempFile(bytes, suffix, original_file);
		if (original != null && original.exists()) file = original;
		
		MMFG fv = new MMFG();
		Node n = new Node("Root-Asset", fv);
		fv.addNode(n);
		fv.setCurrentNode(n);
		Location loc = new Location(Location.TYPE_ORIGINAL, null, assetName);
		fv.addLocation(loc);
		
		filterChain.process(location, file, bytes, fv, maxRecursions, maxNodes, suffix);
		Security sec = new Security();
		sec.setAcl("private");
		sec.setOwner_id(userId);
		sec.setGroup_id("");
		return fv;
	}
	
	private static PluginChain filterChain = null;
	private Vector<String> processingPlugins = new Vector<String>();
	private Vector<String> getProcessingPlugins() {
		if (processingPlugins.size() == 0) processingPlugins = getPlugins();
		return processingPlugins;
	}
	
	/** set the processing plugins **/
	public void setProcessingPlugins(Vector<String> p) {
		processingPlugins = p;
	}

	/** returns the configured plugins in GMAF-conf **/
	public Vector<String> getPlugins() {
		Vector<String> plugins = new Vector<String>();
		// Plugin for ProcessingFlows is always available
		plugins.add("de.swa.gmaf.plugin.ProcessFlowPlugin");
		try {
			String pluginFile = "conf" + File.separatorChar + "plugin.config";
			RandomAccessFile rf = new RandomAccessFile(pluginFile, "r");
			String line = "";
			while ((line = rf.readLine()) != null) {
				if (line.equals("")) continue;
				if (line.startsWith("#")) continue;
				try {
					Class.forName(line);
					plugins.add(line);
				}
				catch (Exception x) {
					log.warn("Class not found {}", line);
				}
			}
			rf.close();
		}
		catch (Exception x) {}
		return plugins;
	}
}
