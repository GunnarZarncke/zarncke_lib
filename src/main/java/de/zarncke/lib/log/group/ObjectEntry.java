package de.zarncke.lib.log.group;

import de.zarncke.lib.coll.Elements;
import de.zarncke.lib.sys.Health;
import de.zarncke.lib.util.Chars;

/**
 * An entry for debug output.
 */
class ObjectEntry extends Entry {
	private static final int DEBUG_MESSAGE_LIMIT = 100;
	private static final long serialVersionUID = 1L;
	private Object object;

	public ObjectEntry(final LogCaller caller, final Object object) {
		super(caller);
		this.object = object;
	}

	@Override
	public void addTo(final StringBuilder sb) {
		super.addTo(sb);
		sb.append("debugged ");
		sb.append(Elements.toString(this.object));
		// TODO consider improved formatting
	}

	@Override
	public void addToShort(final StringBuilder sb) {
		// leave out debug info altogether
	}

	@Override
	public CharSequence getShortMessage() {
		return "debug " + Chars.summarize(Elements.toString(this.object), DEBUG_MESSAGE_LIMIT);
	}

	@Override
	public Health getEstimatedSeverity() {
		return Health.MINOR;
	}

	@Override
	void truncate() {
		super.truncate();
		this.object = getShortMessage();
	}
}