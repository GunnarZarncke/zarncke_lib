package de.zarncke.lib.sys;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import de.zarncke.lib.log.Log;

public interface LoggingInstallation {

	static class LogFile implements Serializable {
		private static final long serialVersionUID = 1L;
		private final File file;

		public LogFile(final File logFile) {
			this.file = logFile;
		}

		public DateTime getDateTime() {
			return new DateTime(this.file.lastModified(), DateTimeZone.UTC);
		}

		public long getSize() {
			return this.file.length();
		}

		public String getName() {
			return this.file.getName();
		}

		public InputStream getInputStream() throws IOException {
			return new FileInputStream(this.file);
		}

		public void delete() {
			if (!this.file.delete()) {
				Log.LOG.get().report("cannot delete " + file);
			}
		}

		@Override
		public String toString() {
			return this.file.toString();
		}
	}

	List<LogFile> getLogs();

}
