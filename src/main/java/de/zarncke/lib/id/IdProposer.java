package de.zarncke.lib.id;

import java.io.Serializable;

/**
 * Classes implementing this interface may propose an id for themselves.
 * The id need not be unique among all objects. The user of this interface will add suitable identifying information.
 * A mechanism like {@link SerializingDbResolver} is then used to store the object under this id for later retrieval with a
 * {@link Resolver}.
 * The class should probably also be {@link Serializable} to allow storage in binary form.
 * Note: Readable ids are often used to allow content sensitive indexing and search engine optimization.
 * A short (<<32 bytes) name like string (e.g. "Adventure Jack") would be appropriate.
 * 
 * @author Gunnar Zarncke
 */
public interface IdProposer {
	String getIdProposal();
}
