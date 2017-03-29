package ezcl;

import static org.jocl.CL.*;

import java.nio.ByteBuffer;

import org.jocl.*;

public class EZCommandQueue {

	/** the context associated with this command queue */
	private EZContext context;
	/** the device associated with this command queue */
	private cl_device_id device;
	/** the command queue represented by this instance */
	private cl_command_queue queue;

	/**
	 * Creates a command queue on the context with the device
	 * 
	 * @param context
	 *            the context associated with the queue
	 * @param device
	 *            the device associated with the queue
	 */
	public EZCommandQueue(cl_context context, cl_device_id device) {
		this.device = device;
		this.queue = createCommandQueue(context, device);
	}

	/**
	 * Encapsulates a existing command queue in EZCommandQueue
	 * 
	 * @param queue
	 *            the existing command queue
	 */
	public EZCommandQueue(cl_command_queue queue) {
		this.device = (cl_device_id) EZCL.getCommandQueueInfo(queue, CL_QUEUE_DEVICE);
		this.queue = queue;
	}

	/**
	 * Updates contexts of the EZCL instance. In each row of plat_dev are the
	 * devices added to that device. E.g. if plat_dev[0] = {0, 3}, the 0th and
	 * 3rd device on platform 0 will be added to context[0]. If plat_dev[x] is
	 * empty, the platform will be skipped
	 * 
	 * @param plat_dev
	 *            devices # added to the context on each of the devices
	 */

	/**
	 * Creates the command queue for the context's specified devices. Index
	 * contains the device # whose command queue is to be created
	 * 
	 * @param context
	 *            the context
	 * @param index
	 *            device index in the context
	 * @return an array of command queues in order
	 */
	@SuppressWarnings("deprecation")
	public static cl_command_queue createCommandQueue(cl_context context, cl_device_id device) {
		return clCreateCommandQueue(context, device, 0, null);

	}

	/**
	 * Runs a kernel on the command queue. gSize specifies the global_work_size
	 * of the NDRange, while local_work_size is determined by lSize. Both arrays
	 * should contain size of each dimension (or lSize can be null). The number
	 * of dimensions is specified by dim.
	 * 
	 * @param cmdQ
	 *            the command queue to run the kernel
	 * @param kernel
	 *            the kernel program to be run
	 * @param dim
	 *            the number of working dimensions
	 * @param gSize
	 *            the global size of the working space
	 * @param lSize
	 *            the local size of the working space
	 */
	public static void runKernel(cl_command_queue cmdQ, cl_kernel kernel, int dim, long[] gSize, long[] lSize) {
		if (gSize == null)
			throw new IllegalArgumentException("null global_work_size or local_work_size");
		if (gSize.length != dim || (lSize != null && lSize.length != dim))
			throw new IllegalArgumentException("global_work_size or local_work_size does not match dim");
		// global_work_size should be divisible by local_work_size
		for (int i = 0; lSize != null && i < dim; i++)
			if (gSize[i] % lSize[i] != 0)
				throw new IllegalArgumentException("global_work_size, local_work_size dimension mismatch");

		clEnqueueNDRangeKernel(cmdQ, kernel, dim, null, gSize, lSize, 0, null, null);
	}

	/**
	 * Simply calls clEnqueueReadBuffer to read buffer from buffer to the array
	 * pointed by ptr.
	 * 
	 * @param cmdQ
	 *            the command queue in a context
	 * @param buffer
	 *            the buffer on the same context
	 * @param size
	 *            reading size (bytes)
	 * @param ptr
	 *            array buffer
	 */
	public static void readBuffer(cl_command_queue cmdQ, cl_mem buffer, long size, Object ptr) {
		int lenP = EZMem.getLength(ptr);
		int lenB = EZMem.getLength(buffer);
		if (lenP < size)
			throw new IllegalArgumentException(
					"no enough space in ptr (" + lenP + ") to hold " + size + " bytes from buffer");
		if (lenB < size)
			throw new IllegalArgumentException("no enough bytes in buffer (" + lenB + ") to write (" + size + ")");
		clEnqueueReadBuffer(cmdQ, buffer, true, 0, size, EZMem.getPointerTo(ptr), 0, null, null);
	}

	/**
	 * Simply calls clEnqueueWriteBuffer to write array to the buffer pointed by
	 * ptr.
	 * 
	 * @param cmdQ
	 *            the command queue in a context
	 * @param buffer
	 *            the buffer in the same context
	 * @param size
	 *            writing size (bytes)
	 * @param ptr
	 *            array buffer
	 */
	public static void writeBuffer(cl_command_queue cmdQ, cl_mem buffer, long size, Object ptr) {
		int lenP = EZMem.getLength(ptr);
		int lenB = EZMem.getLength(buffer);
		if (lenB < size)
			throw new IllegalArgumentException(
					"no enough space in buffer (" + lenB + ") to hold " + size + " bytes from ptr");
		if (lenP < size)
			throw new IllegalArgumentException("no enough bytes in ptr (" + lenP + ") to write (" + size + ")");
		clEnqueueWriteBuffer(cmdQ, buffer, true, 0, size, EZMem.getPointerTo(ptr), 0, null, null);
	}

