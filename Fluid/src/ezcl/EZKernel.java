package ezcl;

import static org.jocl.CL.*;
import org.jocl.*;

public class EZKernel {

	/** the context where the kernel is on */
	private EZContext context;
	/** the cl_kernel obj */
	private cl_kernel kernel;
	/** the name of the kernel in source */
	private String kernelName;
	/** argument count */
	private int argCount;
	/** argument modifiers */
	private String[] argModifiers;
	/** argument types */
	private String[] argTypes;
	/** argument names */
	private String[] argNames;

	/**
	 * Creates a kernel on the context from src source with name knlName
	 * 
	 * @param context
	 *            the context where the kernel will be built on
	 * @param src
	 *            source code
	 * @param knlName
	 *            kernel name (function name in src)
	 */
	public EZKernel(cl_context context, String[] src, String knlName) {
		this(createKernels(context, src, new String[] { knlName })[0], src, knlName);
	}

	/**
	 * Encapsulates a existing kernel built from src source with name knlName
	 * 
	 * @param kernel
	 *            the existing kernel
	 * @param src
	 *            source code
	 * @param knlName
	 *            kernel name (function name in src)
	 */
	public EZKernel(cl_kernel kernel, String[] src, String knlName) {
		if (kernel == null)
			throw new IllegalArgumentException("null kernel");

		long[] len = new long[1];

		this.kernel = kernel;
		if (knlName != null)
			kernelName = knlName;
		// get arg num
		{
			clGetKernelInfo(kernel, CL_KERNEL_NUM_ARGS, 0, null, len);
			int[] buf = new int[(int) len[0]];
			clGetKernelInfo(kernel, CL_KERNEL_NUM_ARGS, len[0], Pointer.to(buf), len);
			argCount = buf[0];
		}

		// parse arguments
		argNames = new String[argCount];
		argTypes = new String[argCount];
		argModifiers = new String[argCount];
		String[] pKN = new String[1];
		parseKernelArgs(kernel, src, pKN, argTypes, argNames, argModifiers);
		kernelName = pKN[0];
	}

	/**
	 * Creates the program and build the kernels on the context from Strings in
	 * src. If knlNames is not null, kernels will be built in the order of their
	 * names in knlNames
	 * 
	 * @param context
	 *            the context
	 * @param src
	 *            an array of source code Strings
	 * @param knlNames
	 *            names of kernels to build
	 * @return an array of build kernel
	 */
	public static cl_kernel[] createKernels(cl_context context, String[] src, String[] knlNames) {
		int[] num = new int[] { src.length };
		// *num = number of programs
		cl_program program = clCreateProgramWithSource(context, num[0], src, null, null);
		clBuildProgram(program, 0, null, null, null, null);
		num[0] = 0;
		// *num = number of kernels
		cl_kernel[] knls;
		
		boolean hasNull = false;
		for (int i = 0; i < knlNames.length; i++)
			if (knlNames[i] == null) {
				hasNull = true;
				break;
			}
		if (knlNames == null || hasNull) {
			int len = (knlNames == null) ? 1 : knlNames.length;
			clCreateKernelsInProgram(program, 0, null, num);
			knls = new cl_kernel[num[0] + len - 1];
			clCreateKernelsInProgram(program, num[0], knls, null);
			
			num[0]--;
			
			// additional duplicates
			for (int i = 1; i < len; i++) {
				if (knlNames[i] != null)
					knls[num[0] + i] = clCreateKernel(program, knlNames[i], null);
			}
		} else {
			num = new int[] { knlNames.length };
			knls = new cl_kernel[num[0]];
			for (int i = 0; i < num[0]; i++)
				knls[i] = clCreateKernel(program, knlNames[i], null);
		}
		return knls;
	}

	/**
	 * Parse the program of a kernel into String[]s of types and names
	 * 
	 * @param kernel
	 *            the kernel, can be null if src and kernelName are not null
	 * @param srcript
	 *            the program script, if not applicable, filling in null will
	 *            get the script aumomatically
	 * @param pkName
	 *            the pointer to kernel name, if not applicable, filling in null
	 *            will get the name aumomatically
	 * @param types
	 *            types of the arguments
	 * @param names
	 *            names of the arguments
	 * @param modifiers
	 *            modifiers other than type
	 */
	static void parseKernelArgs(cl_kernel kernel, String[] script, String[] pkName, String[] types, String[] names,
			String[] modifiers) {
		long[] len = new long[1];

		String kernelName = pkName == null ? null : pkName[0];
		// if name not available, find the name
		if (kernelName == null) {
			if (kernel == null)
				throw new IllegalArgumentException("null kernel & name");
			// get name
			clGetKernelInfo(kernel, CL_KERNEL_FUNCTION_NAME, 0, null, len);
			byte[] bufn = new byte[(int) len[0]];
			clGetKernelInfo(kernel, CL_KERNEL_FUNCTION_NAME, len[0], Pointer.to(bufn), len);
			pkName[0] = kernelName = EZCL.byteArray2String(bufn).trim();
		}

		// if src not available, find the script
		String src = "";
		if (script == null) {
			if (kernel == null)
				throw new IllegalArgumentException("null kernel & src");
			// get program
			clGetKernelInfo(kernel, CL_KERNEL_PROGRAM, 0, null, len);
			cl_program[] bufp = new cl_program[(int) len[0]];
			clGetKernelInfo(kernel, CL_KERNEL_PROGRAM, len[0], Pointer.to(bufp), len);

			// getscript
			clGetProgramInfo(bufp[0], CL_PROGRAM_SOURCE, 0, null, len);
			byte[] bufs = new byte[(int) len[0]];
			clGetProgramInfo(bufp[0], CL_PROGRAM_SOURCE, len[0], Pointer.to(bufs), len);
			src = EZCL.byteArray2String(bufs);
		} else
			for (int i = 0; i < script.length; i++)
				src += script[i];

		// find function declaration, split into an array of arguments at ','
		// followed by 0 or more whitespace
		int i = src.indexOf(kernelName);
		if (i == -1)
			throw new IllegalArgumentException("kernel named \"" + kernelName + "\" is not found");
		i += kernelName.length() + 1;
		int j = src.indexOf(")", i);
		String[] arguments = src.substring(i, j).split(",\\s*");

		// parse type and name
		for (int k = 0; k < arguments.length; k++) {
			String s = arguments[k];
			boolean isPointer = false;
			// name starts at the last occurrence of '*' or whitespace
			int nameStart = -1;
			if ((nameStart = s.lastIndexOf('*')) != -1)
				isPointer = true;
			else
				nameStart = s.lastIndexOf(' ');
			names[k] = s.substring(nameStart + 1).trim();

			// find the previous whitespace
			nameStart--;
			while (s.charAt(nameStart) == ' ')
				nameStart--;

			int typeStart = s.lastIndexOf(' ', nameStart);

			// if there is no space before the type as in "(int a)"
			if (typeStart < 1)
				typeStart = 0;
			String primitiveT = s.substring(typeStart, nameStart + 1).trim();
			types[k] = isPointer ? primitiveT + '*' : primitiveT;
			modifiers[k] = s.substring(0, typeStart);
		}
	}

