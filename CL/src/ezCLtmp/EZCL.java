package ezCLtmp;

import static org.jocl.CL.*;

import java.io.File;
import java.util.Scanner;

import org.jocl.*;

public class EZCL {
	/** all the platforms */
	private static EZPlatform[] platforms = null;
	/** all the contexts (one for each of the platforms) */
	public EZContext[] contexts = null;
	/** the command queue */
	cl_command_queue commandQueue = null;

	public static void main(String[] args) {
//		System.out.println(getCLInfo());
//		int[][] plat_dev = new int[2][];
//		plat_dev[0] = new int[] {0};
//		plat_dev[1] = new int[] {0};
//		//EZCL cl = new EZCL();
//		EZCL cl = new EZCL(plat_dev);
//		System.out.println(cl.getContextsInfo());
//		EZPlatform plat = new EZPlatform(EZCL.getPlatforms()[0]);
//		int[] arr = new int[] {0, 0};
//		int[][] ar = new int[][] {{0}, {0}};
//		//EZContext ct = new EZContext(plat, arr);
//		EZCL ec = new EZCL(ar);
//		System.out.println(EZCL.getCLInfo());
//		System.out.println(ec.getPlatforms()[0]);
	}
	
	public EZCL(int[][] plat_dev) {
		CL.setExceptionsEnabled(true);
		
		// encapsulate platforms
		cl_platform_id[] ids = EZCL.getPlatforms();
		platforms = new EZPlatform[ids.length];
		for (int i = 0; i < ids.length; i++)
			platforms[i] = new EZPlatform(ids[i]);
		
		createContexts(plat_dev);
		
		// roll back to start at 0
		for (int i = 0; i < plat_dev.length; i++)
			for (int j = 0; j < plat_dev[i].length; j++)
				plat_dev[i][j] = j;
		
		setCommandQueue(plat_dev);
		
		//EZCL.createCommandQueue(contexts[0].getContext(), new int[]{0, 1, 2});
		for (EZContext ct : contexts)
			System.out.println(ct);
	}

	public EZCL() {
		this(null);
	}

