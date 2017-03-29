import static org.jocl.CL.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Scanner;

import org.jocl.*;

public class EZCL {
	
	public static cl_command_queue commandQueue;
	public static cl_context context;
	private static String source =
	        "__kernel void "+
	        "sampleKernel(__global double *a,"+
	        "             __global const double *b,"+
	        "             __global double *c)"+
	        "{"+
	        "    int gid = get_global_id(0);"+
	        "    c[gid] = a[gid] * b[gid];"+
	        "}";

	public static void main(String[] args) {
		initCL();
		cl_kernel knl = buildKernels(new String[]{source}, new String[]{"sampleKernel"})[0];
		double num[][] = new double[3][3];
        num[0][0] = 0.1;
        num[0][1] = 0.2;
        num[0][2] = 0.3;
        num[1][0] = 0.4;
        num[1][1] = 0.5;
        num[1][2] = 0.6;
        num[2][0] = 0.7;
        num[2][1] = 0.8;
        num[2][2] = 0.9;
        cl_mem[] arg = new cl_mem[3];
        arg[0] = CL.clCreateBuffer(context, CL_MEM_READ_WRITE| CL_MEM_USE_HOST_PTR ,
        		3 * Sizeof.cl_double, Pointer.to(num[0]), null);
        arg[1] = CL.clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_USE_HOST_PTR ,
        		3 * Sizeof.cl_double, Pointer.to(num[1]), null);
        arg[2] = CL.clCreateBuffer(context, CL_MEM_READ_WRITE ,
        		3 * Sizeof.cl_double, null, null);
        ByteBuffer a = CL.clEnqueueMapBuffer(commandQueue, arg[2], true, CL_MAP_WRITE | CL_MAP_READ, 0, 3 * Sizeof.cl_double, 0, null, null, null);
		setKnlArg1(knl, arg[0], arg[1], arg[2]);
		executeKernel(knl, 1, new long[]{3}, new long[]{1});
		//readBuf(arg[2], 3 * Sizeof.cl_double, Pointer.to(num[2]));
		for (double d : num[2])
			System.out.println(d);
		
		ByteBuffer b = CL.clEnqueueMapBuffer(commandQueue, arg[0], true, CL_MAP_WRITE | CL_MAP_READ, 0, 3 * Sizeof.cl_double, 0, null, null, null);
		num[0][0] = 0.2;
		num[0][1] = 0.3;
		CL.clEnqueueUnmapMemObject(commandQueue, arg[0], b, 0, null, null);
		CL.clEnqueueUnmapMemObject(commandQueue, arg[2], a, 0, null, null);
//		arg[0] = CL.clCreateBuffer(context, CL_MEM_READ_WRITE| CL_MEM_USE_HOST_PTR ,
//        		3 * Sizeof.cl_double, Pointer.to(num[0]), null);
		
		//setKnlArg1(knl, arg[0], arg[1], arg[2]);
		executeKernel(knl, 1, new long[]{3}, new long[]{1});
		readBuf(arg[2], 3 * Sizeof.cl_double, Pointer.to(num[2]));
		for (double d : num[2])
		System.out.println(d);

	}
	
	public static boolean initCL() {
		final int PLATFORM_NO = 1;
		final int DEVICE_NO = 0;
		
		CL.setExceptionsEnabled(true);
        
		// get plat 1: NV
		cl_platform_id platforms[] = new cl_platform_id[2];
		clGetPlatformIDs(2, platforms, null);
		cl_platform_id platform = platforms[PLATFORM_NO];
        
        //get device 1: 740M
		cl_device_id devices[] = new cl_device_id[1];
		clGetDeviceIDs(platform, CL_DEVICE_TYPE_ALL, 1, devices, null);
		cl_device_id device = devices[DEVICE_NO];
		
        // create device context on the platform
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
		// Create a context for the selected device
		int[] ret = new int[1];
		context = clCreateContext(
				contextProperties, 1, new cl_device_id[]{device}, 
				null, null, ret);
        
        // create command queue
        commandQueue = CL.clCreateCommandQueue(context, device, 0, ret);
        
        return ret[0] == 0;
	}
	
	public static cl_kernel[] buildKernels(String[] src, String[] knlNames) {
		int[] ret = new int[1];
		int[] num = new int[] {src.length};
		
		// *num = number of programs
		cl_program program = clCreateProgramWithSource(context,
				num[0], src, null, ret);
		clBuildProgram(program, 0, null, null, null, null);
		
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
				knls[i] = clCreateKernel(program, knlNames[i], ret);
		}
		
		clReleaseProgram(program);
		return knls;
	}
	
	public static void setKnlArg(cl_kernel kernel, cl_mem[] memObj, Object...list) {
//		clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memObj[0]));
//		clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memObj[1]));
//		clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memObj[2]));
//		clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memObj[3]));
//		clSetKernelArg(kernel, 4, Sizeof.cl_mem, Pointer.to(memObj[4]));
//		clSetKernelArg(kernel, 5, Sizeof.cl_mem, Pointer.to(memObj[5]));
//		clSetKernelArg(kernel, 6, Sizeof.cl_mem, Pointer.to(memObj[6]));
		for (int i = 0; i < list.length; i++)
			clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(memObj[i]));
	}
	
	public static void setKnlArg1(cl_kernel kernel, Object...list) {
		for (int i = 0; i < list.length; i++) {
			Object cur = list[i];
			Class<?> cls = cur.getClass();
			
			if (cls.equals(Double.class))
				clSetKernelArg(kernel, i, Sizeof.cl_double, Pointer.to(new double[]{(double) cur}));
			else if (cls.equals(Integer.class))
				clSetKernelArg(kernel, i, Sizeof.cl_int, Pointer.to(new int[]{(int) cur}));
			else if (cls.equals(cl_mem.class))
				clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to((cl_mem)cur));
		}
	}
	
	public static int executeKernel(cl_kernel kernel, int dim, long[] global_work_size, long[] local_work_size) {
		return clEnqueueNDRangeKernel(commandQueue, kernel, dim, null, global_work_size, local_work_size,
				0, null, null);
	}
	
	public static int readBuf(cl_mem buf, long size, Pointer dst) {
		return clEnqueueReadBuffer(commandQueue, buf, true, 0, size, dst, 0, null, null);
	}
	
	public static String fromFile(String path) {
		String ret = "";
		try {
			Scanner in = new Scanner(new File(path));
			while(in.hasNextLine())
				ret += in.nextLine() + "\n";
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}
