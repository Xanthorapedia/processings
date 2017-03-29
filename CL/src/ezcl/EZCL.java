package ezcl;

import static org.jocl.CL.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import org.jocl.*;

public class EZCL {

	/** all the contexts (one for each of the platforms) */
	public ArrayList<EZContext> contexts;
	/** the command queue */
	cl_command_queue commandQueue;
	/** all the platforms */
	static cl_platform_id[] platforms;
	/** the the devices of each of the platforms */
	static cl_device_id[][] devices;

	/**
	 * updates the platforms and devices info at the start
	 */
	static {
		CL.setExceptionsEnabled(true);
		updatePlatform_DeviceInfo();
	}

	public static void main(String[] args) {
		String code = "" + "__kernel void addI(int i, __global float* j) {"
				+ "    j[get_global_id(0) * 20 + get_global_id(1)] += i + get_global_id(0) * 20 + get_global_id(1);"
				+ "}" + "__kernel void testb(char i, __global float* j) {" + "    j[get_global_id(0)] -= i;" + "}"
				+ "__kernel void jPlusEqI(__global float* i, global float* j) {"
				+ "    j[get_global_id(0)] += i[get_global_id(0)];" + "}";

		float[] arr314 = new float[200];
		float[] arr271 = new float[200];
		float[] arrR = new float[200];

		// preset argument
		for (int i = 0; i < arr314.length; i++)
			arr314[i] = 3.14f;

		// setup
//		EZCL cl = new EZCL();
//		cl.addContexts(new int[][] { {}, { 0 } });
//		EZContext nv = cl.contexts.get(0);
//
//		nv.addCommandQueues();
//		nv.addKernels(new String[] { code });
		EZContext nv = quickStart(code);
		nv.addMemoryObjects(arr314, arr271);

		// set memory objects
		EZMem mem314 = nv.getMemObjects(0);
		EZMem mem271 = nv.getMemObjects(1);

		// set arguments
		nv.setKernelArguments("addI", 1, mem314);
		nv.setKernelArguments("jPlusEqI", mem271, mem314);

		// run
		nv.runKernel(0, "addI", new long[] { 10, 20 });

		// sync arguments
		nv.mapBuffer(mem271, false);
		for (int i = 0; i < arr271.length; i++)
			arr271[i] = 2.71f;
		nv.unmapBuffer(mem271);

		// and run again
		nv.runKernel(0, "jPlusEqI", new long[] { 200 });

		// gets results
		nv.readBuffer(mem314, arrR);

		for (float f : arrR)
			System.out.println(f);
	}

	public EZCL() {
		contexts = new ArrayList<EZContext>();
	}

	/**
	 * Adds contexts to the EZCL instance. In each row of plat_dev are the
	 * devices added to that context. E.g. if plat_dev[0] = {0, 3}, the 0th and
	 * 3rd device on platform 0 will be added to the context list. If
	 * plat_dev[x] is empty, the platform will be skipped. If plat_dev[x] is
	 * null, all the devices on the platform will be added. If plat_dev is null,
	 * all found devices will be added. Platforms not present in plat_dev will
	 * be ignored.
	 * 
	 * @param plat_dev
	 *            devices # added to the context on each of the platforms
	 */
	public void addContexts(int[]... plat_dev) {
		// add all
		if (plat_dev.length == 0)
			plat_dev = new int[platforms.length][];

		if (plat_dev.length > platforms.length)
			throw new IllegalArgumentException("too many platform rows when adding contexts: " + platforms.length
					+ ", max: " + (platforms.length - 1));

		// for each platform
		for (int i = 0; i < plat_dev.length; i++) {
			int[] row = plat_dev[i];
			cl_device_id[] shoppingCart;

			// if null, add all on this device
			if (row == null) {
				shoppingCart = new cl_device_id[devices[i].length];
				for (int j = 0; j < shoppingCart.length; j++)
					shoppingCart[j] = devices[i][j];
			} else {
				// skip empty contexts
				if (row.length == 0)
					continue;
				shoppingCart = new cl_device_id[row.length];
				for (int j = 0; j < shoppingCart.length; j++) {
					if (row[j] >= devices[i].length)
						throw new IndexOutOfBoundsException(
								"device index out of bound when adding contexts on platform " + i + ": " + row[j]
										+ ", max: " + (devices[i].length - 1));
					shoppingCart[j] = devices[i][row[j]];
				}
			}
			EZContext newContext = new EZContext(platforms[i], shoppingCart);
			if (!contexts.contains(newContext))
				contexts.add(newContext);
		}
	}

