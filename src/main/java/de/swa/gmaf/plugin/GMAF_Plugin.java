package de.swa.gmaf.plugin;

import java.io.File;
import java.net.URL;
import java.util.Vector;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/** 
 * @author stefan_wagenpfeil 
 * @date 16.02.2021
 * interface describing the GMAF feature vector plugins. All methods will be called by the GMAF framework. To add additional plugins to the framework, see conf/plugin.config.
 */
public interface GMAF_Plugin {
	/** 
	 * this method is called by the GMAF framework, when a new asset has to be processed
	 * @param url represents the temporary URL of the Multimedia asset to be process
	 * @param f represents the temporary file of the asset to be processed
	 * @param bytes contains the file's bytes
	 * @param fv represents the current MMFG, where the results of this plugin should be fused into
	 */
	public void process(URL url, File f, byte[] bytes, MMFG fv);

	/**
	 * this method should indicate, if it returns recursive data, which are then re-processed within the GMAF framework
	 * @return true, if recursive data is available
	 */
	public boolean providesRecoursiveData();

	/**
	 * if this plugin contains overall metadata, e.g. EXIF data, this method should return true to indicate, that these general data have to be attached to the MMFG's root node
	 * @return true, if it is a general plugin
	 */
	public boolean isGeneralPlugin();

	/**
	 * this method is called by the GMAF to receive the results of this plugin and to fuse it into the MMFG
	 * @return a vector of MMFG nodes
	 */
	public Vector<Node> getDetectedNodes();

	/**
	 * this method returns, if the plugin is able to process files with a given extension
	 * @param extension
	 * @return true or false
	 */
	public boolean canProcess(String extension);

	default File prepareRecursiveProcessing(URL url, File f, byte[] bytes, MMFG fv) {
		return null;
	}
}
