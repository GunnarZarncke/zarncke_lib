package de.zarncke.lib.struct;

import org.mockito.Mockito;

import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.log.Log;
import de.zarncke.lib.struct.Graph.Traverser;

public class GraphTest extends GuardedTest {
	static class E {
		String n;

		public E(final String n) {
			this.n = n;
		}

		@Override
		public String toString() {
			return this.n;
		}
	}

	static class V {
		String n;

		public V(final String n) {
			this.n = n;
		}

		@Override
		public String toString() {
			return this.n;
		}
	}

	static class Printer implements Traverser<V, E, Void> {

		@Override
		public Void depart(final V vertex) {
			Log.LOG.get().report(vertex + "->");
			return null;
		}

		@Override
		public Void traverse(final V src, final E edge, final V dst) {
			Log.LOG.get().report(src + "-[" + edge + "]-" + dst);
			return null;
		}

		@Override
		public Void arrive(final V vertex, final boolean alreadySeen) {
			Log.LOG.get().report("->" + vertex + (alreadySeen ? "(again)" : ""));
			return null;
		}

	}

	private static V a = new V("a");
	private static V b = new V("b");
	private static V c = new V("c");
	private static V d = new V("d");
	private static E s = new E("s");
	private static E t = new E("t");

	public void testMapGraph() {
		Graph<V,E> graph = new MapGraph<V,E>();

		assertTrue(graph.isEmpty());
		assertTrue(graph.isAscyclic());

		graph.add(a);
		assertFalse(graph.isEmpty());
		assertTrue(graph.isAscyclic());

		Traverser<V, E, Object> trav = Mockito.mock(Traverser.class);
		Mockito.when(trav.depart(a)).thenReturn(null);

		graph.traverse(a, trav);
		Mockito.verify(trav).depart(a);
		Mockito.verifyNoMoreInteractions(trav);

		graph.add(a, s, b);

		trav = Mockito.mock(Traverser.class);
		Mockito.when(trav.depart(a)).thenReturn(null);
		Mockito.when(trav.traverse(a, s, b)).thenReturn(null);
		Mockito.when(trav.arrive(b, false)).thenReturn(null);
		Mockito.when(trav.depart(b)).thenReturn(null);
		graph.traverse(a, new Printer());
		graph.traverse(a, trav);
		Mockito.verify(trav).depart(a);
		Mockito.verify(trav).traverse(a, s, b);
		Mockito.verify(trav).arrive(b, false);
		Mockito.verify(trav).depart(b);
		Mockito.verifyNoMoreInteractions(trav);

		graph.add(a, s, b).add(b, t, c);
		assertFalse(graph.isEmpty());
		assertTrue(graph.isAscyclic());

		trav = Mockito.mock(Traverser.class);
		Mockito.when(trav.depart(a)).thenReturn(null);
		Mockito.when(trav.traverse(a, s, b)).thenReturn(null);
		Mockito.when(trav.arrive(b, false)).thenReturn(null);
		Mockito.when(trav.depart(b)).thenReturn(null);
		Mockito.when(trav.traverse(b, t, c)).thenReturn(null);
		Mockito.when(trav.arrive(c, false)).thenReturn(null);
		Mockito.when(trav.depart(c)).thenReturn(null);
		graph.traverse(a, new Printer());
		graph.traverse(a, trav);
		Mockito.verify(trav).depart(a);
		Mockito.verify(trav).traverse(a, s, b);
		Mockito.verify(trav).arrive(b, false);
		Mockito.verify(trav).depart(b);
		Mockito.verify(trav).traverse(b, t, c);
		Mockito.verify(trav).arrive(c, false);
		Mockito.verify(trav).depart(c);
		Mockito.verifyNoMoreInteractions(trav);

		graph.add(c, s, a);
		assertFalse(graph.isAscyclic());

		trav = Mockito.mock(Traverser.class);
		Mockito.when(trav.depart(a)).thenReturn(null);
		Mockito.when(trav.traverse(a, s, b)).thenReturn(null);
		Mockito.when(trav.arrive(b, false)).thenReturn(null);
		Mockito.when(trav.depart(b)).thenReturn(null);
		Mockito.when(trav.traverse(b, t, c)).thenReturn(null);
		Mockito.when(trav.arrive(c, false)).thenReturn(null);
		Mockito.when(trav.depart(c)).thenReturn(null);
		Mockito.when(trav.traverse(c, s, a)).thenReturn(null);
		Mockito.when(trav.arrive(a, true)).thenReturn(null);

		graph.traverse(a, trav);
		Mockito.verify(trav).depart(a);
		Mockito.verify(trav).traverse(a, s, b);
		Mockito.verify(trav).arrive(b, false);
		Mockito.verify(trav).depart(b);
		Mockito.verify(trav).traverse(b, t, c);
		Mockito.verify(trav).arrive(c, false);
		Mockito.verify(trav).depart(c);
		Mockito.verify(trav).traverse(c, s, a);
		Mockito.verify(trav).arrive(a, true);
		Mockito.verifyNoMoreInteractions(trav);
	}
}
