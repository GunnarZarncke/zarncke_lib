package de.zarncke.lib.sys.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.joda.time.format.DateTimeFormat;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.ExceptionUtil;
import de.zarncke.lib.io.IOTools;
import de.zarncke.lib.io.LineConsumer;
import de.zarncke.lib.io.store.FileStore;
import de.zarncke.lib.time.Times;
import de.zarncke.lib.util.Misc;
import de.zarncke.lib.util.Version;

/**
 * Tool for keeping a history of important changes to an application during its life cycle.
 * 
 * @author Gunnar Zarncke
 */
public final class ApplicationHistory {
	public static final String FILE_APPLICATION_HISTORY = "application_history";

	public static void reportStart(final String applicationName, final String applicationDirectory) {
		reportChange("Started", applicationName, applicationDirectory);
	}

	public static void reportStop(final String applicationName, final String applicationDirectory) {
		reportChange("Stopped", applicationName, applicationDirectory);
	}

	public static void reportChange(final String change, final String applicationName, final String applicationDirectory) {
		try {
			Version v = Version.CTX.get();
			String version = v == null ? "<unknown version>" : v.getVersion() + "." + v.getRevision();
			String msg = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").print(Times.now()) + " " + change + " "
					+ applicationName + " " + version + "\n";
			IOTools.dump(msg.getBytes(Misc.UTF_8), new FileOutputStream(new File(new File(applicationDirectory),
					FILE_APPLICATION_HISTORY), true));
		} catch (Exception e) {
			ExceptionUtil.emergencyAlert("Cannot report Stop of EPG Client", e);
		}
	}

	public static List<String> getHistoryLinesReverse(final String applicationDirectory) throws IOException {
		final List<String> history = L.l();
		new LineConsumer<List<String>>() {
			@Override
			protected void consume(final String line) {
				history.add(line);
			}
		}.consume(new FileStore(new File(new File(applicationDirectory), FILE_APPLICATION_HISTORY)));
		Collections.reverse(history);
		return history;
	}
}
