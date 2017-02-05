package de.zarncke.lib.coll;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.zarncke.lib.id.Gid;
import de.zarncke.lib.id.Ids.HasGid;
import de.zarncke.lib.id.ResolvedChoiceXmlAdapter;
import de.zarncke.lib.id.Resolving;

/**
 * A {@link MultipleChoice} using an internal Collection of Ids.
 *
 * @author Gunnar Zarncke
 * @param <T> of elements
 */
@XmlJavaTypeAdapter(ResolvedChoiceXmlAdapter.class)
public class ResolvedChoice<T extends HasGid<T>> extends AbstractChoice<T> implements Serializable {
	private static final int INITIAL_CAPACITY = 3;
	private static final long serialVersionUID = 1L;
	private Set<Gid<T>> list;
	private transient Set<T> results = null;

	public ResolvedChoice() {
		this.list = null;
	}

	public ResolvedChoice(final ResolvedChoice<T> resolvedChoice) {
		this.list = resolvedChoice.list == null ? null : new LinkedHashSet<Gid<T>>(resolvedChoice.list);
		this.results = resolvedChoice.results;
	}

	@Override
	public Collection<T> getAll() {
		if (this.list == null) {
			return null;
		}
		if (this.results == null) {
			this.results = new LinkedHashSet<T>(Resolving.resolve(this.list, true));
		}
		return this.results;
	}

	@Override
	public void setAll(final Collection<T> newList) {
		if (newList == null) {
			this.results = null;
			this.list = null;
		} else {
			this.results = new LinkedHashSet<T>(newList);
			this.list = new LinkedHashSet<Gid<T>>(Resolving.toPreciseIds(newList));
		}
	}

	@Override
	public void set(final T value) {
		this.results = null;
		this.list = new LinkedHashSet<Gid<T>>(INITIAL_CAPACITY);
		this.list.add(value.getId());
	}

	@Override
	public void add(final T value) {
		if (this.results != null) {
			this.results.add(value);
		}
		if (this.list == null) {
			if (this.results == null) {
				this.results = new LinkedHashSet<T>(INITIAL_CAPACITY);
				this.results.add(value);
			}
			this.list = new LinkedHashSet<Gid<T>>(INITIAL_CAPACITY);
		}
		this.list.add(value.getId());
	}

	@Override
	public void remove(final T value) {
		this.results = null;
		if (this.list != null) {
			this.list.remove(value.getId());
		}
	}

	@Override
	public void toggle(final T value) {
		if (this.list == null) {
			set(value);
		} else if (this.list.contains(value.getId())) {
			remove(value);
		} else {
			add(value);
		}
	}


	@Override
	public boolean isDefined() {
		return this.list != null;
	}

	@Override
	public void clear() {
		this.list = null;
		this.results = null;
	}

	@Override
	public MultipleChoice<T> copy() {
		return new ResolvedChoice<T>(this);
	}

	@Override
	public int hashCode() {
		return this.list == null ? 0 : this.list.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ResolvedChoice<?> other = (ResolvedChoice<?>) obj;
		if (this.list == null) {
			if (other.list != null) {
				return false;
			}
		} else if (!this.list.equals(other.list)) {
			return false;
		}
		return true;
	}


}