	/**
	 * Adds command queues to the EZCL instance. In each row of plat_dev are the
	 * devices added to that queue. E.g. if cont_dev[0] = {0, 3}, the 0th and
	 * 3rd device on context 0 will be added to the command queue list. If
	 * cont_dev[x] is empty, the context will be skipped. If cont_dev[x] is
	 * null, all the devices in the context will be added. If cont_dev is null,
	 * all found devices will be added. Context not present in cont_dev will be
	 * ignored.
	 * 
	 * @param cont_dev
	 *            devices # added to the command queue list on each of the
	 *            contexts
	 */
	public void addCommandQueues(int[]... cont_dev) {
		// add all
		if (cont_dev.length == 0)
			cont_dev = new int[contexts.size()][];

		if (cont_dev.length > contexts.size())
			throw new IllegalArgumentException("too many context rows when adding contexts: " + cont_dev.length
					+ ", max: " + (contexts.size() - 1));

		// for each context
		for (int i = 0; i < cont_dev.length; i++)
			contexts.get(i).addCommandQueues(cont_dev[i]);
	}

	/**
	 * Adds kernels specified by kernelNames[i] and built from src to
	 * contexts[i].
	 * <p>
	 * If kernelNames[i] is null, contexts[i] will adds all the kernels found in
	 * src. If kernelNames is null, all the kernels will be built on each of the
	 * contexts.
	 * <p>
	 * For devices that does not support fp64, be careful when using this method
	 * to create kernels on their contexts from the same sources, or use
	 * {@link #addKernels(String[][], String[][]) addKernels(String[][],
	 * String[][])} instead to indicate which set of src String is to be built
	 * on each of the contexts, or call this method multiple times for creating
	 * kernels from different sources and on different contexts.
	 * 
	 * @param src
	 *            program source, an array of code Strings
	 * @param kernelNames
	 *            the names of the kernel that will be built on each of the
	 *            contexts
	 */
	public void addKernels(String src[], String[][] kernelNames) {
		// add all
		if (kernelNames == null)
			kernelNames = new String[contexts.size()][];

		if (kernelNames.length > contexts.size())
			throw new IllegalArgumentException("too many context rows when adding kernels: " + kernelNames.length
					+ ", max: " + (contexts.size() - 1));

		// for each context
		for (int i = 0; i < kernelNames.length; i++)
			contexts.get(i).addKernels(src, kernelNames[i]);
	}

	/**
	 * Adds kernels specified by kernelNames[i] and built from src[i] to
	 * contexts[i].
	 * <p>
	 * If kernelNames[i] is null, contexts[i] will adds all the kernels found in
	 * src. If kernelNames is null, all the kernels will be built on each of the
	 * contexts.
	 * <p>
	 * To create kernels on different contexts from the same set of sources, use
	 * {@link #addKernels(String[], String[][]) addKernels(String[],
	 * String[][])}, except that the code contains fp64 variables and some of
	 * contexts specified by kernelNames does not support such types.
	 * 
	 * @param src
	 *            program source, an array of code String arrays
	 * @param kernelNames
	 *            the names of the kernel that will be built on each of the
	 *            contexts
	 */
	public void addKernels(String src[][], String[][] kernelNames) {
		// add all
		if (kernelNames == null)
			kernelNames = new String[contexts.size()][];

		if (kernelNames.length > contexts.size())
			throw new IllegalArgumentException("too many context rows when adding kernels: " + kernelNames.length
					+ ", max: " + (contexts.size() - 1));

		// for each context
		for (int i = 0; i < kernelNames.length; i++)
			contexts.get(i).addKernels(src[i], kernelNames[i]);
	}

	/**
	 * Adds memory objects to the EZCL instance. In each row of mems are the
	 * primitive arrays added to that context. E.g. all arrays in mems[0] will
	 * be built on context 0. mems and the arrays in it cannot be null, and
	 * empty context row will be skipped. Context not present in cont_dev will
	 * be ignored.
	 * 
	 * @param mems
	 *            primitive arrays to be added to the contexts
	 */
	public void addMemObjects(Object[][] mems) {
		if (mems == null)
			throw new IllegalArgumentException("null memory when adding memory objects");

		if (mems.length > contexts.size())
			throw new IllegalArgumentException("too many context rows when adding memory objcets: " + mems.length
					+ ", max: " + (contexts.size() - 1));

		// for each context
		for (int i = 0; i < mems.length; i++) {
			contexts.get(i).addMemoryObjects(mems[i]);
		}
	}

