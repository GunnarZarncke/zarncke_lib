package de.zarncke.lib.conv;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public interface Convertible {

	boolean toBoolean();

	Boolean toBooleanOrNull();

	byte toByte();

	Byte toByteOrNull();

	short toShort();

	Short toShortOrNull();

	char toCharacter();

	Character toCharacterOrNull();

	int toInteger();

	Integer toIntegerOrNull();

	long toLong();

	Long toLongOrNull();

	float toFloat();

	Float toFloatOrNull();

	double toDouble();

	Double toDoubleOrNull();

	Number toNumber();

	Number toNumberOrNull();

	BigInteger toBigInteger();

	BigInteger toBigIntegerOrNull();

	BigDecimal toBigDecimal();

	BigDecimal toBigDecimalOrNull();

	String toStringOrNull();

	Object toObject();

	Object toObjectOrNull();

	<T> T toObject(Class<T> clazz);

	<T> T toObjectOrNull(Class<T> clazz);

	<T> T toObjectQualified(Class<T> clazz, Object qualifier);

	<T> T toObjectQualifiedOrNull(Class<T> clazz, Object qualifier);

	<T> Collection<T> toCollectionOf(Class<T> clazz);

	<K, V> Map<K, V> toMapFromTo(Class<K> keyType, Class<V> valueType);

	String[] toStringArray();

	<T> T[] toArray(Class<T> elementType);

	Throwable toThrowable();

	Throwable toThrowableOrNull();

	Convertible failOnMissing();

	Convertible nullOnMissing();

	Convertible emptyOnMissing();
}
