package de.zarncke.lib.coll;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.zarncke.lib.struct.Lattice;
import de.zarncke.lib.util.Misc;

/**
 * <p>
 * This class stores Objects for keys, that form a Lattice and allows retrieval of the best matching (nearest present ancestor)
 * key. With a ClassSet it can be used to find a handler for a given objects Class. First the Object of the same key is
 * returned. Otherwise all Objects with a upper (or lower) key are tried. Among the found Objects the least one (according to
 * Comparable) is returned. If this fails (non-comparable) IllegalArgumentException is thrown.
 * </p>
 *
 * @author Gunnar Zarncke
 */
// TODO add generics
@SuppressWarnings("unchecked")
public class HierarchicalMap extends AbstractMap
{
	/**
	 * the Lattice of the keys of the stored Objects
	 */
	private final Lattice keys;

	/**
	 * the stored Objects indexed by key
	 */
	private Map objects = new HashMap();

	/**
	 * whether to use upper or sub Class when searching
	 * true = search for best matching upper Class
	 */
	private final boolean useUpper;

	/**
	 * Tie breaking comparator for found objecs.
	 */
	private Comparator comparator = Misc.COMPARABLE_COMPARATOR;

    /**
	 * Creates new empty HierarchicalMap
	 * @param lattice to use for hierarchicalization
	 */
    public HierarchicalMap(final Lattice lattice)
	{
        this(lattice, false);
    }

    /**
	 * Creates new empty HierarchicalMap
	 * @param lattice to use for hierarchicalization
	 * @param useUpper whether to use upper or sub Types when searching,
	 *                 true = search for best matching upper Type
	 */
    public HierarchicalMap(final Lattice lattice, final boolean useUpper)
	{
        super();
		if (lattice.size() > 2)
		{
			throw new IllegalArgumentException
				("the lattice may contain no elements except TOP and BOTTOM!");
		}
		this.keys = lattice;
		this.useUpper = useUpper;
    }

    /**
	 * Creates new HierarchicalMap from another Map.
	 * @param map is the Map to insert
	 * @exception IllegalArgumentException if the map contains non-Class keys
	 */
    public HierarchicalMap(final HierarchicalMap map)
	{
		super();
		this.keys = map.keys;
		this.objects = map.objects;
		this.useUpper = map.useUpper;
		this.comparator = map.comparator;
    }

    /**
	 * Creates new HierarchicalMap from another Map.
	 * @param lattice to use for hierarchicalization
	 * @param map is the Map to insert
	 * @exception IllegalArgumentException if the map contains non-Class keys
	 */
    public HierarchicalMap(final Lattice lattice, final Map map)
	{
		this(lattice, map, false);
    }

    /**
	 * Creates new HierarchicalMap from another Map.
	 * @param lattice to use for hierarchicalization
	 * @param map is the Map to insert
	 * @param useUpper whether to use upper or sub Types when searching,
	 *                 true = search for best matching upper Type
	 * @exception IllegalArgumentException if the map contains non-Class keys
	 */
    public HierarchicalMap(final Lattice lattice, final Map map, final boolean useUpper)
	{
		this(lattice, useUpper);
        putAll(map);
    }

	/**
	 * add an Object under a given Type as key.
	 * @param key is the Type to store under
	 * @param value is the Object to store
	 * @return the Object stored under that Type before or null if new.
	 */
	@Override
	public Object put(final Object key, final Object value)
	{
		Object old = this.objects.put(key, value);

		if (old == null)
		{
			this.keys.add(key);
		}

		return old;
	}

	public void setComparator(final Comparator comparator)
	{
		this.comparator = comparator;
	}

	@Override
	public int size()
	{
		return this.objects.size();
	}

	@Override
	public Set entrySet()
	{
		return this.objects.entrySet();
	}

	@Override
	public Set keySet()
	{
		return Collections.unmodifiableSet(this.keys);
	}

	/**
	 * get the Object best matching the given key.
	 * @param key the requested key
	 * @return the best matching Object or null if the given key is
	 *         incompatible to all stored Objects keys.
	 */
	@Override
	public Object get(final Object key)
	{
		Object result = this.objects.get(key);
		if (result != null)
		{
			return result;
		}

		Object[] results = new Object[0];

		if (this.useUpper
			? key != this.keys.getTop()
			: key != this.keys.getBottom())
		{
			results = this.keys.getEquals(key);
		}

		if (results.length == 0)
		{
			if (this.useUpper)
			{
				results = this.keys.getUpper(key);
			}
			else
			{
				results = this.keys.getLower(key);
			}
		}

		if (results.length == 0)
		{
			return null;
		}

		if (results.length == 1)
		{
			return this.objects.get(results[0]);
		}

		//
		// otherwise use the least Object among the found ones
		//
		Object bestObject = null;

		for (Object result2 : results) {
			Object obj = this.objects.get(result2);
			try
			{
				if (bestObject == null
					|| this.comparator.compare(obj, bestObject) < 0)
				{
					bestObject = obj;
				}
			}
			catch (ClassCastException cce)
			{
				throw new IllegalArgumentException
					("Cannot find best matching object, because " + obj +
					 " and " + bestObject + " for key " + key +
					 " are uncomparable!");
			}
		}

		return bestObject;
	}

}