	/**
	 * Shows the platform and device info
	 */
	public static String getCLInfo() {
		String s = "=================================== CL  INFO ===================================\n";

		cl_platform_id platforms[] = getPlatforms();

		s += "Platform number: " + platforms.length + "\n\n";

		int i = 0;
		for (cl_platform_id platform : platforms) {
			s += "Platform #" + i + ":\n";
			s += "Name:\t\t" + getPlatInfo(platform, CL_PLATFORM_NAME);
			s += "Version:\t" + getPlatInfo(platform, CL_PLATFORM_VERSION);
			s += "Vender:\t\t" + getPlatInfo(platform, CL_PLATFORM_VENDOR);
			
			cl_device_id[] devices = getDevices(platform);
			s += "Device number: \t" + devices.length + "\n";
			int j = 0;
			for (cl_device_id device : devices) {
				s += "\tDevice #" + j + ":\n";
				s += "\tName:\t\t" + getDeviceInfo(device, CL_DEVICE_NAME);
				s += "\tVersion:\t" + getDeviceInfo(device, CL_DEVICE_VERSION);
//				s += "\tMax wGS:\t" + getDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
				s += "\tMax Comp. Unit:\t" + getDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS);
				s += "\tVender:\t\t" + getDeviceInfo(device, CL_DEVICE_VENDOR);
				
				s += "\n";
				j++;
			}

			s += "\n";
			i++;
		}
		s += "=================================== CL  INFO ===================================\n";
		return s;
	}

	/**
	 * Gets the platforms found
	 * @return platforms
	 */
	public static cl_platform_id[] getPlatforms() {
		// Obtain the number of platforms
		int numPlatformsArray[] = new int[1];
		clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];
		// Obtain platform IDs
		cl_platform_id platforms[] = new cl_platform_id[numPlatforms];
		clGetPlatformIDs(numPlatforms, platforms, null);
		return platforms;
	}

	/**
	 * Gets the devices found on the platform of the specified type
	 * @param platform from where the devices are gotten
	 * @param the type of devices gotten
	 * @return devices on the platform
	 */
	public static cl_device_id[] getDevices(cl_platform_id platform, int type) {
		// Obtain the number of devices for the platform
		int numDevicesArray[] = new int[1];
		clGetDeviceIDs(platform, -1, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];
		// Obtain device IDs
		cl_device_id devices[] = new cl_device_id[numDevices];
		clGetDeviceIDs(platform, type, numDevices, devices, null);
		return devices;
	}

	/**
	 * Gets all the devices found on the platform
	 * @param platform from where the devices are gotten
	 * @return devices on the platform
	 */
	public static cl_device_id[] getDevices(cl_platform_id platform) {
		return getDevices(platform, -1);
	}

	//	/**
	//	 * Create a EZCL for the platform-device pairs E.g. if plat_dev[0] = {0, 3}, the 0th and 3rd 
	//	 * device on platform 0 will be added to context[0].
	//	 * @param plat_dev plat_dev devices # added to the context on each of the devices
	//	 */
	//	public EZCL(int[][] plat_dev) {
	//		CL.setExceptionsEnabled(true);
	//		
	//		// update global info
	//		platforms = getPlatforms();
	//		devices = new cl_device_id[platforms.length][];
	//		for (int i = 0; i < platforms.length; i++)
	//			devices[i] = getDevices(platforms[i]);
	//		
	//		updateContexts(plat_dev);
	//	}
	//	
	//	/**
	//	 * Create a EZCL for all the platform-device pairs
	//	 * @param plat_dev plat_dev devices # added to the context on each of the devices
	//	 */
	//	public EZCL() {
	//		this(null);
	//	}
	//
	//	/**
	//	 * Updates the contexts of this EZCL instance. In each row of plat_dev are the devices added to
	//	 * that device. E.g. if plat_dev[0] = {0, 3}, the 0th and 3rd device on platform 0 will be added
	//	 * to context[0].
	//	 * @param plat_dev devices # added to the context on each of the devices
	//	 * @return error code, 0 if no error
	//	 */
	//	public int updateContexts(int[][] plat_dev) {
	//		cl_platform_id[] platforms = getPlatforms();
	//		
	//		// add all
	//		if (plat_dev == null)
	//			plat_dev = new int[platforms.length][];
	//		
	//		if (plat_dev.length > platforms.length)
	//			throw new IllegalArgumentException("too many platforms, max: " + platforms.length);
	//		
	//		int[] ret = new int[1];
	//		contexts = new cl_context[plat_dev.length];
	//		
	//		// for each platform
	//		for (int i = 0; i < plat_dev.length; i++) {
	//			int[] thisPlatform = plat_dev[i];
	//			
	//			cl_platform_id platform = platforms[i];
	//			cl_device_id[] devices = getDevices(platform);
	//			
	//			if (thisPlatform == null) {
	//				thisPlatform = new int[devices.length];
	//				for (int j = 0; j < devices.length; j++)
	//					thisPlatform[j] = j;
	//			}
	//			
	//			if (thisPlatform.length > devices.length)
	//				throw new IllegalArgumentException("too many (" + thisPlatform.length + 
	//						") devices to be added on platform " + i + ", max: " + devices.length);
	//			else if (thisPlatform.length == 0)
	//				throw new IllegalArgumentException("too few (" + thisPlatform.length + 
	//						") devices to be added on platform " + i + ", min: 1");
	//			
	//			// create the platform property
	//			cl_context_properties contextProperties = new cl_context_properties();
	//			contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
	//			
	//			// the devices on the platform that will be added to the context
	//			cl_device_id[] added = new cl_device_id[(plat_dev[i] == null) ? thisPlatform.length : plat_dev[i].length];
	//			
	//			// copy the specifed platform to added
	//			for (int d0 = 0; d0 < thisPlatform.length; d0++)
	//				if (thisPlatform[d0] >= devices.length)
	//					throw new IllegalArgumentException("no such device: " + thisPlatform[d0] +
	//							" on platform " + i + ", max: " + (devices.length - 1));
	//				else {
	//					// if no duplicate, add the device
	//					for (int d1 = d0 + 1; d1 < thisPlatform.length; d1++)
	//						if (thisPlatform[d0] == thisPlatform[d1])
	//							throw new IllegalArgumentException("duplicate devices: " + d0 + 
	//									" on platform " + i);
	//					added[d0] = devices[thisPlatform[d0]];
	//				}
	//			contexts[i] = clCreateContext(contextProperties, added.length, added, null, null, ret);
	//		}
	//		return ret[0];
	//	}
	//	
	//	/**
	//	 * Gets the specified context
	//	 * @param c the context index
	//	 * @return the context at index c
	//	 */
	//	public cl_context getContext(int c) {
	//		if (c >= contexts.length)
	//			throw new IllegalArgumentException("no such context: " + c + 
	//					", max: " + (contexts.length - 1));
	//		return contexts[c];
	//	}
	//	
		static String getContextInfo(cl_context context, int cons) {
			String s = "";
			// char length
			long[] len = new long[1];
			clGetContextInfo(context, cons, 0, null, len);
	
			if (cons == CL_CONTEXT_DEVICES) {
				Pointer p = null;
				cl_device_id[] dev = new cl_device_id[(int) len[0] / Sizeof.cl_device_id];
				
				p = Pointer.to(dev);
				
				// find where from
				clGetContextInfo(context, cons, len[0], p, len);
				boolean found = false;
				for (cl_platform_id plat : getPlatforms()) {
					for (cl_device_id device : getDevices(plat))
						if (dev[0].toString().equals(device.toString())) {
							s += "Associated:\t" + getPlatInfo(plat, CL_PLATFORM_NAME);
							found = true;
							break;
						}
					if (found) break;
				}
				
				// device info
				s += "Device number: \t" + dev.length + "\n";
				int j = 0;
				for (cl_device_id device : dev) {
					s += "\tDevice #" + j + ":\n";
					s += "\tName:\t\t" + getDeviceInfo(device, CL_DEVICE_NAME);
					
					s += "\n";
					j++;
				}
			}
			//s += charArray2String(str);
			return s;
		}

	/**
	 * @param platform
	 *            the platform
	 * @param cons
	 *            the field constant
	 * @return the platform info String
	 */
	static String getPlatInfo(cl_platform_id platform, int cons) {
		String s = "";
		// char length
		long[] len = new long[1];
		clGetPlatformInfo(platform, cons, 0, null, len);
		// info string
		char[] str = new char[(int) len[0]];
		Pointer p = Pointer.to(str);
		clGetPlatformInfo(platform, cons, len[0], p, len);
		s += charArray2String(str);
		return s + "\n";
	}
	
	/**
	 * @param device
	 *            the device
	 * @param cons
	 *            the field constant
	 * @return the device info String
	 */
	static String getDeviceInfo(cl_device_id device, int cons) {
		String s = "";
		// char length
		long[] len = new long[1];
		clGetDeviceInfo(device, cons, 0, null, len);
		// info string
		char[] str = new char[(int) len[0]];
		Pointer p = Pointer.to(str);
		clGetDeviceInfo(device, cons, len[0], p, len);
		if (cons == CL_DEVICE_MAX_COMPUTE_UNITS || cons == CL_DEVICE_MAX_WORK_GROUP_SIZE)
			s += (int)str[0];
		else
			s += charArray2String(str);
		return s + "\n";
	}

	/**
	 * 
	 * @param str
	 *            input string
	 * @return converted to string
	 */
	static String charArray2String(char[] str) {
		String s = "";
		for (char ch : str) {
			s += (char) ((ch & 0x00ff));
			s += (char) ((ch & 0xff00) >> 8);
		}
		return s;
	}
	
	/**
	 * Updates contexts of the EZCL instance. In each row of plat_dev are the devices added to
	 * that device. E.g. if plat_dev[0] = {0, 3}, the 0th and 3rd device on platform 0 will be 
	 * added to context[0]. If plat_dev[x] is empty, the platform will be skipped
	 * @param plat_dev devices # added to the context on each of the devices
	 */
	public void createContexts(int[][] plat_dev) {
		// add all
		if (plat_dev == null)
			plat_dev = new int[platforms.length][];
		
		if (plat_dev.length != platforms.length)
			throw new IllegalArgumentException("incorrect number of platforms: " + plat_dev.length + 
					", expected: " + platforms.length);
		
		// create contexts for each of the platform
		int j = 0;
		// number of platforms to be really added
		for (int i = 0; i < plat_dev.length; i++)
			if (plat_dev[i] == null || plat_dev[i].length != 0)
				j++;
		contexts = new EZContext[j];
		j = 0;
		for (int i = 0; i < plat_dev.length; i++) {
			// accepts only "add all" and normal platforms
			if (plat_dev[i] == null || plat_dev[i].length != 0)
				contexts[j++] = new EZContext(platforms[i], plat_dev[i]);
		}
	}
	
	/**
	 * Creates the command queue for the context's specified devices. Index contains the device #
	 * whose command queue is to be created
	 * @param context the context
	 * @param index device index in the context
	 * @return an array of command queues in order
	 */
	@SuppressWarnings("deprecation")
	public static cl_command_queue[] createCommandQueue(cl_context context, int[] index) {
		// get all devices associated with the context
		long[] len = new long[1];
		clGetContextInfo(context, CL_CONTEXT_DEVICES, 0, null, len);
		cl_device_id[] devices = new cl_device_id[(int) len[0] / Sizeof.cl_device_id];
		clGetContextInfo(context, CL_CONTEXT_DEVICES, len[0], Pointer.to(devices), len);
		
		// add all
		if (index == null) {
			index = new int[devices.length];
			for (int i = 0; i < index.length; i++)
				index[i] = i;
		}
		
		// check for out of bound and duplicate index and create cmdQ
		cl_command_queue[] queue = new cl_command_queue[index.length];
		for (int i = 0; i < index.length; i++) {
			if (index[i] >= devices.length)
				throw new IllegalArgumentException("device not found: " + index[i] + 
						", on context: " + getContextInfo(context, CL_CONTEXT_DEVICES).split("\t|\n")[1].trim() +
						", max: " + (devices.length - 1));
			for (int j = i + 1; j < index.length; j++)
				if (index[j] == index[i])
					throw new IllegalArgumentException("duplicate device index: " + index[i] + 
							", on context: " + getContextInfo(context, CL_CONTEXT_DEVICES).split("\t|\n")[1].trim());
			queue[i] = CL.clCreateCommandQueue(context, devices[index[i]], 0, null);
		}
		
		return queue;
		
	}
	
	/**
	 * Sets command queue for each of the EZContext
	 * @param index (context, device) array
	 */
	public void setCommandQueue(int[][] index) {
		if (index == null)
			index = new int[contexts.length][];
		
		int i = 0;
		for (EZContext context : contexts)
			context.setCommandQueue(createCommandQueue(context.getContext(), index[i++]));
	}
	
	/**
	 * Reads all the content from the file
	 * @param path file path
	 * @return content String
	 */
	private static String fromFile(String path) {
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
	
	/** 
	 * Sets kernels for contexts. Kernels specified by knlNames are built from src
	 * @param contexts the context index
	 * @param src source Strings
	 * @param knlNames the kernels to be built
	 */
	public void setKernels(int[] contexts, String[] src, String[] knlNames) {
		for (int i = 0; i < contexts.length; i++) {
			if (contexts[i] >= 0 && contexts[i] < this.contexts.length) {
				EZContext thisContext = this.contexts[contexts[i]];
				thisContext.setKernels(EZKernel.buildKernels(thisContext, src, knlNames));
			} else {
				throw new IllegalArgumentException("context not found: " + contexts[i] + 
						", max " + (this.contexts.length - 1));
			}
		}
	}
	
	/** 
	 * Sets kernels for contexts. Kernels specified by knlNames are built from src
	 * @param contexts the context
	 * @param src source Strings
	 * @param knlNames the kernels to be built
	 */
	public void setKernels(EZContext[] contexts, String[] src, String[] knlNames) {
		for (int i = 0; i < contexts.length; i++) {
				EZContext thisContext = contexts[i];
				thisContext.setKernels(EZKernel.buildKernels(thisContext, src, knlNames));
		}
	}
	
	/**
	 * Reads source code from file and creates all kernels for all contexts
	 * @param path file path
	 */
	public void setAllKernelsFromFile(String[] path) {
		String[] srcs = new String[path.length];
		for (int i = 0; i < path.length; i++)
			srcs[i] = fromFile(path[i]);
		
		int[] index = new int[contexts.length];
		for (int i = 0; i < contexts.length; i++)
			index[i] = i;
		
		setKernels(index, srcs, null);//new String[]{"sampleKernel"});
	}
	
	public void simpleTest() {
		int ctxt = 0;
		int queue = 0;
		cl_context context = contexts[ctxt].getContext();
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
        		3 * Sizeof.cl_float, Pointer.to(num[0]), null);
        arg[1] = CL.clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR ,
        		3 * Sizeof.cl_float, Pointer.to(num[1]), null);
        arg[2] = CL.clCreateBuffer(context, CL_MEM_READ_WRITE ,
        		3 * Sizeof.cl_float, null, null);
        
        cl_kernel kernel = contexts[ctxt].getKernels()[0].getKernel();
        clSetKernelArg(kernel, 0, 
                Sizeof.cl_mem, Pointer.to(arg[0]));
            clSetKernelArg(kernel, 1, 
                Sizeof.cl_mem, Pointer.to(arg[1]));
            clSetKernelArg(kernel, 2, 
                Sizeof.cl_mem, Pointer.to(arg[2]));
            
            long global_work_size[] = new long[]{3};
            long local_work_size[] = new long[]{1};
            
            cl_command_queue commandQueue = contexts[ctxt].getCommandQueue()[queue];
         // Execute the kernel
            System.out.println("*Executing kernel");
            clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
                global_work_size, local_work_size, 0, null, null);
            clEnqueueReadBuffer(commandQueue, arg[2], true, 0,
                    3 * Sizeof.cl_float, Pointer.to(num[2]), 0, null, null);
            
            for (float x : num[2])
                System.out.println(x);
	}
}
