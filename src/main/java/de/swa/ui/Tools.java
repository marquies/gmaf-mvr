package de.swa.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import javax.imageio.ImageIO;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.slf4j.LoggerFactory;

/** utility class **/
public class Tools {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Tools.class);

	/** returns a scaled image instance including bounding boxes **/
	public static Image getScaledInstance(String name, Image srcImg, int w, boolean showBoundingBox){
		String bb = "";
		if (showBoundingBox) bb = "_BB_";
		File thumbNail = new File(Configuration.getInstance().getThumbnailPath() + File.separatorChar + name + bb + "_" + w + ".png");
		if (thumbNail.exists() && !showBoundingBox) {
			BufferedImage bi;
			try {
				bi = ImageIO.read(thumbNail);
				return bi;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		int side = Math.min(srcImg.getWidth(null), srcImg.getHeight(null));
		int horizontalOffset = srcImg.getWidth(null) - side;
		int verticalOffset = srcImg.getHeight(null) - side;
		int scale = side / w;
		
	    BufferedImage resizedImg = new BufferedImage(w, w, BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g2 = resizedImg.createGraphics();

	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    g2.drawImage(srcImg, 0, 0, w, w, horizontalOffset / 2, verticalOffset / 2, srcImg.getWidth(null) - horizontalOffset / 2, srcImg.getHeight(null) - verticalOffset / 2, null);
	    
	    if (showBoundingBox) {
		    File f = Configuration.getInstance().getSelectedAsset();
		    MMFG fv = MMFGCollectionFactory.createOrGetCollection().getMMFGForFile(f);
		    drawBoundingBoxes(fv, g2, scale, horizontalOffset / 2, verticalOffset / 2);
	    }
	    g2.dispose();
	    
	    try {
			ImageIO.write(resizedImg, "png", thumbNail);
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return resizedImg;
	}
	
	private static void drawBoundingBoxes(MMFG fv, Graphics2D g2, int scale, int xo, int yo) {
		painted.clear();
    	// Root-Node
	    for (Node n : fv.getNodes()) {
	    	// 1st Level of Detail
		    for (Node ni : n.getChildNodes()) {
		    	try {
		    		TechnicalAttribute ta = ni.getTechnicalAttributes().get(0);
		    		drawBoundingBox(ni.getName(), g2, (ta.getRelative_x() - xo) / scale, (ta.getRelative_y() - yo) / scale, ta.getWidth() / scale, ta.getHeight() / scale, Color.red, Color.white);
		    	}
		    	catch (Exception ex) {}
		    	int xoffset = 0; try { xoffset = ni.getTechnicalAttributes().get(0).getRelative_x(); } catch (Exception ex) {}
		    	int yoffset = 0; try { yoffset = ni.getTechnicalAttributes().get(0).getRelative_y(); } catch (Exception ex) {}
			    
			    for (Node nj : ni.getChildNodes()) {
			    	try {
			    		TechnicalAttribute ta = ni.getTechnicalAttributes().get(0);
			    		drawBoundingBox(ni.getName(), g2, (ta.getRelative_x() - xo + xoffset) / scale, (ta.getRelative_y() - yo + yoffset) / scale, ta.getWidth() / scale, ta.getHeight() / scale, Color.orange, Color.black);
			    	}
			    	catch (Exception ex) {}
				    int xoffset2 = 0; try { xoffset2 = nj.getTechnicalAttributes().get(0).getRelative_x(); } catch (Exception ex) {}
				    int yoffset2 = 0; try { yoffset2 = nj.getTechnicalAttributes().get(0).getRelative_y(); } catch (Exception ex) {}
				    
				    for (Node nk : nj.getChildNodes()) {
				    	try {
				    		TechnicalAttribute ta = ni.getTechnicalAttributes().get(0);
				    		drawBoundingBox(ni.getName(), g2, (ta.getRelative_x() - xo + xoffset + xoffset2) / scale, (ta.getRelative_y() - yo + yoffset + yoffset2) / scale, ta.getWidth() / scale, ta.getHeight() / scale, Color.yellow, Color.black);
				    	}
				    	catch (Exception ex) {}
				    }
			    }
		    }
	    }
	}

	private static Vector<String> painted = new Vector<String>();
	private static void drawBoundingBox(String s, Graphics2D g, int x, int y, int w, int h, Color box, Color font) {
		if (painted.contains(s)) return;
		g.setColor(box);
		if (s == null || s.equals("")) return;
		log.debug("BB: {}: {}, {}, {}, {}", s, x, y, w, h);
    	g.drawRect(x, y, w, h);
    	g.fillRect(x,y,w,14);
    	g.setColor(Color.WHITE);
    	g.setFont(new Font("Arial", Font.PLAIN, 10));
    	g.setColor(font);
    	g.drawString(s, x + 2, y + 11);
    	painted.add(s);
	}

}
