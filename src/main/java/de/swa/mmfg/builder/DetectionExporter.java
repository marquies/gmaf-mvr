package de.swa.mmfg.builder;

import java.util.Date;
import java.util.Vector;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeGenerator;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** export the complete list of MMFGs as XML **/
public class DetectionExporter implements Flattener, Unflattener {
	private static final Logger log = LoggerFactory.getLogger(DetectionExporter.class);

	public String flatten(MMFG fv) {	
		String fn = fv.getGeneralMetadata().getFileName();
		try {
			fn = fv.getGeneralMetadata().getFileReference().getName();
			log.debug("Found original file: {}", fv.getGeneralMetadata().getFileReference().getAbsolutePath());
		}
		catch (Exception x) {}
		if (fn.indexOf("GMAF_TMP_") >= 0) {
			fn = fn.substring(9, fn.length());
			fn = fn.substring(0, fn.indexOf("_"));
			log.debug("Found temp file for: {}", fn);
		}
		String s = "<gmaf-data>\n"
				+ "<file>" + fn + "</file>\n"
				+ "<date>" + new Date() + "</date>\n"
				+ "<objects>\n";

		StringBuffer sb = new StringBuffer();
		for (Node n : fv.getNodes()) {
			appendNode(n, sb);
		}
		s += sb.toString();
		s += "</objects>\n"
			+ "</gmaf-data>";

		return s;
	}
	
	private void appendNode(Node n, StringBuffer sb) {
		TechnicalAttribute bbox = new TechnicalAttribute(0,0,0,0,0,0);

		for (TechnicalAttribute ta : n.getTechnicalAttributes()) {
			if (ta.getRelative_x() != 0 || ta.getRelative_y() != 0 || ta.getWidth() != 0 || ta.getHeight() != 0) {
				bbox = ta;
				log.debug("Found bounding box");
			}
		}
		String s = "	<object>\n"
			+ "		<term>" + n.getName() + "</term>\n"
			+ "		<bounding-box>\n"
			+ "			<x>" + bbox.getRelative_x() + "</x>\n"
			+ "			<y>" + bbox.getRelative_y() + "</y>\n"
			+ "			<width>" + bbox.getWidth() + "</width>\n"
			+ "			<height>" + bbox.getHeight() + "</height>\n"
			+ "		</bounding-box>\n"
			+ "		<probability>100</probability>\n"
			+ "	</object>\n"
			+ "";
		sb.append(s);
		
		for (Node ni : n.getChildNodes()) appendNode(ni, sb);
	}
	
	public MMFG unflatten(String xml) {
		return null;
	}
	
	public String getFileExtension() {
		return "xml";
	}

	public String startFile() { return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
			+ "<gmaf-collection xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"gmaf_schema.xsd\">" ; }
	public String endFile() { return "</gmaf-collection>"; }

}
