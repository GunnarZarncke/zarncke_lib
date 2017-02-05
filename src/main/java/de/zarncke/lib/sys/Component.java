package de.zarncke.lib.sys;

import de.zarncke.lib.i18n.Translations;

/**
 * A Component is a part of an application.
 *
 * @author Gunnar Zarncke
 */
public interface Component {
	public static enum State {
		UP, GOING_UP, GOING_DOWN, RECOVERING, UNINITIALIZED, DOWN, STUCK, UNDEFINED;
		public static State aggregateStatus(final State s1, final State s2) {
			if (s1 == null || s1 == State.UNDEFINED) {
				return s2;
			}
			if (s2 == null || s2 == State.UNDEFINED) {
				return s1;
			}
			if (s1 == s2) { // NOPMD enum
				return s1;
			}
			if (s1 == State.STUCK || s2 == State.STUCK) {
				return State.STUCK;
			}
			return combineLinearStates(s1, s2);
		}

		private static State combineLinearStates(final State s1, final State s2) {
			switch (s1) {
			case UP:
				return combineUp(s2);
			case GOING_UP:
				return combineGoingUp(s2);
			case GOING_DOWN:
				return combineGoingDown(s2);
			case DOWN:
				return combineDown(s2);
			case RECOVERING:
				return combineRecovering(s2);
			default:
				return State.UNDEFINED;
			}
		}

		private static State combineRecovering(final State s2) {
			switch (s2) {
			case UP:
				return State.RECOVERING;
			case GOING_DOWN:
			case DOWN:
				return State.GOING_DOWN;
			default:
				return State.UNDEFINED;
			}
		}

		private static State combineDown(final State s2) {
			switch (s2) {
			case RECOVERING:
			case GOING_DOWN:
				return State.GOING_DOWN;
			default:
				return State.UNDEFINED;
			}
		}

		private static State combineGoingDown(final State s2) {
			switch (s2) {
			case RECOVERING:
			case DOWN:
				return State.GOING_DOWN;
			default:
				return State.UNDEFINED;
			}
		}

		private static State combineGoingUp(final State s2) {
			switch (s2) {
			case RECOVERING:
				return State.RECOVERING;
			case UNINITIALIZED:
				return State.GOING_UP;
			case UP:
				return State.GOING_UP;
			default:
				return State.UNDEFINED;
			}
		}

		private static State combineUp(final State s2) {
			switch (s2) {
			case RECOVERING:
				return State.RECOVERING;
			case UNINITIALIZED:
			case GOING_UP:
				return State.GOING_UP;
			case GOING_DOWN:
			case DOWN:
				return State.GOING_DOWN;
			default:
				return State.UNDEFINED;
			}
		}
	}

	Translations getName();

	State getState();

	Health getHealth();

	/**
	 * @return value representing load 0.0 means no load, 1.0 means full load; values >1.0 are allowed, NaN means not
	 * available
	 */
	double getLoad();

	/**
	 * @return any human readable text the
	 */
	String getMetaInformation();
}
