package de.zarncke.lib.math;

import java.util.List;

import org.junit.Test;

import de.zarncke.lib.coll.L;
import de.zarncke.lib.err.GuardedTest;

/**
 * Test {@link SetTheory}.
 *
 * @author Gunnar Zarncke
 */
public class SetTheoryTest extends GuardedTest {
	@Test
	public void testCartesianEmpty() {
		assertContentEquals(L.l(L.<Object> e()), SetTheory.expandToCartesianProduct(L.<List<Object>> e()));

		assertContentEquals(L.e(), SetTheory.expandToCartesianProduct(L.s(L.e())));

		assertContentEquals(L.l(), SetTheory.expandToCartesianProduct(L.l(L.l("1"), L.<String> e(), L.l("a", "b"))));
	}

	@Test
	public void testCartesianSimple() {

		assertContentEquals(L.s(L.s("1")), SetTheory.expandToCartesianProduct(L.s(L.s("1"))));

		assertContentEquals(L.l(L.l("1"), L.l("2")), SetTheory.expandToCartesianProduct(L.s(L.l("1", "2"))));

		assertContentEquals(L.s(L.l("1", "2")), SetTheory.expandToCartesianProduct(L.l(L.l("1"), L.l("2"))));
	}

	@Test
	public void testCartesianMultiple() {

		assertContentEquals(L.l(L.l("1", "a"), L.l("2", "a"), L.l("1", "b"), L.l("2", "b")),
				SetTheory.expandToCartesianProduct(L.l(L.l("1", "2"), L.l("a", "b"))));

		assertContentEquals(L.l(L.l("1", "2", "a"), L.l("1", "2", "b")),
				SetTheory.expandToCartesianProduct(L.l(L.l("1"), L.l("2"), L.l("a", "b"))));
	}

	@Test
	public void testAbsorbing() {
		assertContentEquals(L.e(), SetTheory.absorbSuperSets(L.<List<Object>> e()));
		assertContentEquals(L.l(L.<Object> l()), SetTheory.absorbSuperSets(L.<List<Object>> s(L.e())));

		assertContentEquals(L.l(L.<String> set()), SetTheory.absorbSuperSets(L.l(L.<String> e(), L.l("a", "b"))));

		assertContentEquals(L.l(L.set("a", "b"), L.set("a", "c"), L.set("d")),
				SetTheory.absorbSuperSets(L.l(L.l("a", "b", "c"), L.l("a", "b"), L.l("a", "c"), L.l("b", "d"), L.l("d"))));
	}
}
