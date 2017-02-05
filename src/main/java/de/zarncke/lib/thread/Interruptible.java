package de.zarncke.lib.thread;

/**
 * This marker interface indicates that a certain Thread supports generic interruption.
 * This is intended to allow generic monitoring frameworks to interrupt processing of such threads
 * without harming threads not cleanly supporting this feature.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 */
public interface Interruptible {
	// roughly from least to fullest support
	enum Support {
		UNSUPPORTED, // not actually interruptible (e.g. in decoration cases)
		LOOP_STOPS, // the thread runs repeatedly and just stops (which may be unwanted)
		SINGLE_JOB_STOPS, // the thread runs a single job which stops (annd will not be recreated automatically)
		BEST_EFFORT, // no specific reaction (in generic cases, should be avoided)
		AUTO_JOB_STOPS, // the thread runs a single task and stop, but the thread/task will be recreated automatically
		LOOP_CONTINUE, // the thread runs repeatedly and continues with the next task
		LOOP_RETRY, // the thread runs repeatedly and will retry the interrupted task
		RECOVERING // the thread will (try to) recover completely (generic declaration of full support)
	}

	Support getSupport();
}
