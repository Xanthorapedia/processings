package ezcl;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;

import static org.jocl.CL.*;
import org.jocl.*;

public class EZMem {

	/** the context associated with this object */
	private EZContext context;
	/** the cl memory object */
	private cl_mem memory;
	/** read-/writ-ability */
	private long flags;
//	/** memory associated with this buffer */
//	private Object buffer;
	/** the c type of this Object */
	private int cType;
	/** the size of the buffer (in bytes) */
	private int size;
	/** the mapped pointer */
	private ByteBuffer ptr;

	/** type enums */
	public static enum VARTYPES {
		OTHER_T(null, 0), VOID_T("void", 1), BOOLEAN_T("bool", Sizeof.cl_uchar), BYTE_T("char",
				Sizeof.cl_uchar), CHAR_T("short", Sizeof.cl_ushort), SHORT_T("short", Sizeof.cl_ushort), INT_T("int",
						Sizeof.cl_uint), LONG_T("long", Sizeof.cl_ulong), FLOAT_T("float",
								Sizeof.cl_float), DOUBLE_T("double", Sizeof.cl_double);
		int value;
		int length;
		String cTypeName;

		/**
		 * Constructs with value from 0 to 8 and name
		 * 
		 * @param typeName
		 *            the name of each of the type
		 * @param len
		 *            the length (in bytes) of each type
		 */
		VARTYPES(String typeName, int len) {
			value = this.ordinal();
			cTypeName = typeName;
			length = len;
		}
	}

	/**
	 * Creates a EZMem from a existing cl_mem object
	 * 
	 * @param memory
	 *            the memory object
	 */
	public EZMem(cl_mem memory) {
		if (memory == null)
			throw new IllegalArgumentException("null cl_mem when creating EZMem");
		// mem created from cl_mem is a void type
		this.memory = memory;
		this.flags = (long) EZCL.getMemObjectInfo(memory, CL_MEM_FLAGS);
		this.size = (int) EZCL.getMemObjectInfo(memory, CL_MEM_SIZE);
		this.cType = 11;
	}

	/**
	 * Creates a EZMem instance on context containing array mem. Flag and size
	 * specifies the read/write permission and size (in bytes) of the memory
	 * object.
	 * 
	 * @param context
	 *            the context where the memory object is created
	 * @param mem
	 *            the memory object, must be an primitive array
	 * @param flag
	 *            read/write permission specifier
	 * @param size
	 *            the size of mem
	 */
	public EZMem(cl_context context, Object mem, long flag, int size) {
		this(createBuffers(context, flag, getLength(mem), mem));
		if (size > this.size)
			throw new IllegalArgumentException("memObject size (" + size + ") exceeds buffer size (" + this.size + ")");
		this.cType = getObjType(mem);
//		this.buffer = mem;
	}

	/**
	 * Creates a EZMem instance on context containing array mem. The memory
	 * object is readable and writable and uses host ptr by default. the size of
	 * the memory object is exactly the size of the array.
	 * 
	 * @param context
	 * @param mem
	 */
	public EZMem(cl_context context, Object mem) {
		this(context, mem, CL_MEM_READ_WRITE | CL_MEM_USE_HOST_PTR, getLength(mem));
	}

	/**
	 * Creates a memory object on context with access specifiers, size and a
	 * pointer to a local memory
	 * 
	 * @param context
	 *            the context associated with the mem object
	 * @param flags
	 *            access modifiers
	 * @param size
	 *            size of the buffer in bytes
	 * @param p
	 *            reference or Pointer to the buffer
	 * @return the cl_mem object created
	 */
	public static cl_mem createBuffers(cl_context context, long flags, int size, Object ptr) {
		if (EZMem.getObjType(ptr) == 11)
			throw new IllegalArgumentException("cannot create a cl_mem buffer from cl_mem");
		// already a Pointer, create pointer
		if (ptr instanceof Pointer)
			return clCreateBuffer(context, flags, size, (Pointer) ptr, null);
		else {
			// if not, check and create a pointer
			if (size < 1)
				throw new IllegalArgumentException("buffer size too small: " + size);
			if (context == null)
				throw new IllegalArgumentException("null context when creating buffers");
			Pointer p = EZMem.getPointerTo(ptr);
			if (p == null && (flags & CL_MEM_USE_HOST_PTR) != 0)
				throw new IllegalArgumentException("null host pointer when creating buffers using host_ptr");
			return clCreateBuffer(context, flags, size, p, null);
		}
	}

