package de.zarncke.lib.index;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest;
import de.zarncke.lib.index.crit.Criteria;
import de.zarncke.lib.index.crit.ResolvedCriteria;
import de.zarncke.lib.index.crit.SingleCriteria;
import de.zarncke.lib.index.crit.StringCriteria;

/**
 * Test MultiIndex.
 *
 * @author Gunnar Zarncke
 */
public class MultiIndexTest extends GuardedTest {
	private static final class SortByA implements Comparator<Data> {
		@Override
		public int compare(final Data o1, final Data o2) {
			return o1.a.compareTo(o2.a);
		}
	}

	static class Data {
		String a;
		long v;

		public Data(final String a, final long v) {
			this.a = a;
			this.v = v;
		}

		@Override
		public String toString() {
			return this.a + "," + this.v;
		}
	}

	private static Collection<Criteria<?, Data>> none = L.e();
	private static StringCriteria<Data> eqA = new StringCriteria<Data>("a") {
		@Override
		public boolean matches(final Data entry) {
			return entry.a.equals(this.str);
		}
	};
	private static Criteria<Long, Data> eq2 = new SingleCriteria<Long, Data>(Long.valueOf(2), Long.class) {
		@Override
		public boolean matches(final Data entry) {
			return entry.v == 2;
		}
	};

	private final Data a1 = new Data("a", 1);
	private final Data b2 = new Data("b", 2);
	private final Data a3 = new Data("a", 3);
	private final Data b4 = new Data("b", 4);
	private final Data a2 = new Data("a", 2);

