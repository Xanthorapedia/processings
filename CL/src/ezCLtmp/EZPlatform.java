package ezCLtmp;

import static org.jocl.CL.*;
import org.jocl.*;

public class EZPlatform {
	/** the platform represented by this instance */
	private cl_platform_id platform;
	/** all the devices on the platform */
	private EZDevice[] devices;
//	/** the context the platform is associated with */
//	private EZContext context;
	
	public EZPlatform(cl_platform_id pid) {
		platform = pid;
		cl_device_id[] devices = EZCL.getDevices(platform, -1);
		this.devices = new EZDevice[devices.length];
		// encapsulate
		for (int i = 0; i < devices.length; i++) {
			this.devices[i] = new EZDevice(devices[i]);
			this.devices[i].setPlatform(this);
		}
	}
	
	/**
	 * Gets all the devices found on the platform
	 * @return devices on the platform
	 */
	public EZDevice[] getDevices() {
		return devices;
	}
	
	/**
	 * Gets all the devices' IDs
	 * @return devices' IDs on the platform
	 */
	public cl_device_id[] getDeviceIDs() {
		cl_device_id[] devices = new cl_device_id[this.devices.length];
		for (int i = 0; i < devices.length; i++)
			devices[i] = this.devices[i].getDeviceId();
		return devices;
	}
	
	/**
	 * Gets the devices found on the platform of the specified type
	 * @param the type of devices gotten
	 * @return devices on the platform
	 */
	public EZDevice[] getDevices(int type) {
		return EZPlatform.getDevices(this, type);
	}
	
	/**
	 * Gets the devices found on the platform of the specified type
	 * @param platform from where the devices are gotten
	 * @param the type of devices gotten
	 * @return devices on the platform
	 */
	public static EZDevice[] getDevices(EZPlatform platform, int type) {
		// Obtain the number of devices for the platform
		int numDevicesArray[] = new int[1];
		clGetDeviceIDs(platform.getPlatformId(), -1, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];
		// Obtain device IDs
		cl_device_id devices[] = new cl_device_id[numDevices];
		clGetDeviceIDs(platform.getPlatformId(), type, numDevices, devices, null);
		EZDevice[] ezDevices = new EZDevice[numDevices];
		
		// encapsulate
		for (int i = 0; i < numDevices; i++) {
			ezDevices[i] = new EZDevice(devices[i]);
			ezDevices[i].setPlatform(platform);
		}
		return ezDevices;
	}
	
	/**
	 * @param platform
	 *            the platform
	 * @param cons
	 *            the field constant
	 * @return the platform info String
	 */
	private static String getPlatInfo(cl_platform_id platform, int cons) {
		String s = "";
		// char length
		long[] len = new long[1];
		clGetPlatformInfo(platform, cons, 0, null, len);
		// info string
		char[] str = new char[(int) len[0]];
		Pointer p = Pointer.to(str);
		clGetPlatformInfo(platform, cons, len[0], p, len);
		s += EZCL.charArray2String(str);
		return s + "\n";
	}
	
	/**
	 * Gets the platform's id
	 * @return the platform's id
	 */
	public cl_platform_id getPlatformId() {
		return platform;
	}
	
//	/**
//	 * Gets the context associated with this platform
//	 * @return the context associated with this platform
//	 */
//	public EZContext getContext() {
//		return context;
//	}
//
//	/**
//	 * Sets the context associated with this platform
//	 * @param context the context associated with this platform
//	 */
//	void setContext(EZContext context) {
//		this.context = context;
//	}

	@Override
	public String toString() {
		String s = "";
		s += "Name:\t\t" + getPlatInfo(platform, CL_PLATFORM_NAME);
		s += "Version:\t" + getPlatInfo(platform, CL_PLATFORM_VERSION);
		s += "Vender:\t\t" + getPlatInfo(platform, CL_PLATFORM_VENDOR);
		
		EZDevice[] devices = getDevices();
		s += "Device number: \t" + devices.length + "\n";
		int j = 0;
		for (EZDevice device : devices) {
			s += "\tDevice #" + j + ":\n";
			s += device;
			j++;
		}

		s += "\n";
		return s;
	}
}
