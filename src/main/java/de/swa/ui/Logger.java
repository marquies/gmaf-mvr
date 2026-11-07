package de.swa.ui;

import java.util.Vector;
import org.slf4j.LoggerFactory;

public class Logger {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(Logger.class);

	private static Logger instance = null;
	public static Logger getInstance() {
		if (instance == null) instance = new Logger();
		return instance;
	}

	private Vector<LogListener> listeners = new Vector<LogListener>();
	public void addLogger(LogListener ll) {
		listeners.add(ll);
	}
	
	public void log(String msg) {
		log.info(msg);
		for (LogListener ll : listeners) ll.log(msg);
	}
}
