package de.swa.gmaf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import de.swa.gmaf.plugin.GMAF_Plugin;
import de.swa.mmfg.MMFG;

/**
 * PluginChain must only create a temp file (via TempFileProvider) for assets
 * that have no file on disk, because nothing deletes those files again.
 */
public class TempFileUsageTest {

	private static final File TMP = new File(System.getProperty("java.io.tmpdir"));
	private final String id = UUID.randomUUID().toString();

	/** TempFileProvider names its files GMAF_TMP_<original name>_<random><suffix> */
	private File[] tempCopiesOf(String name) {
		File[] files = TMP.listFiles((dir, n) -> n.startsWith("GMAF_TMP_" + name + "_"));
		return files == null ? new File[0] : files;
	}

	private File fileNamed(String name) throws IOException {
		File f = new File(TMP, name);
		Files.write(f.toPath(), new byte[] { 1, 2, 3 });
		f.deleteOnExit();
		return f;
	}

	@AfterEach
	void removeTempCopies() {
		File[] files = TMP.listFiles((dir, n) -> n.startsWith("GMAF_TMP_") && n.contains(id));
		if (files != null) for (File f : files) f.delete();
	}

	private static PluginChain emptyChain() {
		return new PluginChain(new Vector<String>());
	}

	@Test
	void assetWithAFileOnDisk_isProcessedWithoutATempCopy() throws Exception {
		String name = "asset-" + id + ".bin";
		File original = fileNamed(name);

		MMFG m = emptyChain().processAsset(new byte[] { 1, 2, 3 }, name, "test", 1, 10, name, original);

		assertEquals(0, tempCopiesOf(name).length);
		assertEquals(original, m.getGeneralMetadata().getFileReference());
	}

	@Test
	void assetGivenOnlyAsBytes_getsATempFileThatIsKeptAsItsFile() {
		String name = "bytes-" + id + ".bin";

		MMFG m = emptyChain().processAsset(new byte[] { 1, 2, 3 }, name, "test", 1, 10, name, null);

		File[] copies = tempCopiesOf(name);
		assertEquals(1, copies.length);
		assertEquals(copies[0], m.getGeneralMetadata().getFileReference());
		assertTrue(copies[0].exists(), "previews and downloads read this file later");
	}

	// ---- sub-assets -------------------------------------------------------

	static File subAsset;

	public static class OneSubAssetPlugin implements GMAF_Plugin {
		public void process(URL url, File f, byte[] bytes, MMFG fv) {}
		public boolean providesRecoursiveData() { return true; }
		public boolean isGeneralPlugin() { return false; }
		public boolean canProcess(String extension) { return extension.equals(".bin"); }
		public File[] prepareRecursiveProcessing(URL url, File f, byte[] bytes, MMFG fv) {
			return new File[] { subAsset };
		}
	}

	@Test
	void subAssets_areProcessedFromTheirFileWithoutATempCopy() throws Exception {
		String name = "asset-" + id + ".bin";
		File original = fileNamed(name);
		String subName = "sub-" + id + ".sub";
		subAsset = fileNamed(subName);
		Vector<String> plugins = new Vector<String>();
		plugins.add(OneSubAssetPlugin.class.getName());

		new PluginChain(plugins).processAsset(new byte[] { 1, 2, 3 }, name, "test", 2, 10, name, original);

		assertEquals(0, tempCopiesOf(subName).length);
	}
}
