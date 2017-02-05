/**
 *
 */
package de.zarncke.lib.index.crit;


import java.util.Collection;

import de.zarncke.lib.id.Id;
import de.zarncke.lib.id.Ids.HasIds;

/**
 * Matches a {@link Id}s of something which {@link HasIds#getIds() has ids}.
 *
 * @author Gunnar Zarncke
 * @param <T> type of element
 */
public final class IdMatch<T extends HasIds> extends CollectionCriteria<Id, T> {
	private static final long serialVersionUID = 1L;

	public IdMatch(final Collection<Id> requiredIds) {
		super(requiredIds, Id.class);
	}

	@Override
	protected Collection<? extends Id> getValues(final T entry) {
		return entry.getIds();
	}
}