package de.zarncke.lib.struct;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.zarncke.lib.err.CantHappenException;
import de.zarncke.lib.util.Constants;

/**
 * a Lattice is a Set of Objects, that are partially ordered by
 * a PartialOrder. It can be asked for the least upper (lub)
 * and highest lower (hlb) elements.
 */
// TODO add generics
@SuppressWarnings("unchecked")
public class Lattice extends AbstractSet
{
	/**
	 * A default TOP element.
	 */
	public static final Object TOP =
	new Object() { @Override
	public String toString() { return "TOP"; }};

 	/**
	 * A default BOT element.
	 */
	public static final Object BOT =
	new Object() { @Override
	public String toString() { return "BOT"; }};

	/**
	 * mutable boolean
	 */
	private static class Flag
	{
		boolean flag;
		public Flag(final boolean b)
		{
			this.flag = b;
		}
	}

	/**
	 * <p>This interface allows access to the children and parent
	 * Objects of an Object.</p>
	 */
	private static class Node
	{
		/**
		 * a list of equal (with respect to the PartialOrder)
		 * Objects represented by this Node.
		 */
		public List objects;

		/**
		 * the children of this Node (lower Objects)
		 */
		public List children;

		/**
		 * the parents of this Node (the super Objects)
		 */
		public List parents;

		public Node(final Object object, final List children, final List parents)
		{
			this.objects = new ArrayList();
			this.objects.add(object);

			this.children = children;
			this.parents = parents;
		}

		public Object getObject()
		{
			return this.objects.get(0);
		}

		private String toName()
		{
			// concatenate all names
			StringBuffer sb = new StringBuffer();
			int i;
			for (i = 0; i < this.objects.size(); i++)
			{
				if (i > 0)
				{
					sb.append(",");
				}
				sb.append(this.objects.get(i));
			}

			if (i > 0)
			{
				return (i == 1 ? sb : sb.insert(0, "{").append("}"))
					.toString();
			}

			throw new CantHappenException
				("each Node must have at least one Object.");
		}

		private static String toNames(final List list)
		{
			StringBuffer sb = new StringBuffer();
			Iterator it = list.iterator();
			while (it.hasNext())
			{
				sb.append(((Node) it.next()).toName());
				if (it.hasNext())
				{
					sb.append(", ");
				}
			}
			return sb.toString();
		}

		@Override
		public String toString()
		{
			return toName() + " lub {" + toNames(this.parents) +
				"} hlb {" + toNames(this.children) + "}";
		}
	}

	/**
	 * compares Comparables. Respects TOP and BOT.
	 */
	public static final PartialOrder IMPLICIT_ORDER = new PartialOrder()
	{
		@Override
		public boolean lessEqual(final Object a, final Object b)
			{
				if (a == b)
				{
					return true;
				}
				if (b == TOP)
				{
					return true;
				}
				if (a == BOT)
				{
					return true;
				}
				return ((Comparable) a).compareTo(b) <= 0;
			}
		};

	/**
	 * the Nodes of child/parent relations indexed by the Object
	 */
	private final Map lattice;

	/**
	 * the TOP Node
	 */
	private final Node top;

	/**
	 * the BOT Node
	 */
	private final Node bot;

	/**
	 * A PartialOrder.
	 * It must respect TOP and BOT values.
	 */
	private final PartialOrder order;

	public Lattice()
	{
		this(IMPLICIT_ORDER, TOP, BOT);
	}
	public Lattice(final PartialOrder order)
	{
		this(order, TOP, BOT);
	}
	public Lattice(final PartialOrder order, final Object top, final Object bot)
	{
		if (order.lessEqual(top, bot) && !order.lessEqual(bot, top))
		{
			throw new IllegalArgumentException
				("PartialOrder " + order + " must ensure, that top " + top +
					" is stricty greater than bottom " + bot + "!");
		}

		this.order = order;

		// init lattice to contain top TOP and BOT

        // TOP never contains parent, BOT never children
		this.top = new Node(top, new ArrayList(), Collections.EMPTY_LIST);
		this.bot = new Node(bot, Collections.EMPTY_LIST, new ArrayList());

		this.top.children.add(this.bot);
		this.bot.parents.add(this.top);

		this.lattice = new HashMap();

		this.lattice.put(top, this.top);
		this.lattice.put(bot, this.bot);
	}