	/**
	 * Maps the memory object of size to the array buffer associated with the
	 * object. The local array buffer gets access to read the buffer if
	 * CL_MAP_READ flag is enabled. The memory object gets access to write the
	 * buffer if CL_MAP_WRITE flag is enabled.
	 * 
	 * @param cmdQ
	 *            the command queue in a context
	 * @param buffer
	 *            the buffer on the same context
	 * @param flags
	 *            mapping flags
	 * @param size
	 *            mapping size
	 * @param ptr
	 *            the pointer to map to
	 * @return a pointer to the mapped region
	 */
	public static ByteBuffer mapBuffer(cl_command_queue cmdQ, cl_mem buffer, long flags, long size) {
		return clEnqueueMapBuffer(cmdQ, buffer, true, flags, 0, size, 0, null, null, null);
	}

	/**
	 * Unmaps the memory object. The memory object will be rewritten if
	 * CL_MAP_WRITE flag is enabled.
	 * 
	 * @param cmdQ
	 *            the command queue in a context
	 * @param buffer
	 *            the buffer on the same context
	 * @param ptr
	 *            the pointer to the mapped region
	 */
	public static void unmapBuffer(cl_command_queue cmdQ, cl_mem buffer, ByteBuffer ptr) {
		CL.clEnqueueUnmapMemObject(cmdQ, buffer, ptr, 0, null, null);
	}

	/**
	 * Runs kernel in this command queue
	 * 
	 * @param knl
	 *            the kernel in the same context
	 * @param gSize
	 *            the global size of the working space
	 * @param lSize
	 *            the local size of the working space
	 */
	public void runKerel(EZKernel knl, long[] gSize, long[] lSize) {
		runKernel(queue, knl.getKernel(), gSize.length, gSize, lSize);
	}

	/**
	 * Reads the whole buffer (sizeof mem) to array contained by obj
	 * 
	 * @param mem
	 *            an EZMem object
	 * @param obj
	 *            an array buffer
	 */
	public void readWholeBuffer(EZMem mem, Object obj) {
		int type = EZMem.getObjType(obj);
		if (type < 12)
			throw new IllegalArgumentException("cannot read to a primitive type or cl_mem buffer");
		readBuffer(queue, mem.getMemory(), mem.getSize(), obj);
	}

	/**
	 * Writes the whole buffer (sizeof obj) to buffer mem
	 * 
	 * @param mem
	 *            an EZMem object
	 * @param obj
	 *            an array buffer
	 */
	public void writeWholeBuffer(EZMem mem, Object obj) {
		int type = EZMem.getObjType(obj);
		if (type < 12)
			throw new IllegalArgumentException("cannot write from a primitive type or cl_mem buffer");
		writeBuffer(queue, mem.getMemory(), mem.getSize(), obj);
	}

	/**
	 * Maps the memory object of size to the array buffer associated with the
	 * object. The local array buffer will be rewritten in the call. The memory
	 * object will be rewritten when calling unmap if readOnly = false.
	 * 
	 * @param buffer
	 *            the buffer to be mapped to a local address
	 * @param read
	 *            if the mapping is readable
	 * @param write
	 *            if the mapping is writable
	 * @return a pointer to the mapped region
	 */
	public ByteBuffer mapWholeBuffer(EZMem buffer, boolean readOnly) {
		if (buffer.getMappedPtr() != null)
			throw new IllegalArgumentException("the buffer has already been mapped: " + buffer);
		
		long flags = CL_MAP_READ;
		flags |= readOnly ? 0 : CL_MAP_WRITE;
		cl_mem memory = buffer.getMemory();
		
		// link to the buffer EZMem
		ByteBuffer bb = mapBuffer(queue, memory, flags, EZMem.getLength(memory));
		buffer.setMappedPtr(bb);
		return bb;
	}

	/**
	 * Unmaps the buffer
	 * 
	 * @param buffer
	 *            the mem object
	 * @param ptr
	 *            a pointer to the mapped region
	 */
	public void unmapBuffer(EZMem buffer) {
		ByteBuffer bb = buffer.getMappedPtr();
		if (bb == null)
			throw new IllegalArgumentException("the buffer has not been mapped: " + buffer);
		
		buffer.setMappedPtr(null);
		unmapBuffer(queue, buffer.getMemory(), bb);
	}

	public cl_command_queue getQueue() {
		return queue;
	}

	public void setQueue(cl_command_queue queue) {
		this.queue = queue;
	}

	public EZContext getContext() {
		return context;
	}

	public void setContext(EZContext context) {
		this.context = context;
	}

	@Override
	public String toString() {
		String s = "Command queue : " + queue.toString().substring(16) + "\n";
		s += "Associated device:\t" + EZCL.getDeviceInfo(device, CL_DEVICE_NAME) + ": "
				+ device.toString().substring(12) + "\n";
		if (context != null)
			s += "On context:\n" + EZCL.infoPadding(context.toString(), "\t", 1) + "\n";
		return s;
	}

}
