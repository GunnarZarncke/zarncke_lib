package de.zarncke.lib.lang.gen;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.struct.Lattice;
import de.zarncke.lib.struct.PartialOrder;

/**
 * a ClassSet is a Set of Classes. It can be asked for
 * super and sub classes.
 */
public class ClassSet extends Lattice
{
	private class Top {}
	private class Bottom {}

	/**
	 * A Class, that stands for the nonexistent top class, from wdich
	 * all classes inherit (a type above Object).
	 */
	public static final Class VOIDCLASS = Top.class;

 	/**
	 * A Class, that stands for the nonexistent bottom class, that inherits
	 * from all classes (the type of "null" if you like).
	 */
	public static final Class NONECLASS = Bottom.class;

	/**
	 * A PartialOrder for types, a class is greater, if it is higher in the
	 * class hierarchy.
	 * full contravariance (please note return types!), only public considered.
	 */
	public static final PartialOrder STRUCTURAL_ASSIGNABILITY =
	new PartialOrder()
	{
		public boolean lessEqual(Object a, Object b)
		{
			return isClassAssignableFrom((Class) b, (Class) a);
		}
		private boolean isClassAssignableFrom(Class sup, Class sub)
		{
			if (sup == VOIDCLASS)
			{
				return true;
			}

			if (sup == NONECLASS)
			{
				return sub == NONECLASS;
			}

			if (sub == VOIDCLASS)
			{
				return false;
			}

			if (sub == NONECLASS)
			{
				return true;
			}

			if (sup.isAssignableFrom(sub))
			{
				return true;
			}
		
			// todo: recursive types!

			Method[] methods = ClassTools.getMethods(sup);
			
			for (int i = 0; i < methods.length; i++)
			{
				Method metha = methods[i];
				if (!Modifier.isPublic(metha.getModifiers()))
				{
					continue;
				}

				Class[] as = metha.getParameterTypes();

				try
				{
					Method methb = sub.getMethod(metha.getName(), as);

					if (!isClassAssignableFrom(methb.getReturnType(), 
											   metha.getReturnType()))
					{
						return false;
					}
					Class[] bs = methb.getParameterTypes();
					for (int j = 0; j < bs.length; j++)
					{
						if (!isClassAssignableFrom(as[j], bs[j]))
						{
							return false;
						}
					}
				}
				catch (NoSuchMethodException nsme)
				{
					return false;
				}
			}
			return true;
		}
	};

	/**
	 * A PartialOrder for types, a class is greater, if it is higher in the
	 * class hierarchy.
	 * Uses standard java {@link Class#isAssignable}.
	 */
	public static final PartialOrder JAVA_ASSIGNABILITY =
	new PartialOrder()
	{
		public boolean lessEqual(Object a, Object b)
		{
			return isClassAssignableFrom((Class) b, (Class) a);
		}
		public boolean isClassAssignableFrom(Class sup, Class sub)
		{
			if (sup == VOIDCLASS)
			{
				return true;
			}

			if (sup == NONECLASS)
			{
				return sub == NONECLASS;
			}

			if (sub == VOIDCLASS)
			{
				return false;
			}

			if (sub == NONECLASS)
			{
				return true;
			}

			return sup.isAssignableFrom(sub);
		}
	};

	public ClassSet()
	{
		super(JAVA_ASSIGNABILITY, VOIDCLASS, NONECLASS);
	}

	public ClassSet(PartialOrder orderRespectingVoidAndNone)
	{
		super(orderRespectingVoidAndNone, VOIDCLASS, NONECLASS);
	}

	public Class[] getSuperClasses(Class c)
	{
		Object[] u = getUpper(c);
		Class[] uc = new Class[u.length];
		System.arraycopy(u, 0, uc, 0, u.length);
		return uc;
	}
	public Class[] getSubClasses(Class c)
	{
		Object[] u = getLower(c);
		Class[] uc = new Class[u.length];
		System.arraycopy(u, 0, uc, 0, u.length);
		return uc;
	}

	public String toString()
	{
		return "ClassSet of " + super.toString();
	}
}