	/**
	 * Gets the length (in bytes) of a 1d primitive array Object. Unknown type
	 * returns the negative of the array (if it is)'s length.
	 * 
	 * @param obj
	 *            the 1d-array Object or a cl_mem or EZMem instance
	 * @return length in bytes
	 */
	static int getLength(Object obj) {
		if (obj == null)
			throw new IllegalArgumentException("cannot get length of a null array");

		// special treatment
		if (obj instanceof EZMem)
			return ((EZMem) obj).size;
		else if (obj instanceof cl_mem)
			return (int) EZCL.getMemObjectInfo((cl_mem) obj, CL_MEM_SIZE);//Sizeof.cl_mem;

		int type = getObjType(obj);
		// primitive
		if (type < 10)
			return VARTYPES.values()[type].length;

		int num = Array.getLength(obj);
		int len = VARTYPES.values()[getObjType(obj) % 10].length;
		len = (len == 0) ? -1 : len;
		return num * len;
	}

	/**
	 * Analyzes obj's type and gives a number that represents its type. Java
	 * primitive types <tt>boolean, byte, char, short, int, long, float, double
	 * </tt> and their corresponding wrapper types are assigned with number 2 to
	 * 9 respectively. <tt>cl_mem</tt> returns 11 as a void*. <tt>EZMem</tt>
	 * returns the type of the buffer it contains (can be void*) Other types
	 * returns 0. Array type number = primitive type number + dimension * 10.
	 * 
	 * @param obj
	 *            the Object to get type
	 * @return type number
	 */
	static int getObjType(Object obj) {
		if (obj == null)
			return VARTYPES.OTHER_T.value;

		// special treatment, obj is void*
		if (obj instanceof EZMem)
			return ((EZMem) obj).cType;
		else if (obj instanceof cl_mem)
			return 11;

		String javaType = obj.getClass().getSimpleName();
		int dimension = 0;
		// gets the first '[', gets array dimension and gets primitive name
		int i = 0;
		for (; i < javaType.length(); i++)
			if (javaType.charAt(i) == '[')
				break;
		// dimension = 0, return 0~8, dimension = 1, return 9~17, ...
		for (int j = i; j < javaType.length(); j += 2)
			dimension += 10;
		javaType = javaType.substring(0, i);

		switch (javaType) {
		case "Boolean":
		case "boolean":
			return VARTYPES.BOOLEAN_T.value + dimension;
		case "Byte":
		case "byte":
			return VARTYPES.BYTE_T.value + dimension;
		case "Character":
		case "char":
			return VARTYPES.CHAR_T.value + dimension;
		case "Short":
		case "short":
			return VARTYPES.SHORT_T.value + dimension;
		case "Integer":
		case "int":
			return VARTYPES.INT_T.value + dimension;
		case "Long":
		case "long":
			return VARTYPES.LONG_T.value + dimension;
		case "Float":
		case "float":
			return VARTYPES.FLOAT_T.value + dimension;
		case "Double":
		case "double":
			return VARTYPES.DOUBLE_T.value + dimension;
		default:
			return 0;
		}
	}

	/**
	 * Gets the c style primitive (or primitive array) type name. Arrays are
	 * represented as pointers. Non-primitive (array) type results in null
	 * return value.
	 * 
	 * @param type
	 *            the type of the Object, primitive values in [0, 9]
	 * @return the corresponding c type name
	 */
	static String getCTypeName(int type) {
		// primitive type index = type mod 10
		String cType = VARTYPES.values()[type % 10].cTypeName;
		while (type > 10) {
			cType += '*';
			type -= 10;
		}
		return cType;
	}

