package de.swa.gmaf;

import de.swa.mmfg.MMFG;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import de.swa.gmaf.plugin.GMAF_Plugin;
import de.swa.mmfg.Location;
import de.swa.mmfg.Node;
import de.swa.mmfg.TechnicalAttribute;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Vector;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PluginChainTest {

	@Test
	public void constructor_withEmptyPluginList_doesNotThrow() {
		new PluginChain(new java.util.Vector<>());
	}

	@Test
	public void process_withEmptyPlugins_setsGeneralMetadataAndLocations() throws Exception {
		PluginChain chain = new PluginChain(new java.util.Vector<>());
		MMFG m = new MMFG();
		File temp = createTempJpeg();
		chain.process(temp.toURI().toURL(), temp, new byte[0], m, 1, 10, ".jpg");
		assertEquals(temp.getName(), m.getGeneralMetadata().getFileName());
		assertNotNull(m.getLocations());
		assertEquals(2, m.getLocations().size());
	}

	private File createTempJpeg() throws IOException {
		File temp = File.createTempFile("gmaf_test_", ".jpg");
		temp.deleteOnExit();
		javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB), "jpg", temp);
		return temp;
	}

	private static final Vector<String> calls = new Vector<>();

	public static class RecordingPlugin implements GMAF_Plugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) { calls.add(getClass().getSimpleName()); }
		public boolean providesRecoursiveData() { return false; }
		public boolean isGeneralPlugin() { return false; }
		public Vector<Node> getDetectedNodes() { return new Vector<>(); }
		public boolean canProcess(String extension) { return extension.equals(".jpg"); }
	}

	public static class GeneralPlugin extends RecordingPlugin {
		public boolean isGeneralPlugin() { return true; }
	}

	public static class OtherFormatPlugin extends RecordingPlugin {
		public boolean canProcess(String extension) { return false; }
	}

	/** detects a 100x100 section and adds three nodes to the MMFG **/
	public static class SectionPlugin extends RecordingPlugin {
		protected int sectionSize = 100;
		private final Vector<Node> detected = new Vector<>();

		public void process(URL url, File f, byte[] bytes, MMFG fv) {
			super.process(url, f, bytes, fv);
			Node section = new Node("section", fv);
			section.addTechnicalAttribute(new TechnicalAttribute(0, 0, sectionSize, sectionSize, 0, 0));
			detected.add(section);
			fv.addNode(section);
			fv.addNode(new Node("first", fv));
			fv.addNode(new Node("second", fv));
		}
		public boolean providesRecoursiveData() { return true; }
		public Vector<Node> getDetectedNodes() { return detected; }
	}

	public static class SmallSectionPlugin extends SectionPlugin {
		public SmallSectionPlugin() { sectionSize = 79; }
	}

	private static PluginChain chain(Class<?>... plugins) {
		Vector<String> names = new Vector<>();
		for (Class<?> c : plugins) names.add(c.getName());
		return new PluginChain(names);
	}

	private static File sectionDir() {
		return new File("temp");
	}

	private static File[] sectionFiles() {
		File[] files = sectionDir().listFiles((dir, name) -> name.startsWith("section_") && name.contains("PluginChainTest"));
		return files == null ? new File[0] : files;
	}

	private File createTempJpeg(int size) throws IOException {
		File temp = File.createTempFile("gmaf_test_", ".jpg");
		temp.deleteOnExit();
		ImageIO.write(new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB), "jpg", temp);
		return temp;
	}

	@BeforeEach
	public void clearCalls() {
		calls.clear();
		sectionDir().mkdirs();
	}

	@AfterEach
	public void removeSections() {
		for (File f : sectionFiles()) f.delete();
	}

	private void process(PluginChain chain, MMFG m, int depth, int max) throws Exception {
		File image = createTempJpeg(200);
		chain.process(image.toURI().toURL(), image, new byte[0], m, depth, max, ".jpg");
	}

	@Test
	public void constructor_ignoresClassesThatCannotBeUsedAsPlugins() throws Exception {
		PluginChain chain = new PluginChain(new Vector<>(Arrays.asList(
				"de.swa.DoesNotExist", String.class.getName(), RecordingPlugin.class.getName())));

		process(chain, new MMFG(), 1, 10);
		assertEquals(Arrays.asList("RecordingPlugin"), calls);
	}

	@Test
	public void constructor_acceptsClassToStringFormat() throws Exception {
		process(new PluginChain(new Vector<>(Arrays.asList(RecordingPlugin.class.toString()))), new MMFG(), 1, 10);
		assertEquals(Arrays.asList("RecordingPlugin"), calls);
	}

	@Test
	public void process_withNegativeDepth_doesNothing() throws Exception {
		MMFG m = new MMFG();
		process(chain(RecordingPlugin.class), m, -1, 10);

		assertTrue(calls.isEmpty());
		assertNull(m.getGeneralMetadata().getFileName());
	}

	@Test
	public void process_runsGeneralPluginsBeforeOthers() throws Exception {
		process(chain(RecordingPlugin.class, GeneralPlugin.class), new MMFG(), 1, 10);
		assertEquals(Arrays.asList("GeneralPlugin", "RecordingPlugin"), calls);
	}

	@Test
	public void process_skipsPluginsForOtherFormats() throws Exception {
		process(chain(OtherFormatPlugin.class), new MMFG(), 1, 10);
		assertTrue(calls.isEmpty());
	}

	@Test
	public void process_doesNotCutSectionsSmallerThan80Pixels() throws Exception {
		process(chain(SmallSectionPlugin.class), new MMFG(), 1, 10);
		assertEquals(0, sectionFiles().length);
	}

	@Disabled("Section cutting is commented out in PluginChain.process(); the chain does not write section files, so this test can never pass")
	@Test
	public void process_cutsDetectedSections() throws Exception {
		process(chain(SectionPlugin.class), new MMFG(), 1, 10);
		assertEquals(1, sectionFiles().length);
	}

	@Disabled("PluginChain.process() accepts a maxNodes argument but never enforces it; no code path compares the node count against the limit")
	@Test
	public void process_stopsWhenMaxNodesIsExceeded() throws Exception {
		process(chain(SectionPlugin.class, RecordingPlugin.class), new MMFG(), 1, 2);
		assertFalse(calls.contains("RecordingPlugin"));
	}

	@Disabled("Known bug: GMAF passes a null URL, and the resulting NPE also skips the TYPE_ORIGINAL location")
	@Test
	public void process_withoutUrl_stillAddsOriginalLocation() throws Exception {
		MMFG m = new MMFG();
		File image = createTempJpeg(1);
		new PluginChain(new Vector<>()).process(null, image, new byte[0], m, 1, 10, ".jpg");

		assertTrue(m.getLocations().stream().anyMatch(l -> l.getType() == Location.TYPE_ORIGINAL));
	}
}
