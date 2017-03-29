import ezCLtmp.*;

public class EZCLTest {
	static String programSource =
	        "__kernel void "+
	        "sampleKernel(__global const float *a,"+
	        "             __global const float *b,"+
	        "             __global float *c)"+
	        "{"+
	        "    int gid = get_global_id(0);"+
	        "    c[gid] = a[gid] * b[gid];"+
	        "}";

	public static void main(String[] args) throws ClassNotFoundException {
		// intel graphics x work (cannot get build kernel)
		EZCL cl = new EZCL(new int[][]{{0, 1}, {0}});
		//System.out.println(cl.contexts[0]);
		cl.setKernels(new int[]{0, 1}, new String[] {programSource}, new String[] {"sampleKernel"});
		//cl.setAllKernelsFromFile(new String[] {"src.cl"});
		cl.simpleTest();
		//System.out.println(cl.contexts[1]);
		System.out.println(byte.class);
	}

}