	private MultiIndex<Data> multiIndex1;
	private MultiIndex<Data> multiIndex2;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		this.multiIndex1 = createSimpleIndex();
		this.multiIndex2 = createSimpleIndex();
		// second index is on Long(v) and on String(a)
		this.multiIndex2.addIndex(Long.class, createLongIndex());
	}

	public KeyValueIndexing<Long, Data> createLongIndex() {
		return new KeyValueIndexing<Long, Data>(Long.class) {
			@Override
			public void add(final Data d) {
				add(Long.valueOf(d.v), d);
			}

			@Override
			protected Index<Data> createNewIndex(final Long key) {
				return new SortedIndex<Data>(new SortByA()) {
					@Override
					public Indexing<Data> getSubIndexing() {
						return new DefaultSubIndexing<Data>(String.class) {
							@Override
							public Index<Data> getIndex(final Criteria<?, Data> crit) {
								if (crit.getKeys() == null) {
									// this case tests null result handling
									return new Index<Data>() {
										@Override
										public Index<Data> add(final Data entry) {
											return null;
										}

										@Override
										public Results<Data> getAll() {
											return null;
										}

										@Override
										public Indexing<Data> getSubIndexing() {
											return null;
										}

										@Override
										public int size() {
											return 0;
										}

										@Override
										public void clear() {
											// nop
										}
									};
								}
								Data keyOb = new Data((String) crit.getKeys().iterator().next(), 0);
								return new ListIndex<Data>(L.l(getEntries().tailSet(keyOb).first()));
							}

							@Override
							public void clear() {
								// nop
							}
						};
					}
				};
			}
		};
	}

	private MultiIndex<Data> createSimpleIndex() {
		MultiIndex<Data> mi = new MultiIndex<Data>();

		// first index is on String(a)
		mi.addIndex(String.class, new KeyValueIndexing<String, Data>(String.class) {
			@Override
			public void add(final Data d) {
				add(d.a, d);
			}

			@Override
			protected Index<Data> createNewIndex(final String key) {
				return new ListIndex<Data>();
			}
		});

		mi.add(this.a1);
		mi.add(this.b2);
		mi.add(this.a3);
		return mi;
	}

	@Test
	public void testSimpleIndex() {
		Assert.assertEquals(3, this.multiIndex1.getTotalCandidates());
		Results<Data> res = this.multiIndex1.getMatches(none, 5);
		Assert.assertEquals(new HashSet<Data>(L.l(this.a1, this.b2, this.a3)), new HashSet<Data>(res.realize()));

		@SuppressWarnings("unchecked")
		List<? extends Criteria<?, Data>> findA = L.l(eqA);

		Assert.assertEquals(2, this.multiIndex1.estimateSize(findA));

		Results<Data> matchesA = this.multiIndex1.getMatches(findA, 5);
		Assert.assertEquals(2, matchesA.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.a1, this.a3)), new HashSet<Data>(matchesA.realize()));
	}

	@Test
	public void testIndex2() {
		@SuppressWarnings("unchecked")
		List<? extends Criteria<?, Data>> find2 = L.l(eq2);

		Results<Data> matches2 = this.multiIndex2.getMatches(find2, 5);
		Assert.assertEquals(1, matches2.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.b2)), new HashSet<Data>(matches2.realize()));

		this.multiIndex2.add(this.a2);

		// check finding by two criteria with nested index
		Results<Data> res4 = this.multiIndex2.getMatches(none, 5);
		Assert.assertEquals(new HashSet<Data>(L.l(this.a1, this.b2, this.a3, this.a2)), new HashSet<Data>(res4.realize()));

		@SuppressWarnings("unchecked")
		List<? extends Criteria<?, Data>> find2a = L.l(eq2, eqA);
		Results<Data> matches2a = this.multiIndex2.getMatches(find2a, 5);
		Assert.assertEquals(1, matches2a.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.a2)), new HashSet<Data>(matches2a.realize()));
	}

	@Test
	public void testIndexNull() {
		this.multiIndex2.add(this.a2);

		// find by two criteria with null case
		Criteria<Long, Data> eqNull = new Criteria<Long, Data>() {
			@Override
			public boolean matches(final Data entry) {
				return true;
			}

			@Override
			public Collection<Long> getKeys() {
				return null;
			}

			@Override
			public Class<Long> getType() {
				return Long.class;
			}
		};
		@SuppressWarnings("unchecked")
		List<? extends Criteria<?, Data>> find2Null = L.l(eq2, eqNull);
		Results<Data> matches2Null = this.multiIndex2.getMatches(find2Null, 5);
		Assert.assertEquals(2, matches2Null.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.a2, this.b2)), new HashSet<Data>(matches2Null.realize()));
	}

	@Test
	public void testIndexResolvedComplete() {
		Criteria<Long, Data> resolved = new ResolvedCriteria<Long, Data>() {
			@Override
			public boolean matches(final Data entry) {
				return getMatches(L.<Criteria<?, Data>> e(), Integer.MAX_VALUE).getMatches().contains(entry);
			}

			@Override
			public Class<Long> getType() {
				return Long.class;
			}

			@Override
			public Collection<Long> getKeys() {
				return null;
			}

			@Override
			public Results<Data> getMatches(final Collection<? extends Criteria<?, Data>> remainingCriteria,
					final int max) {
				return new Results<Data>(L.<Data> l(MultiIndexTest.this.a2, MultiIndexTest.this.b4), true);
			}

		};

		Results<Data> matchesRes = this.multiIndex2.getMatches(L.s(resolved), 2);
		Assert.assertEquals(2, matchesRes.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.a2, this.b4)), new HashSet<Data>(matchesRes.realize()));

		Criteria<Long, Data> eq4 = new SingleCriteria<Long, Data>(Long.valueOf(2), Long.class) {
			@Override
			public boolean matches(final Data entry) {
				return entry.v == 4;
			}
		};
		Results<Data> matchesRes2 = this.multiIndex2.getMatches(L.l(resolved, eq4), 2);
		Assert.assertEquals(1, matchesRes2.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.b4)), new HashSet<Data>(matchesRes2.realize()));
	}

	@Test
	public void testIndexResolvedOpen() {
		Criteria<Long, Data> resolved2 = new ResolvedCriteria<Long, Data>() {
			@Override
			public boolean matches(final Data entry) {
				return entry.v == 1 || entry.v == 4;
			}

			@Override
			public Class<Long> getType() {
				return Long.class;
			}

			@Override
			public Collection<Long> getKeys() {
				return null;
			}

			@Override
			public Results<Data> getMatches(final Collection<? extends Criteria<?, Data>> remainingCriteria,
					final int max) {
				return new Results<Data>(L.l(MultiIndexTest.this.b4), false);
			}
		};

		Results<Data> matchesRes3 = this.multiIndex2.getMatches(L.l(resolved2), 3);
		Assert.assertEquals(2, matchesRes3.size());
		Assert.assertEquals(new HashSet<Data>(L.l(this.a1, this.b4)), new HashSet<Data>(matchesRes3.realize()));
	}
}
