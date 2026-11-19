package de.swa.gmaf.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.swa.mmfg.MMFG;

/**
 * Detects shot changes in a video with ffmpeg and hands one still frame per
 * shot back to the framework, which analyses each frame with the same plugins
 * and attaches the results below a timecode node per shot.
 * Requires ffmpeg on the PATH.
 */
public class VideoShotDetection implements GMAF_Plugin {
	private static final Logger log = LoggerFactory.getLogger(VideoShotDetection.class);

	/** ffmpeg scene score (0..1) above which a frame counts as the start of a new shot */
	static final String SCENE_THRESHOLD = "0.4";

	private static final Pattern PTS_TIME = Pattern.compile("pts_time:\\s*([0-9.]+)");

	public boolean canProcess(String extension) {
		return FfmpegSupport.isVideo(extension);
	}

	/** nothing to do here: the frames are produced in prepareRecursiveProcessing */
	public void process(URL url, File f, byte[] bytes, MMFG fv) {
	}

	public boolean providesRecoursiveData() {
		return true;
	}

	/**
	 * @return one PNG per shot, named {@code shot_<start millis>.png}; the first
	 *         frame of the video always starts a shot. Null if none were found.
	 */
	public File[] prepareRecursiveProcessing(URL url, File f, byte[] bytes, MMFG fv) {
		try {
			File video = FfmpegSupport.videoFile(f, bytes);
			File dir = Files.createTempDirectory("gmaf_shots_").toFile();
			// showinfo logs pts_time for every selected frame, in output order.
			// -vsync vfr writes only the selected frames instead of repeating them.
			List<String> out = FfmpegSupport.run(Arrays.asList(
					"-hide_banner", "-i", video.getAbsolutePath(),
					"-vf", "select='eq(n,0)+gt(scene," + SCENE_THRESHOLD + ")',showinfo",
					"-vsync", "vfr",
					new File(dir, "frame_%04d.png").getAbsolutePath()));

			List<Long> startMillis = new ArrayList<Long>();
			for (String line : out) {
				if (!line.contains("Parsed_showinfo")) continue;
				Matcher m = PTS_TIME.matcher(line);
				if (m.find()) startMillis.add(Math.round(Double.parseDouble(m.group(1)) * 1000));
			}

			List<File> shots = new ArrayList<File>();
			for (int i = 0; i < startMillis.size(); i++) {
				File frame = new File(dir, String.format("frame_%04d.png", i + 1));
				if (frame.exists()) shots.add(FfmpegSupport.renameWithMillis(frame, "shot", startMillis.get(i)));
			}
			if (shots.isEmpty()) {
				dir.delete();
				return null;
			}
			log.info("VideoShotDetection: {} -> {} shots", f.getName(), shots.size());
			return shots.toArray(new File[0]);
		} catch (IOException | RuntimeException x) {
			log.warn("VideoShotDetection could not process {}: {}", f.getName(), x.getMessage());
			return null;
		}
	}

	public void mergeRecursiveResult(File subAsset, MMFG sub, MMFG parent) {
		FfmpegSupport.mergeUnderTimecode(subAsset, sub, parent);
		FfmpegSupport.deleteWithEmptyParent(subAsset);
	}

	public boolean isGeneralPlugin() {
		return false;
	}
}
