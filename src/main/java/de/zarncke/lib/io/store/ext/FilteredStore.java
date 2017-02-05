package de.zarncke.lib.io.store.ext;

import java.util.regex.Pattern;

import de.zarncke.lib.io.store.Store;

public class FilteredStore extends EnhanceBaseStore<FilteredStore> {

	public FilteredStore(final Store delegate, final String regExp) {
		super(delegate, new Enhancer<FilteredStore>() {
			private Pattern pattern = Pattern.compile(regExp);

			@Override
			public FilteredStore enhance(final Store store) {
				return new FilteredStore(store, ".*");
			}

			@Override
			public boolean isIncluded(final Store store) {
				return this.pattern.matcher(store.getName()).matches();
			}
		});
	}

	@Override
	public boolean exists() {
		return super.exists();
	}

}
