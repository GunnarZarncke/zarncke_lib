/**
 *
 */
package de.zarncke.lib.value;

import de.zarncke.lib.ctx.Context;

public interface TypeNameMapping{
	public static final Context<TypeNameMapping> CTX = Context.of(Default.of(new BiTypeMapping(), TypeNameMapping.class));

	String getNameForType(Class<?> type);

	Class<?> getTypeForName(String name);

}