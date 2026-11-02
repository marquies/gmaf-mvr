package de.swa.experiment;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeGenerator;
import de.swa.gmaf.plugin.fusion.SpacialFeatureFusion;
import de.swa.mmfg.CompositionRelationship;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/** one annotated PASCAL VOC image, with the objects marked as difficult left out **/
public final class VocImage {
	public static final class Box {
		final String label;
		final int xmin, ymin, xmax, ymax;

		Box(String label, int xmin, int ymin, int xmax, int ymax) {
			this.label = label;
			this.xmin = xmin;
			this.ymin = ymin;
			this.xmax = xmax;
			this.ymax = ymax;
		}
	}

	public final String name;
	public final List<Box> boxes = new ArrayList<Box>();
	public final GraphCode graphCode;

	private VocImage(String name, List<Box> boxes, boolean correctLabels) {
		this.name = name;
		this.boxes.addAll(boxes);
		this.graphCode = GraphCodeGenerator.generate(toMmfg(correctLabels));
	}

	/** detection is replaced by the gold annotations, the spatial edges come from the production fusion **/
	private MMFG toMmfg(boolean correctLabels) {
		MMFG m = new MMFG();
		m.getGeneralMetadata().setFileName(name);
		for (Box b : boxes) {
			Node n = new Node(b.label, m);
			n.addTechnicalAttribute(new TechnicalAttribute(b.xmin, b.ymin, b.xmax - b.xmin, b.ymax - b.ymin, 0, 0));
			m.addNode(n);
		}
		new SpacialFeatureFusion().optimize(m, null);
		if (correctLabels) swapAboveAndUnder(m);
		return m;
	}

	/** control for SpacialFeatureFusion labelling the upper node UNDER, y grows downwards in images **/
	private static void swapAboveAndUnder(MMFG m) {
		Set<Node> seen = Collections.newSetFromMap(new IdentityHashMap<Node, Boolean>());
		for (Node n : m.getAllNodes()) {
			if (seen.add(n)) {
				for (CompositionRelationship cr : n.getCompositionRelationships()) {
					if (cr.getType() == CompositionRelationship.RELATION_ABOVE) cr.setType(CompositionRelationship.RELATION_UNDER);
					else if (cr.getType() == CompositionRelationship.RELATION_UNDER) cr.setType(CompositionRelationship.RELATION_ABOVE);
				}
			}
		}
	}

	public boolean contains(String label) {
		return boxes.stream().anyMatch(b -> b.label.equals(label));
	}

	public static VocImage parse(File annotation, boolean correctLabels) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		Document document = factory.newDocumentBuilder().parse(annotation);

		List<Box> boxes = new ArrayList<Box>();
		NodeList objects = document.getElementsByTagName("object");
		for (int i = 0; i < objects.getLength(); i++) {
			Element object = (Element) objects.item(i);
			if ("1".equals(text(object, "difficult"))) continue;
			boxes.add(new Box(text(object, "name"), Integer.parseInt(text(object, "xmin")),
					Integer.parseInt(text(object, "ymin")), Integer.parseInt(text(object, "xmax")),
					Integer.parseInt(text(object, "ymax"))));
		}
		return new VocImage(annotation.getName().replace(".xml", ""), boxes, correctLabels);
	}

	private static String text(Element parent, String tag) {
		NodeList list = parent.getElementsByTagName(tag);
		return list.getLength() == 0 ? "" : list.item(0).getTextContent().trim();
	}

	public static Vector<VocImage> load(File directory, boolean correctLabels) throws Exception {
		File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
		Arrays.sort(files);
		Vector<VocImage> images = new Vector<VocImage>();
		for (File f : files) images.add(parse(f, correctLabels));
		return images;
	}
}
