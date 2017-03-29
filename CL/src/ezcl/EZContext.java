package ezcl;

import static org.jocl.CL.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.jocl.*;

public class EZContext {

	/** the contexts represented by this instance */
	private cl_context context;
	/** the platform associated with this context */
	private cl_platform_id platform;
	/** the devices in this context */
	private cl_device_id[] devices;
	/** the command queues associated with the devices */
	private ArrayList<EZCommandQueue> commandQueues;
	/** the kernels available to the context */
	private ArrayList<EZKernel> kernels;
	/** the memory objects on the context */
	private ArrayList<EZMem> memories;

	/**
	 * Creates a EZContext on the platform with devices
	 * 
	 * @param platform
	 *            the associated platform
	 * @param devices
	 *            the associated devices
	 */
	public EZContext(cl_platform_id platform, cl_device_id[] devices) {
		this.context = createContexts(platform, devices);
		this.platform = platform;
		this.devices = devices == null ? EZCL.getDevices(platform) : devices;
		commandQueues = new ArrayList<EZCommandQueue>();
		kernels = new ArrayList<EZKernel>();
		memories = new ArrayList<EZMem>();
	}

	/**
	 * Encapsulates a existing context in an EZContext instance
	 * 
	 * @param context
	 */
	public EZContext(cl_context context) {
		this.context = context;
		this.platform = (cl_platform_id) EZCL.getContextInfo(context, CL_CONTEXT_PLATFORM);
		this.devices = (cl_device_id[]) EZCL.getContextInfo(context, CL_CONTEXT_DEVICES);
		commandQueues = new ArrayList<EZCommandQueue>();
		kernels = new ArrayList<EZKernel>();
		memories = new ArrayList<EZMem>();
	}

	/**
	 * Creates a cl_context on the platform with devices
	 * 
	 * @param platform
	 *            the platform associated with the context
	 * @param devices
	 *            associated devices
	 * @return the context created
	 */
	public static cl_context createContexts(cl_platform_id platform, cl_device_id[] devices) {
		if (devices == null)
			devices = EZCL.getDevices(platform);
		for (int i = 0; i < devices.length; i++)
			for (int j = i + 1; j < devices.length; j++)
				if (devices[i] == devices[j])
					throw new IllegalArgumentException("duplicate device: " + i);
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

		return clCreateContext(contextProperties, devices.length, devices, null, null, null);
	}

	/**
	 * Creates command queues based on index of the devices on the context. Null
	 * index adds all
	 * 
	 * @param index
	 *            device index on the context
	 */
	public void addCommandQueues(int... index) {
		// null = add all
		if (index.length == 0 || index == null) {
			index = new int[devices.length];
			for (int j = 0; j < devices.length; j++)
				index[j] = j;
		}

		// the devices on the platform that will be added to the context
		for (int i = 0; i < index.length; i++) {
			if (index[i] >= devices.length)
				throw new IndexOutOfBoundsException("context index out of bound when creating command queues: "
						+ index[i] + ", max: " + devices.length);

			EZCommandQueue newQ = new EZCommandQueue(context, devices[index[i]]);
			newQ.setContext(this);
			commandQueues.add(newQ);
		}
	}

	/**
	 * Creates kernels specified by knlNaes form src code String
	 * 
	 * @param src
	 *            code scripts
	 * @param knlNames
	 *            names of kernels to be built
	 */
	public void addKernels(String[] src, String... knlNames) {
		if (knlNames.length == 0)
			knlNames = null;
		
		// build selected kernels
		cl_kernel[] tmpKernels = EZKernel.createKernels(context, src, knlNames);

		// adds each of the kernels to this context
		for (int i = 0; i < tmpKernels.length; i++) {
			EZKernel newK = new EZKernel(tmpKernels[i], src, knlNames == null ? null : knlNames[i]);
			newK.setContext(this);
			kernels.add(newK);
		}
	}

	/**
	 * Adds primitive arrays to this context as memory objects.
	 * 
	 * @param mem
	 *            primitive arrays
	 */
	public void addMemoryObjects(Object... mem) {
		if (mem == null)
			throw new IllegalArgumentException("null mem when adding memory objects");
		for (int i = 0; i < mem.length; i++) {
			EZMem newM = new EZMem(context, mem[i]);
			newM.setContext(this);
			memories.add(newM);
		}
	}

	/**
	 * Sets the argument of a kernel named kernelName
	 * 
	 * @param kernelName
	 *            the name of the kernel being set
	 * @param arguments
	 *            a list of arguments
	 */
	public void setKernelArguments(String kernelName, Object... arguments) {
		getKernel(kernelName).setArguments(arguments);
	}

	/**
	 * Runs the kernel on this command queue.
	 * 
	 * @param qIndex
	 *            the queue index in this context
	 * @param kernelName
	 *            the kernel's name
	 * @param globalSize
	 *            working space's global size
	 * @param localSize
	 *            working space's local size (if applicable)
	 */
	public void runKernel(int qIndex, String kernelName, long[] globalSize, long[]... localSize) {
		EZCommandQueue queue = commandQueues.get(qIndex);
		EZKernel kernel = getKernel(kernelName);
		
		if (localSize.length > 1)
			throw new IllegalArgumentException("only one localSize argument allowed");
		long[] lSize = (localSize.length == 1) ? localSize[0] : null;
		queue.runKerel(kernel, globalSize, lSize);
	}

	/**
	 * Reads the memory object at index mIndex on this context to a 1d primitive
	 * array using a randomly chosen command queue.
	 * 
	 * @param mIndex
	 *            the index of the memObj
	 * @param array
	 *            an primitive array
	 */
	public void readBuffer(int mIndex, Object array) {
		readBuffer(memories.get(mIndex), array);
	}

