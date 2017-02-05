package de.zarncke.lib.sys.app;

import java.io.IOException;
import java.util.List;

import de.zarncke.lib.err.Warden;
import de.zarncke.lib.io.store.Store;
import de.zarncke.lib.io.store.StoreUtil;
import de.zarncke.lib.sys.BinarySupplier;
import de.zarncke.lib.util.Chars;
import de.zarncke.lib.util.Version;

/**
 * Can be used as a delegate in a main {@link de.zarncke.lib.sys.module.Module}. to implement {@link BinarySupplier}.
 *
 * @author Gunnar Zarncke
 */
public class ApplicationInfo implements BinarySupplier {

	private final String historyDirectory;
	private final String appName;

	public ApplicationInfo(final String historyDirectory, final String appName) {
		this.historyDirectory = historyDirectory;
		this.appName = appName;
	}

	@Override
	public Store getBinaryInformation(final Object key) {
		if ("application_version".equals(key)) {
			String msg = "1.0.0";
			final Version v = Version.CTX.get();
			if (v != null) {
				msg = v.getVersion() + "." + v.getRevision();
			}
			return StoreUtil.asStore(msg, null);
		}
		if ("version_details".equals(key)) {
			String msg = "1.0.0";
			final Version v = Version.CTX.get();
			if (v != null) {
				msg = v.toDetailedInfo();
				return StoreUtil.asStore(msg, null);}
		}
		if ("application_history".equals(key)) {
			try {
				final List<String> history = ApplicationHistory.getHistoryLinesReverse(this.historyDirectory);
				return StoreUtil.asStore(Chars.join(history, "\n"), null);
			} catch (IOException e) {
				Warden.disregardAndReport(e);
				return null;
			}
		}
		return null;
	}

	public void reportStart() {
		ApplicationHistory.reportStart(this.appName, this.historyDirectory);
	}

	public void reportStop() {
		ApplicationHistory.reportStop(this.appName, this.historyDirectory);
	}
}
