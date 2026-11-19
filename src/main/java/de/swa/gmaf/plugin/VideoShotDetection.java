package de.swa.gmaf.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;

import de.swa.gmaf.GMAF;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/** automated scene detection based on FFMPEG **/
public class VideoShotDetection implements GMAF_Plugin {
	public boolean canProcess(String extension) {
		String[] extensions = {"mpg", "mpeg", "mov", "flv", "mp4", "mxf", "qt", "m4v"};
		for (String ext : extensions) {
			if (ext.equalsIgnoreCase(extension)) return true;
		}
		return false;
	}

	public void process(URL url, File f, byte[] bytes, MMFG fv) {
		if (!f.exists()) {
			// Download the video
			try {
				URLConnection uc = url.openConnection();
				String extension = url.toString();
				extension = extension.substring(extension.lastIndexOf("."), extension.length());
				f = new File("temp/" + System.currentTimeMillis() + extension);
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(uc.getInputStream().readAllBytes());
				fout.close();
			}
			catch (Exception x) {
				x.printStackTrace();
			}
		}
		if (!f.exists()) {
			// create the video out of the bytes
			try {
				f = new File("temp/" + System.currentTimeMillis() + ".mp4");
				FileOutputStream fout = new FileOutputStream(f);
				fout.write(bytes);
				fout.close();
			}
			catch (Exception x) {
				x.printStackTrace();
			}
		}
		if (f.exists()) {
			// create folder for the scene screenshots
			File scene_screenshots = new File("temp/" + f.getName() + "_Scenes");
			scene_screenshots.mkdirs();
			String[] cmd = {"ffmpeg", "-i", 
					f.getAbsolutePath(), 
					"-vf", "select='gt(scene\\,0,4)'", 
					"-vsync", "vfr", 
					"temp/" + f.getName() + "_Scenes/frame_%d.png"};
			try {
				Runtime.getRuntime().exec(cmd);
				
				File[] fs = new File("temp/" + f.getName() + "_Scenes").listFiles();
				GMAF gmaf = new GMAF();
				for (File fi : fs) {
					MMFG sceneMMFG = gmaf.processAsset(fi);
					String name = fi.getName();
					String timecode = name.substring(name.indexOf("_"), name.indexOf("."));
					Node tcNode = new Node(timecode, fv);
					for (Node n : sceneMMFG.getNodes()) {
						tcNode.addChildNode(n);
					}
					fv.addNode(tcNode);
				}
			}
			catch (Exception x) {
				x.printStackTrace();
			}
		}
	}

	public boolean isGeneralPlugin() {
		return false;
	}


	public boolean providesRecoursiveData() {
		return false;
	}
}
