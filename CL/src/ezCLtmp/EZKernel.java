package ezCLtmp;
import static org.jocl.CL.*;

import java.io.File;
import java.util.Scanner;

import org.jocl.*;

public class EZKernel {
	/** the cl_kernel obj */
	private cl_kernel kernel = null;
	/** the name of the kernel in source */
	private String kernelName = null;
	/** argument count */
	private int argCount = 0;
	/** argument types */
	private Class<?>[] argTypes = null;
	
	public EZKernel(cl_kernel kernel) {
		this.kernel = kernel;
		long[] len = new long[1];
		
		// get name 
		{
		clGetKernelInfo(this.kernel, CL_KERNEL_FUNCTION_NAME, 0, null, len);
		char[] buf = new char[(int) len[0]];
		clGetKernelInfo(this.kernel, CL_KERNEL_FUNCTION_NAME, len[0], Pointer.to(buf), len);
		kernelName = EZCL.charArray2String(buf).trim();
		}
		
		{
			clGetKernelInfo(this.kernel, CL_KERNEL_PROGRAM, 0, null, len);
			cl_program[] buf = new cl_program[(int) len[0]];
			clGetKernelInfo(this.kernel, CL_KERNEL_PROGRAM, len[0], Pointer.to(buf), len);
			
			clGetProgramInfo(buf[0], CL_PROGRAM_SOURCE, 0, null, len);
			char[] buf1 = new char[(int) len[0]];
			clGetProgramInfo(buf[0], CL_PROGRAM_SOURCE, len[0], Pointer.to(buf1), len);
			String sc = EZCL.charArray2String(buf1);
			int i = sc.indexOf(kernelName) + kernelName.length() + 1;
			int j = sc.indexOf(")");
			sc = sc.substring(i, j);
			int k = 0;
		}
		
		// get arg num 
		{
		clGetKernelInfo(kernel, CL_KERNEL_NUM_ARGS, 0, null, len);
		int[] buf = new int[(int) len[0]];
		clGetKernelInfo(kernel, CL_KERNEL_NUM_ARGS, len[0], Pointer.to(buf), len);
		argCount = buf[0];
		}
		
		argTypes = new Class<?>[argCount];
//		
//		
		String argName = getKernelArgInfo(kernel, 0, CL_KERNEL_ARG_NAME).trim();
//		
//		for (int i = 0; i < argCount; i++) {
//			String argType = getKernelArgInfo(kernel, i, CL_KERNEL_ARG_TYPE_NAME).trim();
//			// array name = "[" + initial in uppercase
//			if (argType.endsWith("*"))
//				argType = "[" + (char)(argType.charAt(0) - 32);
//			if(0 == 0)break;
//			try {
//				this.argTypes[i] = Class.forName(argType);
//			} catch (ClassNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
	
	static String getKernelArgInfo(cl_kernel kernel, int arg, int info) {
		long[] len = new long[1];
		clGetKernelArgInfo(kernel, arg, info, 0, null, len);
		char[] buf = new char[(int) len[0]];
		clGetKernelArgInfo(kernel, arg, info, len[0], Pointer.to(buf), len);
		return EZCL.charArray2String(buf);
	}
	
	/**
	 * Creates the program and build the kernels on the context from Strings in src. If knlNames
	 * is not null, kernels will be built in the order of their names in knlNames 
	 * @param context the context
	 * @param src an array of source code Strings
	 * @param knlNames names of kernels to build
	 * @return an array of build kernel (encapsulated in EZKernel)
	 */
	public static EZKernel[] buildKernels(EZContext context, String[] src, String[] knlNames) {
		cl_kernel[] kernels = buildKernels(context.getContext(), src, knlNames);
		if (kernels.length == 0)
			System.out.println("Warning: no kernel built on context:\n" + context);
		EZKernel[] ezKernels = new EZKernel[kernels.length];
		for (int i = 0; i < kernels.length; i++)
			ezKernels[i] = new EZKernel(kernels[i]);
		return ezKernels;
	}

	/**
	 * Creates the program and build the kernels on the context from Strings in src. If knlNames
	 * is not null, kernels will be built in the order of their names in knlNames 
	 * @param context the context
	 * @param src an array of source code Strings
	 * @param knlNames names of kernels to build
	 * @return an array of build kernel
	 */
	public static cl_kernel[] buildKernels(cl_context context, String[] src, String[] knlNames) {
		int[] num = new int[] {src.length};
		// *num = number of programs
		cl_program program = clCreateProgramWithSource(context,	num[0], src, null, null);
		clBuildProgram(program, 0, null, null, null, null);
		num[0] = 0;
		// *num = number of kernels
		cl_kernel[] knls;
		if (knlNames == null) {
			clCreateKernelsInProgram(program, 0, null, num);
			knls = new cl_kernel[num[0]];
			clCreateKernelsInProgram(program, num[0], knls, null);
		}
		else {
			num = new int[] {knlNames.length};
			knls = new cl_kernel[num[0]];
			for (int i = 0; i < num[0]; i++)
				knls[i] = clCreateKernel(program, knlNames[i], null);
		}
		
		//TODO clReleaseProgram(program);
		return knls;
	}
	
	public cl_kernel getKernel() {
		return kernel;
	}
}	
