package de.zarncke.lib.struct;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.zarncke.lib.coll.Pair;

public class MapGraph<V, E> implements Graph<V, E>, Serializable {

	private static final class LinkedHashSetSupplier<E> implements Supplier<Collection<E>> {
		@Override
		public Collection<E> get() {
			return new LinkedHashSet<E>();
		}
	}

	private static final long serialVersionUID = 1L;

	private final Set<V> vertices = new HashSet<V>();
	private final Multimap<V, Pair<E, V>> edgesFrom = Multimaps.newMultimap(new HashMap<V, Collection<Pair<E, V>>>(),
			new LinkedHashSetSupplier<Pair<E, V>>());
	private final Multimap<V, Pair<V, E>> edgesTo = Multimaps.newMultimap(new HashMap<V, Collection<Pair<V, E>>>(),
			new LinkedHashSetSupplier<Pair<V, E>>());
	private final Multimap<Pair<V, V>, E> edges = Multimaps.newMultimap(new HashMap<Pair<V, V>, Collection<E>>(),
			new LinkedHashSetSupplier<E>());

	private Boolean acyclic = null;

	public MapGraph() {
	}

	// public MapGraph(MapGraph<E,V> graph) {
	// vertices = new HashSet<V>( graph.vertices);
	// ...
	// }

	@Override
	public Graph<V, E> add(final V vertex) {
		this.vertices.add(vertex);
		return this;
	}

	@Override
	public Graph<V, E> add(final V src, final E edge, final V dst) {
		this.vertices.add(src);
		this.vertices.add(dst);
		this.edgesFrom.put(src, Pair.pair(edge, dst));
		this.edgesTo.put(dst, Pair.pair(src, edge));
		this.edges.put(Pair.pair(src, dst), edge);
		this.acyclic = null;
		return this;
	}

	@Override
	public Collection<V> getVertices() {
		return Collections.unmodifiableCollection(this.vertices);
	}

	@Override
	public Collection<E> getEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}

	@Override
	public Collection<Pair<E, V>> getEdgesFrom(final V src) {
		return this.edgesFrom.get(src);
	}

	@Override
	public Collection<Pair<V, E>> getEdgesTo(final V dst) {
		return this.edgesTo.get(dst);
	}

	@Override
	public Collection<E> getEdges(final V src, final V dst) {
		return this.edges.get(Pair.pair(src, dst));
	}

	@Override
	public boolean isAscyclic() {
		if (this.acyclic == null) {
			determineAcyclic();
		}
		return this.acyclic.booleanValue();
	}

	private void determineAcyclic() {
		Set<V> seen = new HashSet<V>();
		if (this.vertices.isEmpty()) {
			this.acyclic = Boolean.TRUE;
			return;
		}
		V v = this.vertices.iterator().next();
		this.acyclic = Boolean.valueOf(determineAcyclic(v, seen));
	}

	private boolean determineAcyclic(final V v, final Set<V> seen) {
		if (seen.contains(v)) {
			return false;
		}
		seen.add(v);
		Set<V> neighbors = new HashSet<V>();
		for (Pair<E, V> pair : this.edgesFrom.get(v)) {
			neighbors.add(pair.getSecond());
		}
		for (V neighbor : neighbors) {
			if (!determineAcyclic(neighbor, seen)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void removeVertex(final V vertex) {
		this.vertices.remove(vertex);
		// remove all incident edges
		for (Pair<E, V> pair : this.edgesFrom.get(vertex)) {
			this.edges.remove(Pair.pair(vertex, pair.getSecond()), pair.getFirst());
		}
		for (Pair<V, E> pair : this.edgesTo.get(vertex)) {
			this.edges.remove(Pair.pair(pair.getFirst(), vertex), pair.getSecond());
		}

		this.edgesFrom.removeAll(vertex);
		this.edgesTo.removeAll(vertex);
		this.acyclic = null;
	}

	@Override
	public void removeEdge(final V source, final E edge, final V destination) {
		this.edges.remove(Pair.pair(source, destination), edge);
		this.edgesFrom.remove(source, Pair.pair(edge, destination));
		this.edgesTo.remove(destination, Pair.pair(source, edge));
		this.acyclic = null;
	}

	@Override
	public boolean containsVertex(final V vertex) {
		return this.vertices.contains(vertex);
	}

	@Override
	public <T> T traverse(final V start, final Traverser<V, E, T> traverser) {
		if (this.acyclic == Boolean.TRUE) {
			return traverseAcyclic(start, traverser);
		}
		Set<V> seen = new HashSet<V>();
		return traverse(start, traverser, seen);
	}

	private <T> T traverseAcyclic(final V v, final Traverser<V, E, T> traverser) {
		T r = traverser.depart(v);
		if (r != null) {
			return r;
		}
		Set<V> neighbors = new HashSet<V>();
		for (Pair<E, V> pair : this.edgesFrom.get(v)) {
			V neighbor = pair.getSecond();
			r = traverser.traverse(v, pair.getFirst(), neighbor);
			if (r != null) {
				return r;
			}
			if (neighbors.contains(neighbor)) {
				r = traverser.arrive(neighbor, true);
				continue;
			}
			neighbors.add(neighbor);
			r = traverser.arrive(neighbor, false);
			if (r != null) {
				return r;
			}
			r = traverse(neighbor, traverser);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	private <T> T traverse(final V v, final Traverser<V, E, T> traverser, final Set<V> seen) {
		T r = traverser.depart(v);
		if (r != null) {
			return r;
		}
		for (Pair<E, V> pair : this.edgesFrom.get(v)) {
			V target = pair.getSecond();
			r = traverser.traverse(v, pair.getFirst(), target);
			if (r != null) {
				return r;
			}
			if (seen.contains(target)) {
				r = traverser.arrive(target, true);
				if (r != null) {
					return r;
				}
				continue;
			}
			seen.add(v);
			r = traverser.arrive(target, false);
			if (r != null) {
				return r;
			}
			r = traverse(target, traverser, seen);
			if (r != null) {
				return r;
			}
		}
		return null;
	}

	@Override
	public int size() {
		return this.edges.size();
	}

	@Override
	public int numberOfVertices() {
		return this.vertices.size();
	}

	@Override
	public boolean isEmpty() {
		return this.edges.isEmpty() && this.vertices.isEmpty();
	}

	@Override
	public String toString() {
		return "Graph of " + this.vertices.toString();
	}

	@Override
	public Iterator<Join<V, E>> iterator() {
		return Iterators.transform(this.edges.entries().iterator(),
				new Function<Map.Entry<Pair<V, V>, E>, Join<V, E>>() {
					@Override
					public Join<V, E> apply(@Nullable final Entry<Pair<V, V>, E> from) {
						return new Join<V, E>() {
							@Override
							public V getSource() {
								return from.getKey().getFirst();
							}

							@Override
							public E getEdge() {
								return from.getValue();
							}

							@Override
							public V getDestination() {
								return from.getKey().getSecond();
							}
						};
					}
				});
	}
}
