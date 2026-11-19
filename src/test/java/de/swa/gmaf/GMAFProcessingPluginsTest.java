package de.swa.gmaf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
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
}
