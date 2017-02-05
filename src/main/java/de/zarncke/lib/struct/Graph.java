package de.zarncke.lib.struct;

import java.util.Collection;

import de.zarncke.lib.coll.Pair;
import de.zarncke.lib.struct.Graph.Join;

/**
 * Generic interface for graphs.
 *
 * @author Gunnar Zarncke <gunnar@zarncke.de>
 * @param <V> vertex type
 * @param <E> edge type
 */
public interface Graph<V, E> extends Iterable<Join<V, E>> {
	interface Join<V, E> {
		V getSource();

		E getEdge();

		V getDestination();
	}

	/**
	 * Interface for traversing {@link Graph}s by the visitor pattern.
	 * Note that not all vertices (and thus edges) may be visited if the graph is split into unconnected parts.
	 *
	 * @param <V> vertex type
	 * @param <E> edge type
	 * @param <T> traversal result type (may be {@link Void})
	 */
	interface Traverser<V, E, T> {
		/**
		 * Called exactly once on each visited vertex.
		 * It is called before calling {@link #traverse} on outgoing edges.
		 * This method is called first on the starting node before any other method.
		 * 
		 * @param vertex where traversal departs from
		 * @return null to continue traversal, any other value stops traversal
		 */
		T depart(V vertex);

		/**
		 * Called on each edge.
		 * May be called multiple times with the same src and dst if multiple such edges exist.
		 * @param src source certex
		 * @param edge traversed edge
		 * @param dst  destination vertex
		 * @return null to continue traversal, any other value stops traversal
		 */
		T traverse(V src, E edge, V dst);

		/**
		 * Called on each vertex after {@link #traverse} has been called.
		 * May be called multiple times for each vertex, but only once with alreadySeen=false.
		 * Even on {@link Graph#isAscyclic() acyclic Graphs} may be called with alreadySeen=true namely when multiple
		 * edges exist between two vertices.
		 *
		 * @param vertex where traversal arrives at
		 * @param alreadySeen true if this vertex was arrived at earlier
		 * @return null to continue traversal, any other value stops traversal
		 */
		T arrive(V vertex, boolean alreadySeen);
	}

	/**
	 * Unconnected vertices are allowed.
	 * Vertices do not need to be added before use.
	 *
	 * @param vertex to add
	 * @return this
	 */
	Graph<V, E> add(V vertex);

	/**
	 * Adds the edge between the vertices. Adds the vertices if necessary.
	 *
	 * @param src
	 * @param edge
	 * @param dst
	 * @return this
	 */
	Graph<V, E> add(V src, E edge, V dst);

	/**
	 * Removes the vertex and all outgoing or incomming edges.
	 *
	 * @param vertex to remove, should exist
	 */
	void removeVertex(V vertex);

	/**
	 * Removes the edge. Incident vertices are unaffected.
	 *
	 * @param source vertex
	 * @param edge to remove, should exist
	 * @param destination vertex
	 */
	void removeEdge(V source, E edge, V destination);

	boolean containsVertex(V vertex);

	Collection<V> getVertices();

	Collection<E> getEdges();

	Collection<Pair<E, V>> getEdgesFrom(V src);

	Collection<Pair<V, E>> getEdgesTo(V dst);

	Collection<E> getEdges(V src, V dst);

	boolean isAscyclic();

	/**
	 * Traverses the graph
	 *
	 * @param start vertex to start traversal at
	 * @param visitor to call on each edge
	 * @return first non-null return of the visitor
	 */
	<T> T traverse(V start, Traverser<V, E, T> visitor);

	/**
	 * @return number of edges
	 */
	int size();

	/**
	 * @return number of vertices
	 */
	int numberOfVertices();

	/**
	 * @return true if no edges and no vertices
	 */
	boolean isEmpty();
}