	/**
	 * Simply calls clSetKernelArg().
	 * 
	 * @param kernel
	 *            the kernel to be set
	 * @param index
	 *            the argument index
	 * @param size
	 *            the size of the argument
	 * @param ptr
	 *            the pointer to the argument
	 */
	public static void setKernelArguments(cl_kernel kernel, int index, int size, Pointer ptr) {
		clSetKernelArg(kernel, index, size, ptr);
	}

	/**
	 * Sets a single argument for the kernel represented by this EZKernel.
	 * Checks for argument type before setting the argument.
	 * 
	 * @param argument
	 *            the kernel argument to be set: primitive (or primitive array),
	 *            EZMem, cl_mem
	 * @param index
	 *            the argument index to be set
	 */
	public void setArgument(Object argument, int index) {
		if (index < 0 || index >= argCount)
			throw new IllegalArgumentException("bad argument index when setting argument: " + index);

		// check argument
		String msg = EZMem.isValidType(argTypes[index], argument);
		if (msg != null)
			throw new IllegalArgumentException("bad argument when setting argument \"" + argNames[index]
					+ "\" of kernel \"" + kernelName + "\": " + msg);

		if (argument.getClass().isArray())
			throw new IllegalArgumentException("cannot set an array as kernel argument \"" + argNames[index]
					+ "\", memory object wrapping needed");

		// trivial case: EZMem argument (cl_mem argument has length 8)
		int len = argument instanceof EZMem ? Sizeof.cl_mem : EZMem.getLength(argument);

		setKernelArguments(kernel, index, len, EZMem.getPointerTo(argument));
	}

	/**
	 * Sets all the arguments for the kernel represented by this EZKernel.
	 * Checks for argument count and ALL argument types before setting each of
	 * them.
	 * 
	 * @param arguments
	 *            the list of kernel arguments to be set: primitive (or
	 *            primitive array), EZMem, cl_mem
	 */
	public void setArguments(Object... arguments) {
		if (arguments.length > argCount)
			throw new IllegalArgumentException("too many arguments for kernel \"" + kernelName + "\": "
					+ arguments.length + ", expected " + argCount);
		else if (arguments.length < argCount)
			throw new IllegalArgumentException("too few arguments for kernel \"" + kernelName + "\": "
					+ arguments.length + ", expected " + argCount);

		for (int i = 0; i < arguments.length; i++) {
			// check each argument
			String msg = EZMem.isValidType(argTypes[i], arguments[i]);

			// if is not a pointer to primitive (length 1)
			if (arguments[i].getClass().isArray())
				throw new IllegalArgumentException("cannot set an array as kernel argument \"" + argNames[i]
						+ "\", memory object wrapping needed");

			if (msg != null)
				throw new IllegalArgumentException("bad argument when setting argument \"" + argNames[i]
						+ "\" of kernel \"" + kernelName + "\": " + msg);
		}

		for (int i = 0; i < arguments.length; i++) {
			// trivial case: EZMem argument (cl_mem argument has length 8)
			int len = arguments[i] instanceof EZMem ? Sizeof.cl_mem : EZMem.getLength(arguments[i]);
			Pointer p = EZMem.getPointerTo(arguments[i]);
			setKernelArguments(kernel, i, len, p);
		}
	}

	public cl_kernel getKernel() {
		return kernel;
	}

	public EZContext getContext() {
		return context;
	}

	public void setContext(EZContext context) {
		this.context = context;
	}

	public String getKernelName() {
		return kernelName;
	}

	@Override
	public String toString() {
		String s = "Kernel : " + kernel.toString().substring(9) + "\n";
		s += "Function:\t" + kernelName + "(";
		if (argCount != 0) {
			for (int i = 0; i < argCount - 1; i++)
				s += argModifiers[i] + " " + argTypes[i] + " " + argNames[i] + ", ";
			s += argModifiers[argCount - 1] + " " + argTypes[argCount - 1] + " " + argNames[argCount - 1] + ")\n";
		}
		if (context != null)
			s += "On context:\n" + EZCL.infoPadding(context.toString(), "\t", 1) + "\n";
		return s;
	}
}
