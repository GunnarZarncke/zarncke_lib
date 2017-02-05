package de.zarncke.lib.lang.gen;

import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DLOAD;
import org.apache.bcel.generic.DRETURN;
import org.apache.bcel.generic.FLOAD;
import org.apache.bcel.generic.FRETURN;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.IRETURN;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.LLOAD;
import org.apache.bcel.generic.LRETURN;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Type;

import de.zarncke.lib.util.Chars;

/**
 * This class created java-Classes from a specification.
 * BCEL is used to synthesize the classes.
 */
public class ClassFactory implements org.apache.bcel.Constants
{
	public static class Spec
	{
		private String name;
		public String getName() {return this.name;}
		public String setName(final String name) { this.name=name; return name;}

		private String superClass;
		public String getSuperClass() {return this.superClass;}
		public String setSuperClass(final String superClass) { this.superClass=superClass; return this.name;}

		private boolean isInterface;
		public boolean isInterface() {return this.isInterface;}
		public boolean setInterface(final boolean isInterface) { this.isInterface=isInterface; return isInterface;}

		private Map properties;
		public Map getProperties() {return this.properties;}
		public Map setProperties(final Map properties) { this.properties=properties; return properties;}

		private String[] interfaces;
		public String [] getInterfaces() {return this.interfaces;}

		public String[] setInterfaces(final String[] interfaces) {
			this.interfaces = interfaces.clone();
			return interfaces;
		}


		public Spec(final String name, final boolean isInterface,
					final Map properties, final String superClass, final String[] interfaces)
		{
			this.name = name;
			this.isInterface = isInterface;
			this.properties = properties;
			this.interfaces = interfaces.clone();
			this.superClass = superClass;
		}
	}

	private static final class DefineLoader extends ClassLoader
	{
		public Class defineClass(final String name, final byte[] data)
		{
			return super.defineClass(name, data, 0, data.length);
		}
	}
	private final DefineLoader DEFINER = new DefineLoader();

	public ClassFactory()
	{
	}

	public Class createClass(final Spec spec)
	{
		return this.DEFINER.defineClass(spec.getName(), createClassBinary(spec));
	}

	public byte[] createInterfaceBinary(final Spec spec)
	{
		ClassGen cg = new ClassGen(spec.getName(), "java.lang.Object",
								   "<generated>",
								   ACC_INTERFACE | ACC_PUBLIC | ACC_SUPER,
								   spec.getInterfaces());
		ConstantPoolGen cp = cg.getConstantPool();

		for (Iterator it = spec.getProperties().entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry me = (Map.Entry) it.next();
			String name = (String) me.getKey();
			String type = (String) me.getValue();

			MethodGen mg = new MethodGen(ACC_PUBLIC | ACC_ABSTRACT, // access flags
										 Type.getType(type),   // return type
										 new Type[0], // arg types
										 new String[0], // arg names
										 "get" + name,  // method
										 spec.getName(), //class
										 null, cp);
			cg.addMethod(mg.getMethod());

			mg = new MethodGen(ACC_PUBLIC | ACC_ABSTRACT, // access flags
							   Type.VOID,       // return type
							   new Type[]{ Type.getType(type) }, // arg types
							   new String[] { name }, // arg names
							   "set" + name,  // method
							   spec.getName(), //class
							   null, cp);
			cg.addMethod(mg.getMethod());
		}

		return cg.getJavaClass().getBytes();
	}

	public byte[] createClassBinary(final Spec spec)
	{
		if (spec.isInterface())
		{
			return createInterfaceBinary(spec);
		}

		ClassGen cg = new ClassGen(spec.getName(), spec.getSuperClass(),
								   "<generated>", ACC_PUBLIC | ACC_SUPER,
								   spec.getInterfaces());

		ConstantPoolGen cp = cg.getConstantPool();
		InstructionFactory factory = new InstructionFactory(cg);
		MethodGen mg;

		for (Iterator it = spec.getProperties().entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry me = (Map.Entry) it.next();
			String name = (String) me.getKey();
			String type = (String) me.getValue();

			FieldGen fg = new FieldGen(ACC_PRIVATE, // access flags
									   Type.getType(type),   // return type
									   name,  // field
									   cp);
			Field f = fg.getField();
			cg.addField(f);

			int prop = cp.addFieldref(spec.getName(), name, type);


			// getter
			InstructionList il = new InstructionList();
			mg = new MethodGen(ACC_PUBLIC, // access flags
							   Type.getType(type),   // return type
							   new Type[0], // arg types
							   new String[0], // arg names
							   "get" + name,  // method
							   spec.getName(), //class
							   il, cp);

			il.append(new ALOAD(0));
			il.append(new GETFIELD(prop));
			switch (type.charAt(0))
			{
				case 'Z': case 'B': case 'C': case 'S': case 'I':
					il.append(new IRETURN()); break;
				case 'F':
					il.append(new FRETURN()); break;
				case 'D':
					il.append(new DRETURN()); break;
				case 'J':
					il.append(new LRETURN()); break;
				default:
					il.append(new ARETURN());
			}

			mg.setMaxStack();
			cg.addMethod(mg.getMethod());

			il.dispose(); // Allow instruction handles to be reused


			// setter
			il = new InstructionList();
			mg = new MethodGen(ACC_PUBLIC, // access flags
							   Type.VOID,       // return type
							   new Type[]{ Type.getType(type) }, // arg types
							   new String[] { name }, // arg names
							   "set" + name,  // method
							   spec.getName(), //class
							   il, cp);

			il.append(new ALOAD(0));
			switch (type.charAt(0))
			{
				case 'Z': case 'B': case 'C': case 'S': case 'I':
					il.append(new ILOAD(1)); break;
				case 'F':
					il.append(new FLOAD(1)); break;
				case 'D':
					il.append(new DLOAD(1)); break;
				case 'J':
					il.append(new LLOAD(1)); break;
				default:
					il.append(new ALOAD(1));
			}
			il.append(new PUTFIELD(prop));
			il.append(new RETURN());

			mg.setMaxStack();
			cg.addMethod(mg.getMethod());

			il.dispose(); // Allow instruction handles to be reused
		}

		InstructionList il = new InstructionList();
		mg = new MethodGen(ACC_PUBLIC,  // access flags
						   Type.VOID,   // return type
						   new Type[0], // arg types
						   new String[0], // names
						   "<init>",  // method
						   spec.getName(), //class
						   il, cp);
		int sup = cp.addMethodref("java.lang.Object", "<init>", "()V");
		il.append(new ALOAD(0));
		il.append(new INVOKESPECIAL(sup));
		il.append(new RETURN());

		mg.setMaxStack();
		cg.addMethod(mg.getMethod());

		il.dispose(); // Allow instruction handles to be reused


		return cg.getJavaClass().getBytes();
	}

	public static String toSignature(final Class cl)
	{
		return cl.isArray() ? "[" + toSignature(cl.getComponentType())
			: !cl.isPrimitive()
			? "L" + Chars.replaceAll(cl.getName(), ".", "/") + ";"
			: cl.equals(Boolean.TYPE) ? "Z"
			: cl.equals(Byte.TYPE) ? "B"
			: cl.equals(Short.TYPE) ? "S"
			: cl.equals(Character.TYPE) ? "C"
			: cl.equals(Integer.TYPE) ? "I"
			: cl.equals(Long.TYPE) ? "J"
			: cl.equals(Float.TYPE) ? "F"
			: cl.equals(Double.TYPE) ? "D" : "V";
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


