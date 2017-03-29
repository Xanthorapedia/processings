package ezCLtmp;

import static org.jocl.CL.*;
import org.jocl.*;

public class EZContext {
	
	/** the contexts represented by this instance */
	private cl_context context = null;
	/** the platform associated with this context */
	private EZPlatform platform = null;
	/** the command queues associated with the devices */
	private cl_command_queue[] commandQueue = null;
	/** the kernels available to the context */
	private EZKernel[] kernels = null;
	
	public EZContext(EZPlatform platform, int[] devices) {
		this.platform = platform;
		//this.platform.setContext(this);
		updateContext(platform, devices);
	}
	
	/**
	 * Updates the contexts of this EZCL instance.
	 * @param thisPlatform devices # added to the context on each of the devices
	 * @param ezPlatform the EZPlatform associated with the context
	 * @return error code, 0 if no error
	 */
	public int updateContext(EZPlatform ezPlatform, int[] thisPlatform) {
		String i = ezPlatform.toString().split("\n")[0].split("\t")[2].trim();
		cl_platform_id platform = ezPlatform.getPlatformId();
		
		cl_device_id[] devices = ezPlatform.getDeviceIDs();
		
		// null = add all
		if (thisPlatform == null) {
			thisPlatform = new int[devices.length];
			for (int j = 0; j < devices.length; j++)
				thisPlatform[j] = j;
		}
		
		if (thisPlatform.length > devices.length)
			throw new IllegalArgumentException("too many (" + thisPlatform.length + 
					") devices to be added on platform " + i + ", max: " + devices.length);
		else if (thisPlatform.length == 0)
			throw new IllegalArgumentException("too few (" + thisPlatform.length + 
					") devices to be added on platform " + i + ", min: 1");
		
		// create the platform property
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);
		
		// the devices on the platform that will be added to the context
		cl_device_id[] added = new cl_device_id[thisPlatform.length];
		
		// copy the specifed platform to added
		for (int d0 = 0; d0 < thisPlatform.length; d0++)
			if (thisPlatform[d0] >= devices.length)
				throw new IllegalArgumentException("no such device: " + thisPlatform[d0] +
						" on platform " + i + ", max: " + (devices.length - 1));
			else {
				// if no duplicate, add the device
				for (int d1 = d0 + 1; d1 < thisPlatform.length; d1++)
					if (thisPlatform[d0] == thisPlatform[d1])
						throw new IllegalArgumentException("duplicate devices: " + d0 + 
								" on platform " + i);
				added[d0] = devices[thisPlatform[d0]];
			}
		int[] ret = new int[1];
		context = clCreateContext(contextProperties, added.length, added, null, null, ret);
		return ret[0];
	}
	
	public EZPlatform getPlatform() {
		return platform;
	}
	
	public cl_context getContext() {
		return context;
	}
	
	public cl_command_queue[] getCommandQueue() {
		return commandQueue;
	}

	public void setCommandQueue(cl_command_queue[] commandQueue) {
		this.commandQueue = commandQueue;
	}

	public EZKernel[] getKernels() {
		return kernels;
	}

	public void setKernels(EZKernel[] kernels) {
		this.kernels = kernels;
	}

	public String toString() {
		String s = "";
		s += EZCL.getContextInfo(context, CL_CONTEXT_DEVICES);
		return s;
	}
}