	/**
	 * Sets up a context on the first device of the first platform, adds its
	 * command queue and compile the kernel.
	 * 
	 * @param kernelCode
	 *            the source code
	 * @return a context that can set kernel arguments and run immediately
	 */
	public static EZContext quickStart(String kernelCode) {
		EZCL cl = new EZCL();
		cl.addContexts(new int[][] { { 0 } });
		EZContext context = cl.contexts.get(0);

		context.addCommandQueues();
		context.addKernels(new String[] { kernelCode });
		return context;
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
			s += platform2String(platform);

			s += "\n";
			i++;
		}
		s += "=================================== CL  INFO ===================================\n";
		return s;
	}

	/**
	 * Gets the platforms found directly
	 * 
	 * @return platforms
	 */
	public static cl_platform_id[] getPlatformsD() {
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
	 * Gets the platforms found
	 * 
	 * @return platforms
	 */
	public static cl_platform_id[] getPlatforms() {
		if (platforms == null)
			updatePlatform_DeviceInfo();
		return platforms;
	}

	/**
	 * Gets the devices found on the platform directly
	 * 
	 * @param platform
	 *            from where the devices are gotten
	 * @param the
	 *            type of devices gotten
	 * @return devices on the platform
	 */
	public static cl_device_id[] getDevicesD(cl_platform_id platform) {
		// Obtain the number of devices for the platform
		int numDevicesArray[] = new int[1];
		clGetDeviceIDs(platform, -1, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];
		// Obtain device IDs
		cl_device_id devices[] = new cl_device_id[numDevices];
		clGetDeviceIDs(platform, -1, numDevices, devices, null);
		return devices;
	}

	/**
	 * Gets the devices found on the platform directly
	 * 
	 * @param platform
	 *            from where the devices are gotten
	 * @param the
	 *            type of devices gotten
	 * @return devices on the platform, null if the platform is not found
	 */
	public static cl_device_id[] getDevices(cl_platform_id platform) {
		if (platforms == null || devices == null || platforms.length != devices.length)
			updatePlatform_DeviceInfo();
		for (int i = 0; i < platforms.length; i++) {
			if (platforms[i] == platform)
				return devices[i];
		}
		return null;
	}

	/**
	 * Updates the platforms and devices detected by EZCL
	 */
	public static void updatePlatform_DeviceInfo() {
		// Obtain the number of platforms
		int numPlatformsArray[] = new int[1];
		clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];
		// Obtain platform IDs
		platforms = new cl_platform_id[numPlatforms];
		clGetPlatformIDs(numPlatforms, platforms, null);
		// platforms = getPlatforms();

		devices = new cl_device_id[platforms.length][];
		for (int i = 0; i < platforms.length; i++) {
			// Obtain the number of devices for the platform
			int numDevicesArray[] = new int[1];
			clGetDeviceIDs(platforms[i], -1, 0, null, numDevicesArray);
			int numDevices = numDevicesArray[0];
			// Obtain device IDs
			devices[i] = new cl_device_id[numDevices];
			clGetDeviceIDs(platforms[i], -1, numDevices, devices[i], null);
			// devices[i] = getDevices(platforms[i]);
		}
	}

	/**
	 * Gets the String representation of a device including name, version, max
	 * compute units and vender
	 * 
	 * @param device
	 *            the device to be gotten
	 * @return the String representation
	 */
	public static String device2String(cl_device_id device) {
		String s = "";
		s += "\tName:\t\t" + getDeviceInfo(device, CL_DEVICE_NAME) + "\n";
		s += "\tVersion:\t" + getDeviceInfo(device, CL_DEVICE_VERSION) + "\n";
		s += "\tMax Comp. Unit:\t" + getDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS) + "\n";
		s += "\tVender:\t\t" + getDeviceInfo(device, CL_DEVICE_VENDOR) + "\n";
		return s;
	}

	/**
	 * Gets the String representation of a platform including name, version, max
	 * compute units and vender
	 * 
	 * @param device
	 *            the platform to be gotten
	 * @return the String representation
	 */
	public static String platform2String(cl_platform_id platform) {
		String s = "";
		s += "Name:\t\t" + getPlatInfo(platform, CL_PLATFORM_NAME) + "\n";
		s += "Version:\t" + getPlatInfo(platform, CL_PLATFORM_VERSION) + "\n";
		s += "Vender:\t\t" + getPlatInfo(platform, CL_PLATFORM_VENDOR) + "\n";

		cl_device_id[] devices = getDevices(platform);
		s += "Device number: \t" + devices.length + "\n";
		int j = 0;
		for (cl_device_id device : devices) {
			s += "\tDevice #" + j + ":\n";
			s += device2String(device) + "\n";
			j++;
		}
		return s;
	}

	/*****************************************************************************************/

	// /**
	// * Create a EZCL for the platform-device pairs E.g. if plat_dev[0] = {0,
	// 3}, the 0th and 3rd
	// * device on platform 0 will be added to context[0].
	// * @param plat_dev plat_dev devices # added to the context on each of the
	// devices
	// */
	// public EZCL(int[][] plat_dev) {
	// CL.setExceptionsEnabled(true);
	//
	// // update global info
	// platforms = getPlatforms();
	// devices = new cl_device_id[platforms.length][];
	// for (int i = 0; i < platforms.length; i++)
	// devices[i] = getDevices(platforms[i]);
	//
	// updateContexts(plat_dev);
	// }
	//
	// /**
	// * Create a EZCL for all the platform-device pairs
	// * @param plat_dev plat_dev devices # added to the context on each of the
	// devices
	// */
	// public EZCL() {
	// this(null);
	// }

	/**
	 * Gets the context info (needs casting into cl_device_id[] or int)
	 * <p>
	 * Supported constants:
	 * <li><tt>CL_CONTEXT_PLATFORM</tt> (cl_platform_id) the platform associated
	 * with this context (this is borrowed from clCreateContext())
	 * <li><tt>CL_CONTEXT_NUM_DEVICES</tt> (int) number of devices in the
	 * context
	 * <li><tt>CL_CONTEXT_DEVICES</tt> (cl_device_id[]) all the devices on the
	 * platform
	 * 
	 * @param context
	 *            the context
	 * @param cons
	 *            info to get
	 * @return the info Object
	 */
	static Object getContextInfo(cl_context context, int cons) {
		// special treatment for this abusive usage
		if (cons == CL_CONTEXT_PLATFORM) {
			cl_device_id[] buf = (cl_device_id[]) getContextInfo(context, CL_CONTEXT_DEVICES);
			return getDeviceInfo(buf[0], CL_DEVICE_PLATFORM);
		}

		// length
		long[] len = new long[1];
		clGetContextInfo(context, cons, 0, null, len);

		// get info
		switch (cons) {
		case CL_CONTEXT_NUM_DEVICES: {
			int[] buf = new int[(int) len[0] / Sizeof.cl_int];
			clGetContextInfo(context, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		case CL_CONTEXT_DEVICES: {
			cl_device_id[] buf = new cl_device_id[(int) len[0] / Sizeof.cl_device_id];
			clGetContextInfo(context, cons, len[0], Pointer.to(buf), len);
			return buf;
		}
		default:
			return null;
		}
	}

	/**
	 * Gets the platform info (needs casting into String)
	 * <p>
	 * Supported constants:
	 * <li><tt>CL_PLATFORM_PROFILE</tt> (String) as the name suggests
	 * <li><tt>CL_PLATFORM_VERSION</tt> (String) as the name suggests
	 * <li><tt>CL_PLATFORM_NAME</tt> (String) as the name suggests
	 * <li><tt>CL_PLATFORM_VENDOR</tt> (String) as the name suggests
	 * <li><tt>CL_PLATFORM_EXTENSIONS</tt> (String) features it supports such as
	 * fp64
	 * 
	 * @param platform
	 *            the platform
	 * @param cons
	 *            the field constant
	 * @return the platform info Object
	 */
	static Object getPlatInfo(cl_platform_id platform, int cons) {
		// char length
		long[] len = new long[1];
		clGetPlatformInfo(platform, cons, 0, null, len);

		// get info
		switch (cons) {
		case CL_PLATFORM_PROFILE:
		case CL_PLATFORM_VERSION:
		case CL_PLATFORM_NAME:
		case CL_PLATFORM_VENDOR:
		case CL_PLATFORM_EXTENSIONS: {
			byte[] buf = new byte[(int) len[0]];
			clGetPlatformInfo(platform, cons, len[0], Pointer.to(buf), len);
			return byteArray2String(buf);
		}
		default:
			return null;
		}
	}

	/**
	 * Gets the device info (needs casting into String, int, boolean,
	 * cl_platform_id or long)
	 * <p>
	 * Supported constants (I don't want to cpy & pst any more... Go google them
	 * or find in a cl handbook.):
	 * <li><tt>CL_DEVICE_EXTENSIONS</tt> (String)
	 * <li><tt>CL_DEVICE_NAME</tt> (String)
	 * <li><tt>CL_DEVICE_PROFILE</tt> (String)
	 * <li><tt>CL_DEVICE_VENDOR</tt> (String)
	 * <li><tt>CL_DEVICE_VERSION</tt> (String)
	 * <li><tt>CL_DRIVER_VERSION</tt> (String)
	 * 
	 * <li><tt>CL_DEVICE_AVAILABLE</tt> (boolean)
	 * <li><tt>CL_DEVICE_COMPILER_AVAILABLE</tt> (Boolean)
	 * <li><tt>CL_DEVICE_ENDIAN_LITTLE</tt> (Boolean)
	 * <li><tt>CL_DEVICE_ERROR_CORRECTION_SUPPORT</tt> (Boolean)
	 * <li><tt>CL_DEVICE_IMAGE_SUPPORT</tt> (Boolean)
	 * 
	 * <li><tt>CL_DEVICE_PLATFORM</tt>: (cl_platform_id)
	 * 
	 * <li><tt>CL_DEVICE_GLOBAL_MEM_CACHE_SIZE</tt>: (long)
	 * <li><tt>CL_DEVICE_GLOBAL_MEM_SIZE</tt>: (long)
	 * <li><tt>CL_DEVICE_LOCAL_MEM_SIZE</tt>: (long)
	 * <li><tt>CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE</tt>: (long)
	 * <li><tt>CL_DEVICE_MAX_MEM_ALLOC_SIZE</tt>: (long)
	 * <li>any other: (int)
	 * 
	 * @param device
	 *            the device
	 * @param cons
	 *            the field constant
	 * @return the device info Object
	 */
	static Object getDeviceInfo(cl_device_id device, int cons) {
		// length
		long[] len = new long[1];
		clGetDeviceInfo(device, cons, 0, null, len);

		// get info
		switch (cons) {
		case CL_DEVICE_EXTENSIONS:
		case CL_DEVICE_NAME:
		case CL_DEVICE_PROFILE:
		case CL_DEVICE_VENDOR:
		case CL_DEVICE_VERSION:
		case CL_DRIVER_VERSION: {
			byte[] buf = new byte[(int) len[0]];
			clGetDeviceInfo(device, cons, len[0], Pointer.to(buf), len);
			return byteArray2String(buf);
		}
		case CL_DEVICE_AVAILABLE:
		case CL_DEVICE_COMPILER_AVAILABLE:
		case CL_DEVICE_ENDIAN_LITTLE:
		case CL_DEVICE_ERROR_CORRECTION_SUPPORT:
		case CL_DEVICE_IMAGE_SUPPORT: {
			byte[] buf = new byte[(int) len[0]];
			clGetDeviceInfo(device, cons, len[0], Pointer.to(buf), len);
			return buf[0] != 0;
		}
		case CL_DEVICE_PLATFORM: {
			cl_platform_id[] buf = new cl_platform_id[(int) len[0] / Sizeof.cl_platform_id];
			clGetDeviceInfo(device, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		case CL_DEVICE_GLOBAL_MEM_CACHE_SIZE:
		case CL_DEVICE_GLOBAL_MEM_SIZE:
		case CL_DEVICE_LOCAL_MEM_SIZE:
		case CL_DEVICE_MAX_CONSTANT_BUFFER_SIZE:
		case CL_DEVICE_MAX_MEM_ALLOC_SIZE: {
			long[] buf = new long[(int) len[0] / Sizeof.cl_ulong];
			clGetDeviceInfo(device, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		default: {
			int[] buf = new int[(int) len[0] / Sizeof.cl_uint];
			clGetDeviceInfo(device, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		}
	}

	/**
	 * Gets the info of the command queue (needs casting into cl_context or
	 * cl_device_id).
	 * <p>
	 * Supported constants:
	 * <li><tt>CL_QUEUE_CONTEXT</tt>: (cl_context) the context containing the
	 * command queue
	 * <li><tt>CL_QUEUE_DEVICE</tt>: (cl_device_id[]) the devices on this
	 * command queue
	 * 
	 * @param queue
	 *            the command queue
	 * @param cons
	 *            the field constant
	 * @return the command queue info Object
	 */
	static Object getCommandQueueInfo(cl_command_queue queue, int cons) {
		long[] len = new long[1];
		clGetCommandQueueInfo(queue, cons, 0, null, len);

		// get info
		switch (cons) {
		case CL_QUEUE_CONTEXT: {
			cl_context[] buf = new cl_context[(int) len[0] / Sizeof.cl_context];
			clGetCommandQueueInfo(queue, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		case CL_QUEUE_DEVICE: {
			cl_device_id[] buf = new cl_device_id[(int) len[0] / Sizeof.cl_device_id];
			clGetCommandQueueInfo(queue, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		default:
			return null;
		}
	}

	/**
	 * Gets the info of the memObject (needs casting into long, int or
	 * cl_context.
	 * <p>
	 * Supported constants:
	 * <li><tt>CL_MEM_FLAGS</tt>: (long) access specifier
	 * <li><tt>CL_MEM_TYPE</tt>: (int) whether image or buffer
	 * <li><tt>CL_MEM_SIZE</tt>: (int) size of the buffer in bytes
	 * <li><tt>CL_MEM_CONTEXT</tt>: (cl_context) the context containing the
	 * memObj
	 * 
	 * @param memObj
	 *            a cl_mem memory object
	 * @param cons
	 *            field constant
	 * @return info Object
	 */
	static Object getMemObjectInfo(cl_mem memObj, int cons) {
		long[] len = new long[1];
		clGetMemObjectInfo(memObj, cons, 0, null, len);

		// get info
		switch (cons) {
		case CL_MEM_FLAGS: {
			long[] buf = new long[(int) len[0] / Sizeof.cl_ulong];
			clGetMemObjectInfo(memObj, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		case CL_MEM_TYPE:
		case CL_MEM_SIZE: {
			int[] buf = new int[(int) len[0] / Sizeof.cl_uint];
			clGetMemObjectInfo(memObj, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		case CL_MEM_CONTEXT: {
			cl_context[] buf = new cl_context[(int) len[0] / Sizeof.cl_context];
			clGetMemObjectInfo(memObj, cons, len[0], Pointer.to(buf), len);
			return buf[0];
		}
		// case CL_MEM_HOST_PTR: {
		// IntBuffer buf = IntBuffer.allocate((int) len[0]);
		// clGetMemObjectInfo(memObj, cons, len[0], Pointer.to(buf), len);
		// return buf;
		// }
		default:
			return null;
		}
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
	 * 
	 * @param str
	 *            input string
	 * @return converted to string
	 */
	static String byteArray2String(byte[] input) {
		String s = "";
		for (byte ch : input)
			s += (char) ch;
		return s;
	}

	/**
	 * 
	 * @param str
	 *            input byte string
	 * @return converted to number
	 */
	static long byteArray2Num(byte[] input) {
		long l = 0;
		for (int i = 0; i < 8 && i < input.length; i++)
			l |= ((((int) input[i]) & 255) << (i << 3));
		return l;
	}

	/**
	 * Pads at the start each line of the String with num * content
	 * 
	 * @param input
	 *            input String
	 * @param content
	 *            padded String
	 * @param num
	 *            padding for this many times
	 * @return padded String
	 */
	static String infoPadding(String input, String content, int num) {
		String ret = "";
		String[] lines = input.split("\n");
		for (String line : lines) {
			for (int i = 0; i < num; i++)
				ret += content;
			ret += line + "\n";
		}
		return ret;
	}

	/*****************************************************************************************/

	/**
	 * Reads all the content from the file
	 * 
	 * @param path
	 *            file path
	 * @return content String
	 */
	public static String fromFile(String path) {
		String ret = "";
		try {
			Scanner in = new Scanner(new File(path));
			while (in.hasNextLine())
				ret += in.nextLine() + "\n";
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
}
