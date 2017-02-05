package de.zarncke.lib.coll;

import com.google.common.collect.Collections2;

/**
 * @deprecated see {@link Collections2#transform(java.util.Collection, com.google.common.base.Function)}.
 */
@Deprecated
public interface Transform<S, D>
{
	Transform<Object, String> TO_STRING = new Transform<Object, String>() {

		@Override
		public String transform(final Object src) {
			return src == null ? null : src.toString();
		}
	};

	D transform(S src);
}
