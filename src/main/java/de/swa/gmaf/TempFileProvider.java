package de.swa.gmaf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

/**
 * this class provides temporary files based on byte arrays, useful if a plugin can only process files
 **/
public class TempFileProvider {
	static Logger logger = LoggerFactory.getLogger(TempFileProvider.class);

	/**
	 * receives a byte array, creates a temp file and returns the File object
	 **/
	public static File provideTempFile(byte[] bytes, String suffix, String original_file) {
		if (bytes == null || bytes.length == 0) return null;
		try {
			File f = File.createTempFile("GMAF_TMP_" + original_file + "_", suffix);
			FileOutputStream fout = new FileOutputStream(f);
			fout.write(bytes);
			fout.close();
			return f;
		} catch (Exception x) {
			logger.error("Error storing file from byte array. Original File " + original_file + "; Suffix: " + suffix, x);
		}
		return null;
	}
}
