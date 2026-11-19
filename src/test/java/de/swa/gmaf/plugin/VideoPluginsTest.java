package de.swa.gmaf.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.swa.gc.GraphCodeGenerator;
import de.swa.gmaf.GMAF;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

public class VideoPluginsTest {

	// ---- without ffmpeg ---------------------------------------------------

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

	// ---- with ffmpeg ------------------------------------------------------

	@TempDir
	static Path videos;
	static File threeShots;   // 6 s: red, blue, green for 2 s each
	static File twentyFive;   // 25 s test pattern
	static File eight;        // 8 s test pattern

	static boolean ffmpegAvailable() {
		try {
			return new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start().waitFor() == 0;
		} catch (Exception x) {
			return false;
		}
	}

	@BeforeAll
	static void createVideos() throws Exception {
		if (!ffmpegAvailable()) return;
		threeShots = videos.resolve("shots.mp4").toFile();
		FfmpegSupport.run(Arrays.asList("-v", "error",
				"-f", "lavfi", "-i", "color=c=red:s=160x120:d=2:r=10",
				"-f", "lavfi", "-i", "color=c=blue:s=160x120:d=2:r=10",
				"-f", "lavfi", "-i", "color=c=green:s=160x120:d=2:r=10",
				"-filter_complex", "[0][1][2]concat=n=3:v=1:a=0", "-pix_fmt", "yuv420p", threeShots.getPath()));
		twentyFive = testPattern(25);
		eight = testPattern(8);
	}

	static File testPattern(int seconds) throws Exception {
		File f = videos.resolve("pattern_" + seconds + "s.mp4").toFile();
		FfmpegSupport.run(Arrays.asList("-v", "error", "-f", "lavfi",
				"-i", "testsrc=duration=" + seconds + ":size=160x120:rate=10", "-pix_fmt", "yuv420p", f.getPath()));
		return f;
	}

	/** marks every PNG it sees, to show sub-assets ran through the session's plugins */
	public static class FrameMarker implements GMAF_Plugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) {
			fv.getCurrentNode().addChildNode(new Node("frame-marker", fv));
		}
		public boolean providesRecoursiveData() { return false; }
		public boolean isGeneralPlugin() { return false; }
		public boolean canProcess(String extension) { return ".png".equals(extension); }
	}

	static MMFG process(File video, Class<?>... plugins) throws Exception {
		Vector<String> names = new Vector<String>();
		for (Class<?> c : plugins) names.add(c.getName());
		GMAF session = new GMAF();
		session.setProcessingPlugins(names);
		return session.processAsset(Files.readAllBytes(video.toPath()), video.getName(), "test", 3, 500, video.getName(), null);
	}

	static List<String> childNames(Node n) {
		List<String> names = new ArrayList<String>();
		for (Node c : n.getChildNodes()) names.add(c.getName());
		return names;
	}

	@Test
	void shotDetection_findsEachShotAndAnalysesItsFrameWithTheSessionsPlugins() throws Exception {
		assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
		MMFG m = process(threeShots, VideoShotDetection.class, FrameMarker.class);

		Node root = m.getCurrentNode();
		assertEquals(Arrays.asList("00:00:00.000", "00:00:02.000", "00:00:04.000"), childNames(root));
		for (Node shot : root.getChildNodes())
			assertTrue(childNames(shot).contains("frame-marker"), shot.getName() + " -> " + childNames(shot));
		assertTrue(GraphCodeGenerator.generate(m).getDictionary().contains("frame-marker"));
	}

	@Test
	void splitter_cutsTenSecondSegmentsWithTheirRealStartTimes() throws Exception {
		assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
		File[] segments = new VideoSplitter().prepareRecursiveProcessing(null, twentyFive, null, new MMFG());

		assertNotNull(segments);
		assertEquals(3, segments.length);
		assertEquals("00:00:00.000", FfmpegSupport.timecode(segments[0]));
		for (File s : segments) assertTrue(s.length() > 0, s + " is empty");
		for (File s : segments) FfmpegSupport.deleteWithEmptyParent(s);
	}

	@Test
	void subAssetFolders_areRemovedOnceEverythingIsMerged() throws Exception {
		assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
		File[] shots = new VideoShotDetection().prepareRecursiveProcessing(null, threeShots, null, new MMFG());
		assertNotNull(shots);
		File dir = shots[0].getParentFile();

		MMFG parent = new MMFG();
		Node root = new Node("Root-Asset", parent);
		parent.addNode(root);
		parent.setCurrentNode(root);
		for (File s : shots) new VideoShotDetection().mergeRecursiveResult(s, new MMFG(), parent);
		assertFalse(dir.exists(), dir + " still exists");
	}

	@Test
	void splitter_leavesVideosShorterThanTwoSegmentsAlone() throws Exception {
		assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
		assertNull(new VideoSplitter().prepareRecursiveProcessing(null, eight, null, new MMFG()));
	}

	@Test
	void splitter_doesNotSplitItsOwnSegmentsAgain() throws Exception {
		assumeTrue(ffmpegAvailable(), "ffmpeg not on PATH");
		MMFG m = process(twentyFive, VideoSplitter.class);

		Node root = m.getCurrentNode();
		assertEquals(3, root.getChildNodes().size(), "got " + childNames(root));
		for (Node segment : root.getChildNodes())
			assertTrue(segment.getChildNodes().isEmpty(), segment.getName() + " was split again: " + childNames(segment));
	}
}
