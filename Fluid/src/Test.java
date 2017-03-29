import ezcl.EZCL;
import ezcl.EZContext;
import ezcl.EZMem;
//import ezcl.*;
public class Test {

	private static String programSource = "__kernel void "
			+ "sampleKernel(float a, __global const float *b, __global float *c)"
			+ "{"
			+ "    int gid = get_global_id(0);"
			+ "    c[gid] = a * b[gid];"
			+ "}";

	public static void main(String[] args) {
		
		float[][] arg = new float[][] {
			{0, 1, 2},
			{3, 4, 5}
		};
		
		// set up context
		EZContext con = EZCL.quickStart(programSource);
		
		// set up memory objects
		con.addMemoryObjects(arg[0], arg[1]);
		EZMem memb = con.getMemObjects(0);
		EZMem memc = con.getMemObjects(1);
		
		// set arguments
		con.setKernelArguments("sampleKernel", 3.14f, memb, memc);
		
		// run
		con.runKernel(0, "sampleKernel", new long[] {3});
		
		// copy back
		con.readBuffer(memc, arg[1]);
		
		for (float f : arg[1])
			System.out.println(f);
	}

}
