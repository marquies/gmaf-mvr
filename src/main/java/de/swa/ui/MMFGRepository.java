package de.swa.ui;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import de.swa.gc.GraphCode;
import de.swa.gc.GraphCodeGenerator;
import de.swa.gc.GraphCodeIO;
import de.swa.mmfg.MMFG;
import de.swa.mmfg.builder.FeatureVectorBuilder;
import de.swa.mmfg.builder.XMLEncodeDecode;

/**
 * Writes a processing result to the files MMFGCollection.init() loads at
 * startup: &lt;asset name&gt;.mmfg in the MMFG repository and &lt;asset
 * name&gt;.gc in the GraphCode repository.
 **/
public class MMFGRepository {

	private MMFGRepository() {
	}

	/** writes to the repositories set in the configuration */
	public static void store(File asset, MMFG mmfg) throws IOException {
		Configuration c = Configuration.getInstance();
		store(asset, mmfg, new File(c.getMMFGRepo()), new File(c.getGraphCodeRepository()));
	}

	public static void store(File asset, MMFG mmfg, File mmfgRepo, File gcRepo) throws IOException {
		mmfgRepo.mkdirs();
		gcRepo.mkdirs();
		String xml = FeatureVectorBuilder.flatten(mmfg, new XMLEncodeDecode());
		Files.writeString(new File(mmfgRepo, asset.getName() + ".mmfg").toPath(), xml, StandardCharsets.UTF_8);
		GraphCode gc = GraphCodeGenerator.generate(mmfg);
		GraphCodeIO.write(gc, new File(gcRepo, asset.getName() + ".gc"));
	}
}
