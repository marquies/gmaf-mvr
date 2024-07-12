package de.swa;

import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.util.Date;

/**
 * writes the GMAF log to a CSV file
 **/
public class CSVLogWriter {
	private static String folder = "sigir/log";
	private static CSVLogWriter instance;
	private RandomAccessFile logFile;

	private CSVLogWriter() {
		try {
			Date d = new Date();
			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
			String name = df.format(d) + "_" + System.currentTimeMillis() + ".csv";
			logFile = new RandomAccessFile(folder + "/" + name, "rw");
			logFile.writeBytes("Date ; Time ; File ; Nodes ; MMFVG ; GraphML ; GC ; Total ; Message\r\n");
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static CSVLogWriter getInstance() {
		if (instance == null) instance = new CSVLogWriter();
		return instance;
	}

	public void log(String file, int nodes, long mmfvgDuration, long graphMLDuration, long gcDuration, long total, String message) {
		Date d = new Date();
		DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
		DateFormat df2 = DateFormat.getTimeInstance(DateFormat.LONG);

		String logText = df.format(d) + " ; " + df2.format(d) + " ; " + file + " ; " + nodes + " ; " + mmfvgDuration + " ; " + graphMLDuration + " ; " + gcDuration + " ; " + total + " ; " + message;

		try {
			logFile.writeBytes(logText + "\r\n");
			System.out.println(logText);
		} catch (Exception x) {
			System.out.println("Exception in Logger " + x);
		}
	}
}
