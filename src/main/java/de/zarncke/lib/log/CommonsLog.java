package de.zarncke.lib.log;

import org.apache.commons.logging.LogFactory;

import de.zarncke.lib.coll.Elements;

/**
 * Adapter which forwards all our logging to Commons logging.
 *
 * @author Gunnar Zarncke
 */
public class CommonsLog implements Log {

	// TODO could be improved, see Slf4jLog
	private static final org.apache.commons.logging.Log LOG = LogFactory.getLog(CommonsLog.class);

	@Override
	public void report(final Throwable throwableToReport) {
		LOG.error("", throwableToReport);
	}

	@Override
	public void report(final CharSequence issue) {
		LOG.info(issue);
	}

	@Override
	public void report(final Object... debugObject) {
		LOG.debug(Elements.toString(debugObject));
	}

}