	public Lattice(final Collection coll)
	{
		this();
		for (Iterator it = coll.iterator(); it.hasNext(); )
		{
			add(it.next());
		}
	}

	public Object getTop()
	{
		return this.top;
	}

	public Object getBottom()
	{
		return this.bot;
	}

	@Override
	public int size()
	{
		return this.lattice.size();
	}

	@Override
	public Iterator iterator()
	{
		return this.lattice.keySet().iterator();
	}

	/**
	 * Removes an Object from this Lattice.
	 * The internal Object graph is updated.
	 * Please note, that removal is probably not very fast (even slower than
	 * {@link #add(Object)}, even though na object graph is used.
	 * @param object is the object to be added
	 */
	@Override
	public boolean remove(final Object object)
	{
		// remove from relations, if not present exit
		Node node = (Node) this.lattice.remove(object);
		if (node == null)
		{
			return false;
		}

		//
		// first remove us from all parents and children
		// (this will allow the second (main) stage to search for
		// other ways to children without tripping over us).
		//
		Iterator pIt = node.parents.iterator();
		while (pIt.hasNext())
		{
			Node p = (Node) pIt.next();
			p.children.remove(node);
		}

		Iterator chIt = node.children.iterator();
		while (chIt.hasNext())
		{
			Node ch = (Node) chIt.next();
			ch.parents.remove(node);
		}

		//
		// second reconnect our parents with those childs of us, that
		// are not otherwise reachable. that latter is the tricky condition.
		// we use a moving frontier starting at the parent and look which
		// childs (the base) are hit. those are NOT connected.
		//

		// calculate reused child number and hash map
		HashMap keptBase = new HashMap();

		int keptCount = 0;
		Iterator it = node.children.iterator();
		while (it.hasNext())
		{
			keptBase.put(it.next(), new Flag(false));

			keptCount++;
		}

		// iterate over all parents
		Iterator parents = node.parents.iterator();
		while (parents.hasNext())
		{
			//
			// for every parent do
			// - initialize
			// - let the frontier (starting at parent) hit the base (children)
			// - connect parent to unhit children
			//

			// init

			// get next parent
			Node parent = (Node) parents.next();

			// start frontier with parent
			List frontier = new ArrayList();
			frontier.add(parent);

			// start base with kept base
			Map base = parents.hasNext() ? (Map) keptBase.clone() : keptBase;

			int childrenLeft = keptCount;


			// go hit the base

			// as long as a frontier or any unhit children remain
			while (!frontier.isEmpty() && childrenLeft > 0)
			{
				// advance one node of the frontier
				Node first = (Node) frontier.remove(0);

				// traverse all children of that node
				Iterator fIt = first.children.iterator();
				while (fIt.hasNext())
				{
					Node f = (Node) fIt.next();

					// have we hit the base?
					Flag b = (Flag) base.get(f);
					if (b != null)
					{
						// child not yet hit?
						if (!b.flag)
						{
							// then set the flag and decrese count
							b.flag = true;
							childrenLeft--;
						}
					}
					// if child unequals bot
					else if (f != this.bot)
					{
						// then add it to the frontier
						frontier.add(f);
					}
				}
			}


			// reconnect unhit children

			Iterator baseIt = base.entrySet().iterator();
			while (baseIt.hasNext())
			{
				Map.Entry me = (Map.Entry) baseIt.next();

				// child in base unhit?
				Flag b = (Flag) me.getValue();
				if (!b.flag)
				{
					Node ch = (Node) me.getKey();

					ch.parents.add(parent);
					parent.children.add(ch);
				}
				else
				{
					// reset flag for next parent
					b.flag = false;
				}

			}

		}

		// done
		return true;
	}

