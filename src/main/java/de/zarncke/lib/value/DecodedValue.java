package de.zarncke.lib.value;

import java.io.IOException;

import com.google.common.base.Function;

import de.zarncke.lib.data.HasData;
import de.zarncke.lib.err.NotAvailableException;
import de.zarncke.lib.err.Warden;
import de.zarncke.lib.region.Region;

public class DecodedValue<T> implements Value<T> {

	private Function<Region, T> decoder;
	private HasData dataSource;

	@Override
	public T get() {
		try {
			return this.decoder.apply(this.dataSource.asRegion());
		} catch (IOException e) {
			throw Warden.spot(new NotAvailableException("cannot read value from " + this.dataSource, e));
		}
	}

}