	/**
	 * Gets the pointer to the given Object, which must be an primitive array.
	 * 
	 * @param obj
	 *            an primitive array
	 * @return a pointer to the array
	 */
	static Pointer getPointerTo(Object obj) {
		if (obj == null)
			throw new IllegalArgumentException("null pointer 2 array");

		// both known/unknown EZMem type
		if (obj instanceof EZMem)
			return ((EZMem) obj).getPointer();

		try {
			switch (getObjType(obj)) {
			case 0:
				throw new IllegalArgumentException(
						"cannot point to an unknown type: " + obj.getClass().getSimpleName());
			case 2: // boolean
			case 12:// boolean[]
				throw new IllegalArgumentException("boolean types not supported, try to use byte/byte[] instead");
			case 3: // byte
				return Pointer.to(new byte[] { (byte) obj });
			case 4: // char
				return Pointer.to(new char[] { (char) obj });
			case 5: // short
				return Pointer.to(new short[] { (short) obj });
			case 6: // int
				return Pointer.to(new int[] { (int) obj });
			case 7: // long
				return Pointer.to(new long[] { (long) obj });
			case 8: // float
				return Pointer.to(new float[] { (float) obj });
			case 9: // double
				return Pointer.to(new double[] { (double) obj });
			case 11: // cl_mem
				return Pointer.to((cl_mem) obj);
			case 13:// byte[]
				return Pointer.to((byte[]) obj);
			case 14:// char[]
				return Pointer.to((char[]) obj);
			case 15:// short[]
				return Pointer.to((short[]) obj);
			case 16:// int[]
				return Pointer.to((int[]) obj);
			case 17:// long[]
				return Pointer.to((long[]) obj);
			case 18:// float[]
				return Pointer.to((float[]) obj);
			case 19:// double[]
				return Pointer.to((double[]) obj);
			default:// type >= 18, dimension > 1
				throw new IllegalArgumentException("cannot point to a non-1d array: " + obj.getClass().getSimpleName());
			}
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"cannot point to a non-primitive array: " + obj.getClass().getSimpleName());
		}
	}

	/**
	 * Determines if the type of arg is of type expectedType.
	 * 
	 * @param expectedType
	 *            name of the expected type of arg
	 * @param arg
	 *            the object to be determined
	 * @return "null argument" or "null expectedType" if arguments are null;
	 *         "unknown type" or "not an array or primitive" if the type is not
	 *         one of the primitive, wrapper and their array types; "cannot
	 *         convert from..." if the type names does not match caselessly;
	 *         null if match.
	 */
	static String isValidType(String expectedType, Object arg) {
		// general check
		if (arg == null)
			return "null argument";
		if (expectedType == null)
			return "null expectedType";
		int type = getObjType(arg);
		if (type <= 0)
			return "unknown type: " + arg.getClass().getSimpleName();
		else if (type > 19)
			return "not an array or primitive: " + arg.getClass().getSimpleName();

		// type check, void* can be any type
		String thisType = getCTypeName(type);
		if (thisType.trim().equalsIgnoreCase(expectedType.trim()) || thisType.trim().equalsIgnoreCase("void*"))
			return null;
		// if (!thisType.trim().equalsIgnoreCase(expectedType.trim()) ||
		// !thisType.trim().equalsIgnoreCase("void*"))
		return "cannot convert from \"" + thisType + "\" to \"" + expectedType + "\"";
	}

	/**
	 * Converts the flags to String, properties are separated by space.
	 * 
	 * @param flags
	 *            the access specifier when creating mem object
	 * @return String representation of the properties
	 */
	private static String flags2String(long flags) {
		String ret = "";
		if ((flags & CL_MEM_READ_WRITE) != 0)
			ret += "CL_MEM_READ_WRITE" + ' ';
		if ((flags & CL_MEM_WRITE_ONLY) != 0)
			ret += "CL_MEM_WRITE_ONLY" + ' ';
		if ((flags & CL_MEM_READ_ONLY) != 0)
			ret += "CL_MEM_READ_ONLY" + ' ';

		// mutually exclusive
		if ((flags & CL_MEM_USE_HOST_PTR) != 0)
			ret += "CL_MEM_USE_HOST_PTR" + ' ';
		else if ((flags & CL_MEM_ALLOC_HOST_PTR) != 0)
			ret += "CL_MEM_ALLOC_HOST_PTR" + ' ';
		else if ((flags & CL_MEM_COPY_HOST_PTR) != 0)
			ret += "CL_MEM_COPY_HOST_PTR" + ' ';
		return ret;
	}

	/**
	 * Gets the pointer to the cl_mem object
	 * 
	 * @return a pointer to the cl_mem object
	 */
	public Pointer getPointer() {
		return Pointer.to(memory);
	}

	/**
	 * Gets the c type name of the memory object
	 * 
	 * @return the c type name
	 */
	public String getcTypeName() {
		return VARTYPES.values()[cType % 10].cTypeName;
	}

	public int getSize() {
		return size;
	}

	public int length() {
		return size / VARTYPES.values()[cType % 10].length;
	}

	public EZContext getContext() {
		return context;
	}

	public void setContext(EZContext context) {
		this.context = context;
	}

	public cl_mem getMemory() {
		return memory;
	}

	public ByteBuffer getMappedPtr() {
		return ptr;
	}

	public void setMappedPtr(ByteBuffer ptr) {
		this.ptr = ptr;
	}

	@Override
	public String toString() {
		String s = "MemoryObject " + memory.toString().substring(6) + "\n";
		if (flags != 0)
			s += "Access flags:\t" + flags2String(flags) + "\n";
		if (cType != 0)
			s += "Array type:\t" + getCTypeName(cType) + "\n";
		s += "Array size:\t" + size + "\n";
		if (cType != 0)
			s += "# of Elements:\t" + length() + "\n";
		s += "On context:";
		if (context != null)
			s += "\n" + EZCL.infoPadding(context.toString(), "\t", 1) + "\n";
		else
			s += "\t" + EZCL.getMemObjectInfo(memory, CL_MEM_CONTEXT).toString().substring(10) + "\n";
		return s;
	}
}
