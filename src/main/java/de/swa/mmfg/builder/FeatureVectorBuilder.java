package de.swa.mmfg.builder;

import java.io.File;

import de.swa.gmaf.plugin.ExifHandler;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** utility class, that builds a MMFG in various ways **/
public class FeatureVectorBuilder {
	private static final Logger log = LoggerFactory.getLogger(FeatureVectorBuilder.class);

	/** returns a MMFG based on EXIF-data embedded in files **/
	public static MMFG getFeatureVectorFromAsset(File f) {
		return ExifHandler.getFeatureVectorFromExif(f);
	}
	
	/** merges two MMFGs **/
	public static void mergeIntoFeatureVector(MMFG base, MMFG delta) {
		Node n = base.getCurrentNode();
		for (Node ni : delta.getNodes()) {
			if (ni.getName().equals("Root-Asset")) {
				for (Node nix : ni.getChildNodes()) {
					n.addChildNode(nix);
					nix.setFeatureVector(base);
				}
			}
			else {
				log.debug("Merging {} into {}", ni.getName(), n.getName());
				n.addChildNode(ni);
				ni.setFeatureVector(base);
			}
		}
	}
	
	public static MMFG generateFeatureVectorForAsset(String assetWithoutMfv) {
		return null;
	}
	
	/** applies a unflattener to a string to extract the MMFG **/
	public static MMFG unflatten(String s, Unflattener uf) {
		return uf.unflatten(s);
	}
	
	/** flattens a given MMFG with a Flattener into a String **/
	public static String flatten(MMFG fv, Flattener fs) {
		return fs.flatten(fv);
	}	
	
	public static String attach(String assetWithoutMfv, MMFG fv) {
		return "";	
	}
	
	public static MMFG parse(String meta) {
		return new MMFG();
	}
}
