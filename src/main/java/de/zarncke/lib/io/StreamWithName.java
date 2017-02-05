package de.zarncke.lib.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

import org.joda.time.DateTime;

import de.zarncke.lib.data.HasName;
import de.zarncke.lib.data.HasSelfInfo;
import de.zarncke.lib.time.HasDateTime;

/**
 * Named InputStream with optional meta information.
 * Note: Equality is only determined with respect to the name.
 */
public class StreamWithName implements HasName, HasDateTime, HasSelfInfo {

	public static StreamWithName getResourceAsStream(final String name) {
		return new StreamWithName(StreamWithName.class.getResourceAsStream(name), name);
	}

	public static StreamWithName getResourceAsStream(final Class<?> clazz, final String name) {
		return new StreamWithName(clazz.getResourceAsStream(name), clazz.getName() + ":" + name);
	}

	private final String name;
	private final InputStream inputStream;
	private final Map<String, ?> metaInformation;
	private DateTime lastModified;

	public StreamWithName(final InputStream in, final String location) {
		this(in, location, null);
	}

	public StreamWithName(final InputStream in, final String location, final Map<String, ?> metaInformation) {
		this.inputStream = in;
		this.name = location;
		this.metaInformation = metaInformation;
	}

	/**
	 * @deprecated use {@link HasSelfInfo#getSelfInfo()} instead
	 */
	@Deprecated
	public Map<String, ?> getMetaInformation() {
		return getSelfInfo();
	}

	@Override
	public Map<String, ? extends Object> getSelfInfo() {
		return this.metaInformation == null ? Collections.<String, Object> emptyMap() : this.metaInformation;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.name == null ? 0 : this.name.hashCode());
		return result;
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
		StreamWithName other = (StreamWithName) obj;
		if (this.name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!this.name.equals(other.name)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(final HasName o) {
		return this.name.compareTo(o.getName());
	}

	public void close() throws IOException {
		if (this.inputStream != null) {
			this.inputStream.close();
		}
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public DateTime getTime() {
		return this.lastModified;
	}

	public StreamWithName setTime(final DateTime lastModified) {
		this.lastModified = lastModified;
		return this;
	}

}