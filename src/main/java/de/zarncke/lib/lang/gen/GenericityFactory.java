package de.zarncke.lib.lang.gen;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;

import de.zarncke.lib.lang.ClassTools;
import de.zarncke.lib.lang.ObjectProcessor;
import de.zarncke.lib.lang.Processor;

/**
 * This class creates generic java classes that form a class hierarchy 
 * matching their represented/contained/modified Objects.
 * BCEL is used to synthesize the classes.
 */
public class GenericityFactory 
	implements ClassBinaryCreator, org.apache.bcel.Constants
{
	private static final String PROCESSOR = "ProcessorImpl";
	private static final String PROCESSOR_INTERFACE = "Processor";

	private String packagePrefix;

	private ClassLoader classLoader;
	
	public GenericityFactory(final ClassLoader classLoader)
	{
		this.packagePrefix = "de.zarncke.generic.";
		this.classLoader = classLoader;
	}

	public String getPackagePrefix()
	{
		return this.packagePrefix;
	}

	private ClassLoader getClassLoader()
	{
		return this.classLoader;
	}

	public Class createProcessor(final Class element)
	{
		String name = this.packagePrefix + element.getName() + PROCESSOR;
		try
		{
			return getClassLoader().loadClass(name);
		}
		catch (ClassNotFoundException cnfe)
		{
			throw new IllegalStateException
				("Cannot load generated class " + name + " due to " + cnfe + 
				 ". Possibly GenericityFactory not properly setup: " +
				 "must be loaded with a DemandClassLoader and registered there.");
		}
	}

/*
  public Class createSequence(Class element)
  {
  return getClassLoader()
  .loadClass(packagePrefix + element.getClass().getName() + "Sequence");
  }
*/
	@Override
	public byte[] createBinary(final String className)
		throws ClassNotFoundException
	{
		if (!className.startsWith(this.packagePrefix))
		{
			return null;
		}
		if (className.endsWith(PROCESSOR))
		{
			return createProcessorBinary
				(className, 
				 className.substring(this.packagePrefix.length(),
									 className.length() - PROCESSOR.length()), 
				 false);
		}
		else if (className.endsWith(PROCESSOR_INTERFACE))
		{
			return createProcessorBinary
				(className,
				 className.substring(this.packagePrefix.length(), 
									 className.length() 
									 - PROCESSOR_INTERFACE.length()), 
				 true);
		}
		return null;
	}
	
	private byte[] createProcessorBinary(final String className, final String wrapped, 
										 final boolean isInterface)
		throws ClassNotFoundException
	{
		Class wrappedClass = getClassLoader().loadClass(wrapped);
		
		Class wrapSuperClass = wrappedClass.getSuperclass();
		Class[] wrapInterfaces = wrappedClass.getInterfaces();

		int startInterface = 0;
		String superName;
		Class superClass;
		if (wrapSuperClass == null || wrapSuperClass.equals(Object.class))
		{
			wrapSuperClass = Object.class;
			if (wrapInterfaces.length > 0)
			{
				startInterface = 1;
				superName = this.packagePrefix + wrapInterfaces[0].getName() + 
					PROCESSOR;
				superClass = getClassLoader().loadClass(superName);
			}
			else
			{
				// implicitly assume non-interface
				superClass = ObjectProcessor.class;
				superName = superClass.getName();
			}
		}
		else
		{
			superName = this.packagePrefix + wrapSuperClass.getName() + PROCESSOR;
			superClass = getClassLoader().loadClass(superName);
		}

		String[] interfaceNames = new String[wrapInterfaces.length];
		Class[] interfaceClasses = new Class[wrapInterfaces.length];
		for (int i = 0; i < wrapInterfaces.length; i++)
		{
			interfaceNames[i] = this.packagePrefix + wrapInterfaces[i].getName() + 
				PROCESSOR_INTERFACE;
			interfaceClasses[i]=getClassLoader().loadClass(interfaceNames[i]);
		}

		if (isInterface && interfaceNames.length == 0)
		{
			interfaceNames = new String[] { Processor.class.getName() };
			interfaceClasses = new Class[] { Processor.class };
		}
		

		ClassGen cg;
		if (isInterface)
		{
			cg = new ClassGen(className, "java.lang.Object",
							  "<generated>", 
							  ACC_INTERFACE | ACC_PUBLIC | ACC_ABSTRACT,
							  interfaceNames);
		}
		else
		{
			cg = new ClassGen(className, superName,
							  "<generated>", 
							  ACC_PUBLIC | ACC_SUPER | ACC_ABSTRACT,
							  interfaceNames);
		}

		ConstantPoolGen cp = cg.getConstantPool();
		InstructionFactory factory = new InstructionFactory(cg);

		Set processed = new HashSet();
		addImpliedInterfaces(superClass, processed);

		if (isInterface)
		{
			MethodGen mg = new MethodGen(ACC_PUBLIC | ACC_ABSTRACT,
										 Type.VOID, 
										 new Type[]{ Type.getType(ClassTools.toSignature(wrappedClass)), 
													 Type.OBJECT }, 
										 new String[] { "processed", "state" },
										 "process", 
										 className, 
										 null, 
										 cp);
			cg.addMethod(mg.getMethod());
		}
		else
		{
			InstructionList il = new InstructionList();
			MethodGen mg = new MethodGen(ACC_PUBLIC,
										 Type.VOID, 
										 new Type[]{ Type.getType(ClassTools.toSignature(wrappedClass)), 
													 Type.OBJECT }, 
										 new String[] { "processed", "state" },
										 "process", 
										 className, 
										 il, 
										 cp);
			
			if (superClass != Object.class)
			{
				// add call to super
				il.append(new ALOAD(0));
				il.append(new ALOAD(1));
				il.append(new ALOAD(2));
				int sup = cp.addMethodref(superName, "process", 
										  "(" + ClassTools.toSignature(wrapSuperClass) + "Ljava/lang/Object;)V");
				il.append(new INVOKESPECIAL(sup));
			}

			// add calls to interfaces
			for (int i = startInterface; i < interfaceNames.length; i++)
			{
				if (!processed.contains(interfaceClasses[i]))
				{
					il.append(new ALOAD(0));
					il.append(new ALOAD(1));
					il.append(new ALOAD(2));
					int inter = cp.addMethodref(className, "process", "(" + ClassTools.toSignature(extractWrappedName(interfaceClasses[i].getName())) + "Ljava/lang/Object;)V");
					il.append(new INVOKEVIRTUAL(inter));
				}
			}
		
			il.append(new RETURN());
		
			mg.setMaxStack();
			cg.addMethod(mg.getMethod());
			
			il.dispose(); // Allow instruction handles to be reused
		}
		
		Set methods = new HashSet();
		methods.add(wrapped);

		// add methods for interfaces recursively
		addInterfaceMethods(interfaceClasses, cg, cp, processed, methods, 
							wrappedClass, className, isInterface);


		// add empty constructor
		if (!isInterface)
		{
			InstructionList il = new InstructionList();
			MethodGen mg = new MethodGen(ACC_PUBLIC,
										 Type.VOID,
										 new Type[0],
										 new String[0],
										 "<init>", 
										 className,
										 il, cp);
			int sup = cp.addMethodref(superName, "<init>", "()V");
			il.append(new ALOAD(0));
			il.append(new INVOKESPECIAL(sup));
			il.append(new RETURN());

			mg.setMaxStack();
			cg.addMethod(mg.getMethod());

			il.dispose();
		}

		Repository.addClass(cg.getJavaClass());
		org.apache.bcel.verifier.Verifier ver = 
			org.apache.bcel.verifier.VerifierFactory.getVerifier(className);
		org.apache.bcel.verifier.VerificationResult res = ver.doPass2();
		if (res.getStatus() == org.apache.bcel.verifier.VerificationResult.VERIFIED_REJECTED)
		{
			String[] msgs = ver.getMessages();
			
			throw new ClassNotFoundException
				("cannot generate class " + className + 
				 " due to verification error " + res.getMessage() + 
				 ".\nAnd see also:" + java.util.Arrays.asList(msgs));
		}

		return cg.getJavaClass().getBytes();
	}

	private void addInterfaceMethods(final Class [] interfaceClasses, final ClassGen cg, 
									 final ConstantPoolGen cp, final Set processed, 
									 final Set methods, final Class wrappedClass, 
									 final String className, final boolean isInterface) 
	{
		// methods for interfaces (transitively)
		for (Class type : interfaceClasses) {
			String wrapped = extractWrappedName(type.getName());
			if (!methods.contains(wrapped))
			{
				methods.add(wrapped);

				Type bcelType = Type.getType(ClassTools.toSignature(wrapped));

				InstructionList il = new InstructionList();
				MethodGen mg = 
					new MethodGen(ACC_PUBLIC | (isInterface?ACC_ABSTRACT:0),
								  Type.VOID, 
								  new Type[] { bcelType, Type.OBJECT }, 
								  new String[] { "processed", "state" },
								  "process", 
								  className, 
								  il, 
								  cp);

				Class[] subInterfaces = type.getInterfaces();

				if (!isInterface)
				{
					for (int j = 0; j < subInterfaces.length; j++)
					{
						if (!processed.contains(subInterfaces[j]))
						{
							processed.add(subInterfaces[j]);
							il.append(new ALOAD(0));
							il.append(new ALOAD(1));
							il.append(new ALOAD(2));
							int inter = cp.addMethodref(className, "process", "(" + ClassTools.toSignature(extractWrappedName(subInterfaces[j].getName())) + "Ljava/lang/Object;)V");
							il.append(new INVOKEVIRTUAL(inter));
						}
					}
					il.append(new RETURN());

					mg.setMaxStack();
					cg.addMethod(mg.getMethod());
				}

				il.dispose();

				// recurse
				addInterfaceMethods(subInterfaces, cg, cp, processed, methods, 
									wrappedClass, className, isInterface);
			}
		}
	}


	private void addImpliedInterfaces(final Class clazz, final Set set)
	{
		Class[] interfaces = clazz.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
		{
			if (!set.contains(interfaces[i]))
			{
				set.add(interfaces[i]);
				addImpliedInterfaces(interfaces[i], set);
			}
		}
	}

	private String extractWrappedName(final String className)
	{
		if (className.equals(Processor.class.getName())
			|| className.equals(ObjectProcessor.class.getName()))
		{
			return Object.class.getName();
		}
		if (!className.startsWith(this.packagePrefix))
		{
			throw new IllegalArgumentException
				(className + " is no wrapped name!");
		}
		if (className.endsWith(PROCESSOR))
		{
			return className.substring(this.packagePrefix.length(),
									   className.length()-PROCESSOR.length());
		}
		else if (className.endsWith(PROCESSOR_INTERFACE))
		{
			return className.substring(this.packagePrefix.length(), 
									   className.length() 
									   - PROCESSOR_INTERFACE.length());
		}
		throw new IllegalArgumentException
			(className + " is no wrapped name!");
	}


	/**
	 * @return String
	 */
	@Override
	public String toString()
	{
		return getClass().getName();
	}
}


