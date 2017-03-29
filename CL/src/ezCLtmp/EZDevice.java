package ezCLtmp;

import static org.jocl.CL.*;
import org.jocl.*;

public class EZDevice {
	/** the device represented by this instance */
	private cl_device_id device;
	/** the platform the device is on */
	private EZPlatform platform;
	
	public EZDevice(cl_device_id did) {
		device = did;
	}

	/**
	 * @param device
	 *            the device
	 * @param cons
	 *            the field constant
	 * @return the device info String
	 */
	private static String getDeviceInfo(cl_device_id device, int cons) {
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
			s += EZCL.charArray2String(str);
		return s + "\n";
	}
	
	/** 
	 * Gets the device Id of this instance
	 * @return the device ID
	 */
	public cl_device_id getDeviceId() {
		return device;
	}
	
	/**
	 * Gets the platform the device belongs to
	 * @return the platform the device belongs to
	 */
	public EZPlatform getPlatform() {
		return platform;
	}
	
	/**
	 * Sets the platform the device belongs to
	 * @param the platform the device belongs to
	 */
	void setPlatform(EZPlatform platform) {
		this.platform = platform;
	}

	@Override
	public String toString() {
		String s = "";
		s += "\tName:\t\t" + getDeviceInfo(device, CL_DEVICE_NAME);
		s += "\tVersion:\t" + getDeviceInfo(device, CL_DEVICE_VERSION);
//		s += "\tMax wGS:\t" + getDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
		s += "\tMax Comp. Unit:\t" + getDeviceInfo(device, CL_DEVICE_MAX_COMPUTE_UNITS);
		s += "\tVender:\t\t" + getDeviceInfo(device, CL_DEVICE_VENDOR);
		
		s += "\n";
		return s;
	}
}
