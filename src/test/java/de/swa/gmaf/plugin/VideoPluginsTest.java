package de.swa.gmaf.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.swa.gc.GraphCodeGenerator;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/** Unit tests of the video plugins that need no ffmpeg; see {@link VideoPluginsIT} for the rest. */
public class VideoPluginsTest {

	@Test
	void canProcess_acceptsVideoExtensionsWithTheLeadingDotThePluginChainPasses() {
		for (GMAF_Plugin p : new GMAF_Plugin[] { new VideoSplitter(), new VideoShotDetection() }) {
			assertTrue(p.canProcess(".mp4"));
			assertTrue(p.canProcess(".MOV"));
			assertTrue(p.canProcess("mp4"));
			assertFalse(p.canProcess(".jpg"));
			assertFalse(p.canProcess(null));
		}
	}

	@Test
	void timecode_isReadFromTheMillisInTheFileName() {
		assertEquals("00:00:00.000", FfmpegSupport.timecode(new File("segment_0.mp4")));
		assertEquals("00:00:10.200", FfmpegSupport.timecode(new File("segment_10200.mp4")));
		assertEquals("01:02:03.004", FfmpegSupport.timecode(new File("shot_3723004.png")));
		assertNull(FfmpegSupport.timecode(new File("frame.png")));
	}

	@Test
	void mergeUnderTimecode_nestsTheSubAssetAndAddsItsNodesForTheGraphCode() {
		MMFG parent = new MMFG();
		Node root = new Node("Root-Asset", parent);
		parent.addNode(root);
		parent.setCurrentNode(root);

		MMFG sub = new MMFG();
		Node subRoot = new Node("Root-Asset", sub);
		sub.addNode(subRoot);
		Node car = new Node("car", sub);
		subRoot.addChildNode(car);
		car.addChildNode(new Node("red", sub));

		FfmpegSupport.mergeUnderTimecode(new File("shot_2000.png"), sub, parent);

		Node tc = root.getChildNodes().get(0);
		assertEquals("00:00:02.000", tc.getName());
		assertEquals("car", tc.getChildNodes().get(0).getName());
		List<String> dictionary = GraphCodeGenerator.generate(parent).getDictionary();
		assertTrue(dictionary.containsAll(Arrays.asList("00:00:02.000", "car", "red")), "got " + dictionary);
		assertEquals(1, dictionary.stream().filter("root-asset"::equalsIgnoreCase).count());
	}
}
