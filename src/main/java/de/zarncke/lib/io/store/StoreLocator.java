package de.zarncke.lib.io.store;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import de.zarncke.lib.ctx.Context;
import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.value.Default;

/**
 * Establishes a mapping between URLs and corresponding {@link Store Stores}.
 * Note: If a mapped Store changes (with respect to equals) then it will no longer appear to be mapped.
 *
 * @author Gunnar Zarncke
 */
public class StoreLocator {
	/**
	 * A context for a {@link StoreLocator}. <br/>
	 * Defaults to a locator which only knows the currently active {@link Store#ROOT root Store}.
	 */
	public static final Context<StoreLocator> CTX = Context.of(Default.of(new StoreLocator() {
		{
			register(new DelegateStore(null) {
				@Override
				protected Store getDelegate() {
					return Store.ROOT.get();
				}
			}, URI.create("local:///"));
		}
	}, StoreLocator.class));

	// the key uri contains a trailing "/" in the path
	private final Map<String, Store> storesByUri = new MapMaker().makeMap();
	private final Multimap<Store, String> storeToUris = Multimaps.synchronizedListMultimap(LinkedListMultimap.<Store, String> create());

	/**
	 * Tries to locate a Store from a previously registered URI.
	 *
	 * @param uri != null
	 * @return null if not resolvable, Store otherwise (may be non-{@link Store#exists() existent})
	 */
	public Store resolve(final URI uri) {
		URI triedUri = ensureTrailingSlash(uri);
		StringBuilder relPath = new StringBuilder();
		Store store;
		while (true) {
			store = this.storesByUri.get(triedUri.toASCIIString());
			if (store != null) {
				break;
			}
			String uriPath = triedUri.getPath();
			int slashP = uriPath.lastIndexOf("/", uriPath.length() - 2);
			if (slashP < 0) {
				return null;
			}
			if (relPath.length() > 0) {
				relPath.insert(0, "/");
			}
			relPath.insert(0, uriPath.substring(slashP + 1));

			triedUri = triedUri.resolve("../");
		}
		return StoreUtil.resolvePath(store, relPath.toString(), "/");
	}

	/**
	 * Determines a URI that identifies the given Store with this locator.
	 *
	 * @param store != null
	 * @return URI != null
	 */
	public URI determineUri(final Store store) {
		Store candidate = store;
		StringBuilder path = new StringBuilder();
		for (int i = 0; i < StoreUtil.PARENT_LIMIT; i++) {
			Collection<String> matchingUris = this.storeToUris.get(candidate);
			if (!matchingUris.isEmpty()) {
				String uri = matchingUris.iterator().next();
				if (!uri.endsWith("/")) {
					uri = uri + "/";
				}
				return URI.create(uri).resolve(path.toString());
			}

			String name = candidate.getName();
			if (name != null) {
				if (path.length() > 0) {
					path.insert(0, "/");
				}
				path.insert(0, name);
				Store nextCandidate = candidate.getParent();
				if (nextCandidate != null) {
					candidate = nextCandidate;
					continue;
				}
			}
			// we get here if we hit a null name or followed the parents to the top, try unwrapping delegate
			if (store instanceof DelegateStore) {
				return determineUri(((DelegateStore) store).getDelegate());
			}
			throw Warden.spot(new NotAvailableException("Store " + store
					+ " not registered (indirectly) with this locator. name=" + name + " store=" + candidate));

		}
		throw Warden.spot(new IllegalArgumentException("The Store " + store + " seems to have an infinite (>"
				+ StoreUtil.PARENT_LIMIT + ") number of ancestors!"));
	}

	private static URI ensureTrailingSlash(final URI uri) {
		String path = uri.getPath();
		int p = path.lastIndexOf("/");
		if (p == path.length() - 1) {
			return uri;
		}
		return uri.resolve(path.substring(p + 1) + "/");
	}

	public void register(final Store store, final URI uri) {
		String keyUri = ensureTrailingSlash(uri).toASCIIString();
		this.storesByUri.put(keyUri, store);
		this.storeToUris.put(store, keyUri);
	}

	public int size() {
		return this.storesByUri.size();
	}
}