	/**
	 * Reads the EZMem memory object on this context to a 1d primitive array
	 * using a randomly chosen command queue.
	 * 
	 * @param mem
	 *            the EZMem to be read
	 * @param array
	 *            an primitive array
	 */
	public void readBuffer(EZMem mem, Object array) {
		getALuckyCmdQ().readWholeBuffer(mem, array);
	}

	/**
	 * Writes a 1d primitive array to the memory object at index mIndex on this
	 * context using a randomly chosen command queue.
	 * 
	 * @param mIndex
	 *            the index of the memObj
	 * @param array
	 *            an primitive array
	 */
	public void writeBuffer(int mIndex, Object array) {
		writeBuffer(memories.get(mIndex), array);
	}

	/**
	 * Writes a 1d primitive array to the memory object on this context using a
	 * randomly chosen command queue.
	 * 
	 * @param mem
	 *            the EZMem to be write to
	 * @param array
	 *            an primitive array
	 */
	public void writeBuffer(EZMem mem, Object array) {
		getALuckyCmdQ().writeWholeBuffer(mem, array);
	}

	/**
	 * Maps a memory object to its local buffer
	 * 
	 * @param mIndex
	 *            the memory index in this context to map
	 * @param read_write
	 *            is the mapping readable and/or writable? If no such arguments,
	 *            then the mapping is both readable and writable.
	 * 
	 * @return the mapped pointer
	 */
	public ByteBuffer mapBuffer(int mIndex, boolean... readOnly) {
		return mapBuffer(memories.get(mIndex), readOnly);
	}
	
	/**
	 * Maps a memory object to its local buffer
	 * 
	 * @param mem
	 *            the memory object in this context to map
	 * @param read_write
	 *            is the mapping readable and/or writable? If no such arguments,
	 *            then the mapping is both readable and writable.
	 * 
	 * @return the mapped pointer
	 */
	public ByteBuffer mapBuffer(EZMem mem, boolean... readOnly) {
		if (readOnly.length == 0)
			return getALuckyCmdQ().mapWholeBuffer(mem, false);
		else if (readOnly.length == 1)
			return getALuckyCmdQ().mapWholeBuffer(mem, readOnly[0]);
		else
			throw new IllegalArgumentException("only accept (object) or (object, true/false)");
	}

	/**
	 * Unmaps the memory object on mIndex.
	 * 
	 * @param mIndex
	 *            the memory index in this context to map
	 */
	public void unmapBuffer(int mIndex) {
		unmapBuffer(memories.get(mIndex));
	}
	
	public void unmapBuffer(EZMem mem) {
		getALuckyCmdQ().unmapBuffer(mem);
	}

	public cl_context getContext() {
		return context;
	}

	public EZCommandQueue getCommandQueue(int index) {
		return commandQueues.get(index);
	}

	public void setCommandQueue(ArrayList<EZCommandQueue> commandQueue) {
		this.commandQueues = commandQueue;
	}

	public EZKernel getKernels(int index) {
		return kernels.get(index);
	}

	/**
	 * Gets the kernel of the name
	 * 
	 * @param name
	 *            the name of the kernel
	 * @return the EZKernel
	 */
	public EZKernel getKernel(String name) {
		for (EZKernel knl : kernels)
			if (knl.getKernelName().trim().equals(name.trim()))
				return knl;

		throw new IllegalArgumentException("kernel " + "\"" + name + "\" is not found");
	}

	public EZMem getMemObjects(int index) {
		return memories.get(index);
	}

	@Override
	public String toString() {
		String s = "Context " + context.toString().substring(10) + "\n";
		s += "Number of devices:\t" + (int) EZCL.getContextInfo(context, CL_CONTEXT_NUM_DEVICES) + "\n";
		s += "Associated platform:\t" + EZCL.getPlatInfo(platform, CL_PLATFORM_NAME) + ": "
				+ platform.toString().substring(14) + "\n";

		// must has at least 1 device
		s += "Devices: \n";
		for (cl_device_id device : devices) {
			// print device and find out where it is from
			s += "\t" + EZCL.getDeviceInfo(device, CL_DEVICE_NAME) + ": " + device.toString().substring(12) + "\n";
			if (commandQueues != null)
				for (EZCommandQueue queue : commandQueues)
					if (EZCL.getCommandQueueInfo(queue.getQueue(), CL_QUEUE_DEVICE).toString()
							.equals(device.toString()))
						s += "\t\tAssociated command queue : " + queue.getQueue().toString().substring(16) + "\n";
		}

		// has kernel
		if (kernels.size() != 0)
			s += "Kernels: \n";
		for (EZKernel kernel : kernels)
			s += "\t" + kernel.getKernelName() + " : " + kernel.getKernel().toString().substring(9) + "\n";

		// has memObjs
		if (memories.size() != 0)
			s += "MemObjects: \n";
		for (EZMem mem : memories)
			s += "\t" + mem.getcTypeName() + "[" + mem.length() + "] : " + mem.getMemory().toString().substring(6)
					+ "\n";

		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof EZContext))
			return false;
		EZContext ctx = (EZContext) obj;
		return context.toString() == ctx.getContext().toString();
	}

	/**
	 * Gets a lucky command queue to do the drudgery
	 * 
	 * @return a random commandQueue
	 */
	private EZCommandQueue getALuckyCmdQ() {
		if (commandQueues.size() == 0)
			throw new RuntimeException("currently no command queue available to write the buffer");
		int theLuckyGuy = (int) (Math.random() * commandQueues.size());
		return commandQueues.get(theLuckyGuy);
	}
}
