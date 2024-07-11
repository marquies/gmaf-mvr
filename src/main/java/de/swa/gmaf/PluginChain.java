package de.swa.gmaf;

import de.swa.gmaf.plugin.GMAF_Plugin;
import de.swa.gmaf.plugin.fusion.SpacialFeatureFusion;
import de.swa.mmfg.*;
import de.swa.mmfg.builder.FeatureVectorBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Vector;

/**
 * this class chains the filters and plugins for GMAF processing
 **/
public class PluginChain {
	private Vector<GMAF_Plugin> plugins = new Vector<GMAF_Plugin>();

	/**
	 * constructor, which receives a vector of plugin-classes for the current GMAF processing
	 **/
	public PluginChain(Vector<String> pluginClasses) {
		for (String s : pluginClasses) {
			try {
				if (s.startsWith("class"))
					s = s.substring(5, s.length()).trim();
				Class c = Class.forName(s);
				Object o = c.newInstance();
				GMAF_Plugin fvp = (GMAF_Plugin) o;
				plugins.add(fvp);

			} catch (Exception x) {
				System.out.println(x);
			}
		}
	}

	/**
	 * processes a given asset
	 **/
	public void process(URL url, File f, byte[] bytes, MMFG fv, int depth, int max, String extension) {
		if (depth < 0)
			return;
		String depth_offset = "";
		for (int i = 0; i < depth; i++)
			depth_offset += "  ";

		// process general data
		GeneralMetadata gm = new GeneralMetadata();
		gm.setFileName(f.getName());
		gm.setFileReference(f);
		fv.setGeneralMetadata(gm);

		for (GMAF_Plugin fvp : plugins) {

			if (fvp.canProcess(extension)) {
				if (fvp.isGeneralPlugin())
					fvp.process(url, f, bytes, fv);
			}
		}

		try {
			Location loc = new Location(Location.TYPE_HIGHRES, url, url.toString());
			fv.addLocation(loc);
			loc = new Location(Location.TYPE_ORIGINAL, new URL("file:///" + f.getAbsolutePath()), f.getName());
			fv.addLocation(loc);
		} catch (Exception x) {
		}

		// process details

		for (GMAF_Plugin fvp : plugins) {
			if (fvp.canProcess(extension)) {
				if (!fvp.isGeneralPlugin()) {
					fvp.process(url, f, bytes, fv);
					System.out.println("processing with plugin " + fvp.getClass());

					if (fvp.providesRecoursiveData()) {
						Vector<Node> nodes = fvp.getDetectedNodes();

						for (Node n : nodes) {
							fv.setCurrentNode(n);

							for (TechnicalAttribute ta : n.getTechnicalAttributes()) {
								String box = (int) (ta.getRelative_x() / 100) * 100 + ""
										+ (int) (ta.getRelative_y() / 100) * 100 + ""
										+ (int) (ta.getWidth() / 100) * 100 + "" + (int) (ta.getHeight() / 100) * 100;

								try {
									if (ta.getWidth() < 80)
										continue;
									if (ta.getHeight() < 80)
										continue;

									// cut image
									BufferedImage img = ImageIO.read(f);
									BufferedImage section = img.getSubimage(ta.getRelative_x(), ta.getRelative_y(),
											ta.getWidth(), ta.getHeight());

									if (extension.equalsIgnoreCase(".png")) {
										BufferedImage newImage = new BufferedImage(section.getWidth(), section.getHeight(), BufferedImage.TYPE_INT_RGB);
										newImage.createGraphics().drawImage(section, 0, 0, Color.BLACK, null);
										section = newImage;
									}


									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									boolean returnValue = ImageIO.write(section, "jpg", baos);
									if (!returnValue) {
										throw new RuntimeException("Could not create subimage from image '" + f.getName() + "'");
									}
									baos.flush();
									byte[] sectionBytes = baos.toByteArray();
									baos.close();

									FileOutputStream fout = new FileOutputStream(new File("temp/section_"
											+ fvp.getClass().getName() + "_" + System.currentTimeMillis() + ".jpg"));
									ImageIO.write(section, "jpg", fout);

									// reprocess parts of this image
									System.out.println(
											depth_offset + "  reprocessing TA " + ta.getWidth() + " " + ta.getHeight());
									MMFG newImg = new GMAF().processAsset(sectionBytes, "section.jpg", "tmp", depth--,
											max, f.getName(), null);

									FeatureVectorBuilder.mergeIntoFeatureVector(fv, newImg);

									if (fv.getNodes().size() > max) {
										System.out.println("max number of nodes reached - returning");
										return;
									}
								} catch (Exception x) {
									System.out.println("Exception while trying to process file: " + f.getAbsolutePath() + f.getName());
									x.printStackTrace();
								}
							}
						}
					}
				}
			}
		}

		// calculate spacial relationships based on the bounding boxes
//		SpacialFeatureFusion sff = new SpacialFeatureFusion();
//		sff.optimize(fv, null);
	}
}