	/**
	 * Adds an Object to this Lattice. The internal Object graph is updated.
	 * Please note, that the addition is probably not very fast,
	 * even though an object graph is used.
	 * Please construct the Lattice only once and query it often.
	 * @param object is the object to be added
	 * @return true if added (not already present)
	 */
	@Override
	public boolean add(final Object object)
	{
		if (this.lattice.containsKey(object))
		{
			return false;
		}
		//
		// search for direct parents of us
		//

		List mayParent = new ArrayList();
		List mustParent = new ArrayList();

		// start with TOP as possible parent
		mayParent.add(this.top);


		// as long as more possible parents remain
		while (!mayParent.isEmpty())
		{
			// try one possible parent
			Node first = (Node) mayParent.remove(0);

			// whether the tried anchestor is a fact a parent
			boolean precise = true;

			// check all children of that anchestor
			Iterator chIt = first.children.iterator();
			while (chIt.hasNext())
			{
				Node ch = (Node) chIt.next();

				// is the child a parent of us?
				if (isObjectLessThan(object, ch.getObject()))
				{
					// are we possibly equal to the child?
					if (isObjectLessThan(ch.getObject(), object))
					{
						// then shortcut: add us to that node,
						ch.objects.add(object);

						// add us us to the object relations
						this.lattice.put(object, ch);

						// and done
						return true;
					}

					// then the parent is in fact some grand parent
					precise = false;

					// but the child may be our parent
					if (!mayParent.contains(ch))
					{
						mayParent.add(ch);
					}
				}
			}


			// if the parent comes out as one of our parents
			if (precise)
			{
				// then we remember it
				mustParent.add(first);

				// and disconnect it from those children...
				chIt = first.children.iterator();
				while (chIt.hasNext())
				{
					Node ch = (Node) chIt.next();

					// ...that are in fact our children
					// (they will be connected to us later)
					if (isObjectLessThan(ch.getObject(), object)
						|| ch == this.bot)
					{
						chIt.remove();
						//ch.parents.remove(first);
					}
				}
			}
	    }


		//
		// search for direct children of us
		//

		List mayChild = new ArrayList();
		List mustChild = new ArrayList();

		// start with BOT as possible child
		mayChild.add(this.bot);


		// as long as more possible children remain
		while (!mayChild.isEmpty())
		{
			// try one possible child
			Node first = (Node) mayChild.remove(0);

			// whether the tried descendant is a fact a direct child of us
			boolean precise = true;

			// check all parent of that descendant
			Iterator pIt = first.parents.iterator();
			while (pIt.hasNext())
			{
				Node p = (Node) pIt.next();

				// is the parent a child of us?
				if (isObjectLessThan(p.getObject(), object)
					|| p == this.bot)
				{
					// the child is in fact some grand child
					precise = false;

					// but the parent may be our child
					if (!mayChild.contains(p))
					{
						mayChild.add(p);
					}
				}
			}


			// if the child comes out as one of our children
			if (precise)
			{
				// then we remember it
				mustChild.add(first);

				// and disconnect it from those parents...
				pIt = first.parents.iterator();
				while (pIt.hasNext())
				{
					Node p = (Node) pIt.next();

					// ...that are in fact our parents
					// (they will be connected to us later)
					if (isObjectLessThan(object, p.getObject()))
					{
						pIt.remove();
						//p.children.remove(first);
					}
				}
			}
        }


		// now we have our own node
		Node node = new Node(object, mustChild, mustParent);


		//
		// connect all our children and parents with us
		//

		// connect all parents of us
		Iterator pIt = mustParent.iterator();
		while (pIt.hasNext())
		{
			Node p = (Node) pIt.next();
			p.children.add(node);
		}

		// connect all children of us
		Iterator chIt = mustChild.iterator();
		while (chIt.hasNext())
		{
			Node ch = (Node) chIt.next();
			ch.parents.add(node);
		}

		// add us us to the object relations
		this.lattice.put(object, node);

		// done successfully
		return true;
	}

	/**
	 * extract an array of Objects from an array of Nodes
	 */
	private static Object[] toObjects(final Object[] nodes)
	{
		ArrayList objects = new ArrayList();

		for (Object node : nodes) {
			objects.addAll(((Node) node).objects);
		}

		return objects.toArray(new Object[objects.size()]);
	}


