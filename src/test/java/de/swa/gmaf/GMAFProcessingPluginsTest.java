package de.swa.gmaf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.net.URL;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.swa.gmaf.plugin.GMAF_Plugin;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/**
 * setProcessingPlugins must apply to the instance it was called on (one API
 * session), not to every GMAF in the process, and must still take effect after
 * that instance has processed assets.
 */
public class GMAFProcessingPluginsTest {

	static final AtomicInteger calls = new AtomicInteger();

	public static class CountingPlugin implements GMAF_Plugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) { calls.incrementAndGet(); }
		public boolean providesRecoursiveData() { return false; }
		public boolean isGeneralPlugin() { return false; }
		public Vector<Node> getDetectedNodes() { return new Vector<Node>(); }
		public boolean canProcess(String extension) { return true; }
	}

	private static Vector<String> countingPlugin() {
		Vector<String> v = new Vector<String>();
		v.add(CountingPlugin.class.getName());
		return v;
	}

	private static void process(GMAF gmaf) {
		gmaf.processAsset(new byte[] { 1, 2, 3 }, "asset.bin", "test", 1, 10, "asset.bin", null);
	}

	@BeforeEach
	void reset() {
		calls.set(0);
	}

	@Test
	void pluginsSetOnOneInstance_doNotApplyToAnother() {
		GMAF session = new GMAF();
		session.setProcessingPlugins(countingPlugin());
		process(session);
		assertEquals(1, calls.get(), "the session's own plugin runs");

		process(new GMAF());
		assertEquals(1, calls.get(), "another instance keeps the configured default plugins");
	}

	@Test
	void pluginsSetAfterProcessing_takeEffect() {
		GMAF session = new GMAF();
		process(session);
		assertEquals(0, calls.get());

		session.setProcessingPlugins(countingPlugin());
		process(session);
		assertEquals(1, calls.get());
	}

	@Test
	void emptyPluginList_revertsToDefault() {
		GMAF session = new GMAF();
		session.setProcessingPlugins(countingPlugin());
		session.setProcessingPlugins(new Vector<String>());
		process(session);
		assertEquals(0, calls.get());
	}

	// ---- recursive processing ---------------------------------------------

	/** names of the files the session's plugins were run on, in order */
	static final Vector<String> processed = new Vector<String>();

	public static class RecordingPlugin extends CountingPlugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) { processed.add(f.getName()); }
	}

	/** hands one sub-asset back to the framework, but only for the top-level ".bin" asset */
	public static class OneSubAssetPlugin extends CountingPlugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) {}
		public boolean providesRecoursiveData() { return true; }
		public boolean canProcess(String extension) { return extension.equals(".bin"); }
		public File[] prepareRecursiveProcessing(URL url, File f, byte[] bytes, MMFG fv) {
			return new File[] { subAsset(".sub") };
		}
	}

	/** hands a sub-asset back for every asset, so only the depth limit stops it */
	public static class EndlessSubAssetPlugin extends OneSubAssetPlugin {
		public boolean canProcess(String extension) { return true; }
	}

	static File subAsset(String suffix) {
		try {
			File f = File.createTempFile("gmaf_sub_", suffix);
			f.deleteOnExit();
			Files.write(f.toPath(), new byte[] { 4, 5, 6 });
			return f;
		} catch (IOException x) {
			throw new UncheckedIOException(x);
		}
	}

	private static Vector<String> plugins(Class<?>... classes) {
		Vector<String> v = new Vector<String>();
		for (Class<?> c : classes) v.add(c.getName());
		return v;
	}

	@Test
	void subAssets_areProcessedWithTheSessionsPlugins() {
		processed.clear();
		GMAF session = new GMAF();
		session.setProcessingPlugins(plugins(RecordingPlugin.class, OneSubAssetPlugin.class));
		process(session);

		assertEquals(2, processed.size(), "parent and sub-asset, got " + processed);
		assertTrue(processed.get(0).endsWith(".bin"));
		assertTrue(processed.get(1).endsWith(".sub"));
	}

	@Test
	void subAssets_goOneLevelDeeper_untilMaxRecursions() {
		processed.clear();
		GMAF session = new GMAF();
		session.setProcessingPlugins(plugins(RecordingPlugin.class, EndlessSubAssetPlugin.class));
		session.processAsset(new byte[] { 1, 2, 3 }, "asset.bin", "test", 2, 10, "asset.bin", null);

		// depth 2 (the asset), 1 and 0 (sub-assets); depth -1 is not processed
		assertEquals(3, processed.size(), "got " + processed);
	}
}
