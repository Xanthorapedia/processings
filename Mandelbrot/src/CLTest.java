import static org.jocl.CL.*;
import org.jocl.*;

public class CLTest {
	private static String programSource =
	        "__kernel void "+
	        "sampleKernel(__global const float *a,"+
	        "             __global const float *b,"+
	        "             __global float *c)"+
	        "{"+
	        "    int gid = get_global_id(0);"+
	        "    c[gid] = a[gid] / b[gid];"+
	        "}";
	
	public static void main(String[] args) {
		final int platformIndex = 0;
        final long deviceType = CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;
        
		CL.setExceptionsEnabled(true);
		System.out.println("*Finding platforms");
		// Obtain the number of platforms
        int numPlatformsArray[] = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        int numPlatforms = numPlatformsArray[0];

        // Obtain a platform ID
        cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
        clGetPlatformIDs(platforms.length, platforms, null);
        cl_platform_id platform = platforms[1];
        
        System.out.println("numPlat: " + numPlatforms);
        {
	        long[] len = new long[1];
	        clGetPlatformInfo(platform, CL_PLATFORM_NAME, 0, null, len);
	        char[] str = new char[(int) len[0]];
	        Pointer p = Pointer.to(str);
	        clGetPlatformInfo(platform, CL_PLATFORM_NAME, len[0], p, len);
	        for (char ch : str) {
	        	System.out.print((char)(ch & 0x00ff));
	        	System.out.print((char)((ch & 0xff00) >> 8));
	        }
	        System.out.println();
        }
        
        // Obtain the number of devices for the platform
        int numDevicesArray[] = new int[1];
        clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
        int numDevices = numDevicesArray[0];
        
        System.out.println("*Finding devices");
        // Obtain a device ID 
        cl_device_id devices[] = new cl_device_id[numDevices];
        clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
        cl_device_id device = devices[0];
        
        System.out.println("numDev: " + numDevices);
        for (int i = 0; i < numDevices; i++)
        {
        	device = devices[i];
	        long[] len = new long[1];
	        clGetDeviceInfo(device, CL.CL_DEVICE_NAME, 0, null, len);
	        char[] str = new char[(int) len[0]];
	        Pointer p = Pointer.to(str);
	        clGetDeviceInfo(device, CL_DEVICE_NAME, len[0], p, len);
	        for (long ch : str) {
	        	System.out.print((char)(ch & 0x00ff));
	        	System.out.print((char)((ch & 0x0ff00) >> 8));
	        }
	        System.out.println();
        }
        device = devices[0];
        
        System.out.println("*Creating context");
        // Initialize the context properties
        cl_context_properties contextProperties = new cl_context_properties();
        //contextProperties.addProperty(CL_CONTEXT_PLATFORM, platforms[0]);
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platforms[1]);
        // Create a context for the selected device
        int[] ret = new int[1];
        cl_context context = clCreateContext(
            contextProperties, 1, new cl_device_id[]{device}, 
            null, null, ret);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
        System.out.println("*Creating command queue");
        // Create a command-queue for the selected device
        @SuppressWarnings("deprecation")
		cl_command_queue commandQueue = CL.clCreateCommandQueue(context, device, 0, ret);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
        // allocating memory
        System.out.println("*Allocating mem");
        float num[][] = new float[3][3];
        num[0][0] = 0.1f;
        num[0][1] = 0.2f;
        num[0][2] = 0.3f;
        num[1][0] = 0.4f;
        num[1][1] = 0.5f;
        num[1][2] = 0.6f;
        num[2][0] = 0.7f;
        num[2][1] = 0.8f;
        num[2][2] = 0.9f;
        cl_mem[] arg = new cl_mem[3];
        arg[0] = CL.clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR ,
        		3 * Sizeof.cl_float, Pointer.to(num[0]), ret);
        arg[1] = CL.clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR ,
        		3 * Sizeof.cl_float, Pointer.to(num[1]), ret);
        arg[2] = CL.clCreateBuffer(context, CL_MEM_READ_WRITE ,
        		3 * Sizeof.cl_float, null, ret);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
        // program
        System.out.println("*Creating program");
        cl_program program = clCreateProgramWithSource(context,
                1, new String[]{ programSource }, null, ret);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
        System.out.println("*Building program");
        clBuildProgram(program, 0, null, null, null, null);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
        // Create the kernel
        System.out.println("*Creating kernel");
        cl_kernel kernel = clCreateKernel(program, "sampleKernel", ret);
        System.out.println("Sccess: " + (ret[0] == CL.CL_SUCCESS));
        
     // Set the arguments for the kernel
        System.out.println("*Setting args");
        clSetKernelArg(kernel, 0, 
            Sizeof.cl_mem, Pointer.to(arg[0]));
        clSetKernelArg(kernel, 1, 
            Sizeof.cl_mem, Pointer.to(arg[1]));
        clSetKernelArg(kernel, 2, 
            Sizeof.cl_mem, Pointer.to(arg[2]));
        
        // Set the work-item dimensions
        System.out.println("*Setting work-item dimensions");
        long global_work_size[] = new long[]{3};
        long local_work_size[] = new long[]{1};
        
     // Execute the kernel
        System.out.println("*Executing kernel");
        clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
            global_work_size, local_work_size, 0, null, null);
        
     // Read the output data
        System.out.println("*Reading data");
        System.out.println("*Setting work-item dimensions");
        clEnqueueReadBuffer(commandQueue, arg[2], true, 0,
           3 * Sizeof.cl_float, Pointer.to(num[2]), 0, null, null);
        
     // Release kernel, program, and memory objects
        clReleaseMemObject(arg[0]);
        clReleaseMemObject(arg[1]);
        clReleaseMemObject(arg[2]);
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
        for (float x : num[2])
        System.out.println(x);
	}

}
