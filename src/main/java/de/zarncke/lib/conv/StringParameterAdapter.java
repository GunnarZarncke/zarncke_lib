package de.zarncke.lib.conv;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import de.zarncke.lib.conv.AbstractConvertible.FailMode;

public class StringParameterAdapter implements Function<String[], Convertible> {

	private final class StringParameterConvertible extends AbstractConvertible {
		private StringParameterConvertible(final FailMode failMode) {
			super(failMode);
		}

		@Override
		protected AbstractConvertible create(final AbstractConvertible abstractConvertible, final FailMode failMode) {
			return new StringParameterConvertible(failMode);
		}
	}

	private static final StringParameterAdapter INSTANCE = new StringParameterAdapter();

	protected StringParameterAdapter() {
		// for derived classes
	}

	public Convertible apply(final String[] from) {
		return new StringParameterConvertible(FailMode.FAIL);
	}

	public static Map<String, Convertible> makeConvertible(final Map<String, String[]> parameterMap) {
		return Maps.transformValues(parameterMap, StringParameterAdapter.getInstance());
	}

	private static StringParameterAdapter getInstance() {
		return INSTANCE;
	}

}
