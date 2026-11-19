package de.swa.gmaf.plugin;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;

import de.swa.mmfg.MMFG;

public class GMAF_PluginTest {

	/** a plugin no longer has to implement the deprecated getDetectedNodes() */
	static class MinimalPlugin implements GMAF_Plugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) {}
		public boolean providesRecoursiveData() { return false; }
		public boolean isGeneralPlugin() { return false; }
		public boolean canProcess(String extension) { return true; }
	}

	@Test
	@SuppressWarnings("deprecation")
	void getDetectedNodes_defaultsToEmptyVector() {
		GMAF_Plugin p = new MinimalPlugin();
		assertNotNull(p.getDetectedNodes());
		assertTrue(p.getDetectedNodes().isEmpty());
	}
}
