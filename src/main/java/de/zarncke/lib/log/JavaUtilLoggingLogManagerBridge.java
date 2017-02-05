package de.zarncke.lib.log;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JavaUtilLoggingLogManagerBridge extends LogManager {
	@Override
	public synchronized Logger getLogger(final String name) {
		// super.getLogger(name);
		Logger logger = new Logger(name, null) {
			@Override
			public void log(final LogRecord record) {
				if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
					Log.LOG.get().report(record.getMessage());
				} else if (record.getLevel().intValue() >= Level.CONFIG.intValue()) {
					Log.LOG.get().report(record.getMessage());
				} else if (record.getLevel().intValue() >= Level.FINE.intValue()) {
					Log.LOG.get().report(record.getMessage(), record.getParameters());
				} // finer and finest are ignored
				if (record.getThrown() != null) {
					Log.LOG.get().report(record.getThrown());
				}
			}
		};
		return logger;
	}
}