package de.swa.gmaf.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.swa.mmfg.MMFG;

/**
 * Splits a video into segments of {@value #SEGMENT_SECONDS} seconds with ffmpeg
 * and hands them back to the framework, which analyses each segment with the
 * same plugins and attaches the results below a timecode node per segment.
 * Requires ffmpeg on the PATH.
 */
public class VideoSplitter implements GMAF_Plugin {
	private static final Logger log = LoggerFactory.getLogger(VideoSplitter.class);

	static final int SEGMENT_SECONDS = 10;

	public boolean canProcess(String extension) {
		return FfmpegSupport.isVideo(extension);
	}

	/** nothing to do here: the segments are produced in prepareRecursiveProcessing */
	public void process(URL url, File f, byte[] bytes, MMFG fv) {
	}

	public boolean providesRecoursiveData() {
		return true;
	}

	/**
	 * @return the segments, named {@code segment_<start millis>.mp4}, or null if
	 *         the video is shorter than two segments
	 */
	public File[] prepareRecursiveProcessing(URL url, File f, byte[] bytes, MMFG fv) {
		try {
			File video = FfmpegSupport.videoFile(f, bytes);
			File dir = Files.createTempDirectory("gmaf_segments_").toFile();
			File list = new File(dir, "segments.csv");
			// Re-encode with a keyframe every segment: with -c copy the segmenter can
			// only cut at existing keyframes, which may be far apart.
			FfmpegSupport.run(Arrays.asList(
					"-v", "error", "-i", video.getAbsolutePath(),
					"-f", "segment", "-segment_time", "" + SEGMENT_SECONDS,
					"-force_key_frames", "expr:gte(t,n_forced*" + SEGMENT_SECONDS + ")",
					"-reset_timestamps", "1",
					"-segment_list", list.getAbsolutePath(), "-segment_list_type", "csv",
					new File(dir, "segment_%04d.mp4").getAbsolutePath()));

			// segments.csv: <file>,<start seconds>,<end seconds>
			List<File> segments = new ArrayList<File>();
			double durationSeconds = 0;
			for (String line : Files.readAllLines(list.toPath(), StandardCharsets.UTF_8)) {
				String[] cols = line.split(",");
				if (cols.length < 3) continue;
				long startMillis = Math.round(Double.parseDouble(cols[1]) * 1000);
				durationSeconds = Math.max(durationSeconds, Double.parseDouble(cols[2]));
				segments.add(FfmpegSupport.renameWithMillis(new File(dir, cols[0]), "segment", startMillis));
			}
			list.delete();
			// The segments are processed by this chain again; a segment is shorter
			// than two segment lengths, so this also stops the recursion there
			// (a 10.2 s segment would otherwise be split into 10 s + 0.2 s).
			if (durationSeconds < 2 * SEGMENT_SECONDS) {
				for (File s : segments) s.delete();
				dir.delete();
				return null;
			}
			log.info("VideoSplitter: {} -> {} segments", f.getName(), segments.size());
			return segments.toArray(new File[0]);
		} catch (IOException | RuntimeException x) {
			log.warn("VideoSplitter could not split {}: {}", f.getName(), x.getMessage());
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
