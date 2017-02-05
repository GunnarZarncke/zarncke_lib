package de.zarncke.lib.log;

/**
 * A Report which provides a shorter summary.
 * 
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface ExcerptableReport extends Report {
	CharSequence getExcerpt();
}