	/**
	 * get the array of super Objects of the given Object.
	 * This method is fast (it does a object graph lookup).
	 * @param object is a Object (not neccessarily contained in the Lattice).
	 * @return array of spuer Objects.
	 */
	public Object[] getUpper(final Object object)
	{
		Node node = (Node) this.lattice.get(object);

		// if a Node is stored for that Object we simply lookup
		if (node != null)
		{
			return toObjects(node.parents.toArray());
		}

		// otherwise we climb down the Object graph until reaching all parents
		// (like first stage in add() above)

		List may = new ArrayList();
		List must = new ArrayList();

		may.add(this.top);

		while (!may.isEmpty())
		{
			Node first = (Node) may.remove(0);

			Iterator chIt = first.children.iterator();

			boolean precise = true;

			while (chIt.hasNext())
			{
				Node ch = (Node) chIt.next();
				if (isObjectLessThan(object, ch.getObject()))
				{
					// are we possibly equal to the child?
					if (isObjectLessThan(ch.getObject(), object))
					{
						// then shortcut by using that Nodes parents
						return toObjects(ch.parents.toArray());
					}

					// then the parent is in fact some grand parent
					precise = false;

					// but the child may be our parent
					if(!may.contains(ch))
					{
						may.add(ch);
					}
				}
			}

			if (precise)
			{
				must.add(first);
			}
		}

		return toObjects(must.toArray());
	}

	/**
	 * get the array of sub Objects of the given Object.
	 * This method is fast (it does a object graph lookup).
	 * @param object is a Object (not neccessarily contained in the Lattice).
	 * @return array of sub Objects.
	 */
	public Object[] getLower(final Object object)
	{
		Node node = (Node) this.lattice.get(object);

		if (node != null)
		{
			return toObjects(node.children.toArray());
		}

		// otherwise we climb up the Object graph until reaching all children
		// (like second stage in add() above)

		List may = new ArrayList();
		List must = new ArrayList();
		may.add(this.bot);

		while (!may.isEmpty())
		{
			Node first = (Node) may.remove(0);

			Iterator pIt = first.parents.iterator();

			boolean precise = true;

			while (pIt.hasNext())
			{
				Node p = (Node) pIt.next();
				if (isObjectLessThan(p.getObject(), object)
				   || p == this.bot)
				{
					precise = false;

					if (!may.contains(p))
					{
						may.add(p);
					}
				}
			}

			if (precise)
			{
				must.add(first);
			}
		}


		return toObjects(must.toArray());
	}

	/**
	 * get the array of Objects equal (wrt {@link #isObjectEqualTo})
	 * to the given Object.
	 * Please note, that only Objects contained in the Lattice are returned.
	 * This method is fast (it does a object graph lookup).
	 *
	 * @param object is a Object (not neccessarily contained in the Lattice).
	 * @return array of equal Objects (the given Object is contained only, if
	 * is itself is in the Lattice).
	 */
	public Object[] getEquals(final Object object)
	{
		Node node = (Node) this.lattice.get(object);

		if(node != null)
		{
			return node.objects
				.toArray(new Object[node.objects.size()]);
		}

		// otherwise we climb down the Object graph until reaching the object
		// (like first stage in add() above)

		List may = new ArrayList();
		List must = new ArrayList();

		may.add(this.top);

		while (!may.isEmpty())
		{
			Node first = (Node) may.remove(0);

			Iterator chIt = first.children.iterator();

			boolean precise = true;

			while (chIt.hasNext())
			{
				Node ch = (Node) chIt.next();
				if (isObjectLessThan(object, ch.getObject()))
				{
					// are we possibly equal to the child?
					if (isObjectLessThan(ch.getObject(), object))
					{
						// then we found an equal Object
						// and return its objects
						return ch.objects.toArray
							(new Object[ch.objects.size()]);
					}

					// then the parent is in fact some grand parent
					precise = false;

					// but the child may be our parent
					if (!may.contains(ch))
					{
						may.add(ch);
					}
				}
			}

			if (precise)
			{
				must.add(first);
			}
		}

		// if didn't stumble over an equal Object, we return none
		return Constants.NO_OBJECTS;
	}

	public boolean isObjectLessThan(final Object a, final Object b)
	{
		return this.order.lessEqual(a, b);
	}

	public boolean isObjectEqualTo(final Object a, final Object b) {
		return this.order.lessEqual(a, b) && this.order.lessEqual(b, a);
	}

	@Override
	public String toString()
	{
		return "Lattice" + this.lattice;
	}


}


