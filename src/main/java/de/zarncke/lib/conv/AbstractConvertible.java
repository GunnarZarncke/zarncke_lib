package de.zarncke.lib.conv;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import de.zarncke.lib.err.Warden;

public abstract class AbstractConvertible implements Convertible {

	public static enum FailMode {
		EMPTY, FAIL, NULL_OR_ZERO
	}

	private final FailMode failMode;

	public AbstractConvertible(final FailMode failMode) {
		this.failMode = failMode;
	}

	public Convertible emptyOnMissing() {
		return create(this, FailMode.EMPTY);
	}

	protected abstract AbstractConvertible create(AbstractConvertible abstractConvertible, FailMode newFailMode);

	public Convertible failOnMissing() {
		return create(this, FailMode.FAIL);
	}

	public Convertible nullOnMissing() {
		return create(this, FailMode.NULL_OR_ZERO);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] toArray(final Class<T> elementType) {
		return (T[]) toCollectionOf(elementType).toArray();
	}

	public BigDecimal toBigDecimal() {
		return null;
	}

	public BigDecimal toBigDecimalOrNull() {
		return null;
	}

	public BigInteger toBigInteger() {
		return null;
	}

	public BigInteger toBigIntegerOrNull() {
		return null;
	}

	public boolean toBoolean() {
		return "true".equals(toStringOrNull());
	}

	public Boolean toBooleanOrNull() {
		String s = toStringOrNull();
		return s == null ? null : "true".equals(s) ? Boolean.TRUE : Boolean.FALSE;
	}

	public byte toByte() {
		Byte b = toByteOrNull();
		if (b == null) {
			throwOnNull();
			return 0;
		}
		return b.byteValue();
	}

	public Byte toByteOrNull() {
		return null;
	}

	public char toCharacter() {
		return 0;
	}

	public Character toCharacterOrNull() {
		return null;
	}

	public <T> Collection<T> toCollectionOf(final Class<T> clazz) {
		return null;
	}

	public double toDouble() {
		return 0;
	}

	public Double toDoubleOrNull() {
		return null;
	}

	public float toFloat() {
		return 0;
	}

	public Float toFloatOrNull() {
		return null;
	}

	public int toInteger() {
		return 0;
	}

	public Integer toIntegerOrNull() {
		return null;
	}

	public long toLong() {
		return 0;
	}

	public Long toLongOrNull() {
		return null;
	}

	public <K, V> Map<K, V> toMapFromTo(final Class<K> keyType, final Class<V> valueType) {
		return null;
	}

	public Number toNumber() {
		return null;
	}

	public Number toNumberOrNull() {
		return null;
	}

	public Object toObject() {
		return null;
	}

	public <T> T toObject(final Class<T> clazz) {
		return null;
	}

	public Object toObjectOrNull() {
		return null;
	}

	public <T> T toObjectOrNull(final Class<T> clazz) {
		return null;
	}

	public <T> T toObjectQualified(final Class<T> clazz, final Object qualifier) {
		return null;
	}

	public <T> T toObjectQualifiedOrNull(final Class<T> clazz, final Object qualifier) {
		return null;
	}

	public short toShort() {
		return 0;
	}

	public Short toShortOrNull() {
		return null;
	}

	public String[] toStringArray() {
		return null;
	}

	@Override
	public String toString() {
		String s = toStringOrNull();
		if (s == null) {
			throwOnNull();
			return this.failMode == FailMode.EMPTY ? "" : null;
		}
		return s;
	}

	public String toStringOrNull() {
		return null;
	}

	public Throwable toThrowable() {
		return null;
	}

	public Throwable toThrowableOrNull() {
		return null;
	}

	private void throwOnNull() {
		if (this.failMode == FailMode.FAIL) {
			throw Warden.spot(new IllegalArgumentException("Missing value"));
		}
	}

}
