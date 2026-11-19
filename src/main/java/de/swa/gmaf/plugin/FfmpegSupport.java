package de.swa.gmaf.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.swa.mmfg.MMFG;
import de.swa.mmfg.Node;

/**
 * Shared helpers for the ffmpeg based video plugins ({@link VideoSplitter},
 * {@link VideoShotDetection}). Stateless, so safe for the plugin instances
 * that all requests share.
 *
 * Sub-assets carry their start time in their file name ({@code <prefix>_<millis>.<ext>}),
 * so {@link #mergeUnderTimecode} needs no state between
 * prepareRecursiveProcessing and mergeRecursiveResult.
 */
final class FfmpegSupport {
	private FfmpegSupport() {}

	private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
			"mpg", "mpeg", "mov", "flv", "mp4", "mxf", "qt", "m4v");

	/** generous upper bound, so a hanging ffmpeg cannot block a request forever */
	static final long TIMEOUT_MINUTES = 30;

	private static final Pattern MILLIS_IN_NAME = Pattern.compile("_(\\d+)\\.[^.]+$");

	/** true for video extensions, with or without the leading dot the chain passes */
	static boolean isVideo(String extension) {
		if (extension == null) return false;
		String e = extension.startsWith(".") ? extension.substring(1) : extension;
		return VIDEO_EXTENSIONS.contains(e.toLowerCase());
	}

	/** the asset as a file on disk: f if it exists, otherwise bytes written to a temp file */
	static File videoFile(File f, byte[] bytes) throws IOException {
		if (f != null && f.exists()) return f;
		if (bytes == null || bytes.length == 0) throw new IOException("no video file and no bytes");
		File tmp = File.createTempFile("gmaf_video_", ".mp4");
		Files.write(tmp.toPath(), bytes);
		return tmp;
	}

	/**
	 * Runs ffmpeg and waits for it. Its output (stderr merged into stdout) is
	 * read completely, both to return it and so a full pipe buffer cannot
	 * block the process.
	 *
	 * @return the output lines
	 * @throws IOException if ffmpeg cannot be started, times out or exits non-zero
	 */
	static List<String> run(List<String> args) throws IOException {
		List<String> cmd = new ArrayList<String>();
		cmd.add("ffmpeg");
		cmd.add("-nostdin");
		cmd.addAll(args);
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
		List<String> out = new ArrayList<String>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = r.readLine()) != null) out.add(line);
		}
		try {
			if (!p.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
				p.destroyForcibly();
				throw new IOException("ffmpeg timed out: " + cmd);
			}
		} catch (InterruptedException x) {
			p.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new IOException("interrupted while waiting for ffmpeg", x);
		}
		if (p.exitValue() != 0) {
			int from = Math.max(0, out.size() - 5);
			throw new IOException("ffmpeg exited with " + p.exitValue() + ": " + out.subList(from, out.size()));
		}
		return out;
	}

	/** renames f to {@code <prefix>_<millis>.<ext>} in the same folder */
	static File renameWithMillis(File f, String prefix, long millis) throws IOException {
		String name = f.getName();
		String ext = name.substring(name.lastIndexOf('.'));
		File target = new File(f.getParentFile(), prefix + "_" + millis + ext);
		return Files.move(f.toPath(), target.toPath()).toFile();
	}

	/** deletes a sub-asset that has been merged, and its folder once that is empty */
	static void deleteWithEmptyParent(File subAsset) {
		File dir = subAsset.getParentFile();
		subAsset.delete();
		String[] left = dir == null ? null : dir.list();
		if (left != null && left.length == 0) dir.delete();
	}

	/** formats the start time in a sub-asset's name as HH:MM:SS.mmm, or null if it has none */
	static String timecode(File subAsset) {
		Matcher m = MILLIS_IN_NAME.matcher(subAsset.getName());
		if (!m.find()) return null;
		long ms = Long.parseLong(m.group(1));
		return String.format("%02d:%02d:%02d.%03d", ms / 3_600_000, ms / 60_000 % 60, ms / 1000 % 60, ms % 1000);
	}

	/**
	 * Attaches the sub-asset's results below a node named by its timecode,
	 * which hangs below the parent's current node:
	 * {@code Root-Asset -> 00:00:10.200 -> <nodes of that segment>}.
	 * Unlike the default merge, the sub-asset's nodes are also added to the
	 * parent's node list, which the Graph Code is computed from.
	 */
	static void mergeUnderTimecode(File subAsset, MMFG sub, MMFG parent) {
		String tc = timecode(subAsset);
		Node tcNode = new Node(tc != null ? tc : subAsset.getName(), parent);
		List<Node> subRoots = new ArrayList<Node>();
		for (Node n : sub.getNodes()) {
			if (n.getName().equals("Root-Asset")) {
				subRoots.add(n);
				for (Node c : n.getChildNodes()) tcNode.addChildNode(c);
			} else {
				tcNode.addChildNode(n);
			}
		}
		for (Node n : sub.getAllNodes()) {
			if (!subRoots.contains(n) && !parent.allNodes.contains(n)) parent.allNodes.add(n);
		}
		Node root = parent.getCurrentNode();
		if (root != null) root.addChildNode(tcNode);
		else parent.addNode(tcNode);
	}
}
