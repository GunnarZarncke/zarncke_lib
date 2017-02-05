package de.zarncke.lib.region;

/**
 * A {@link Binary binary} object can be converted {@link #encode() to} and {@link Binary.Decoder#decode from} a byte array.
 * Compare with {@link de.zarncke.lib.data.HasData} which only associates some bytes whereas here the intention is to be 'equal'
 * to the bytes.
 * Note: The bytes may or may not contain type information. The decoder may or may not
 * 
 * @author Gunnar Zarncke
 */
public interface Binary {
	interface Decoder<T extends Binary> {
		T decode(Region data);
	}

	Region encode();
}
