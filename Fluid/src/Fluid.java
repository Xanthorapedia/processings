import java.awt.Color;

import ezcl.*;

public class Fluid{
	private final int W;
	private final int H;
	private final int SIZE;
	private EZContext nv;
	public float gSize;
	public float dt;
	private long[] deftWSize;
	
	// velocity
	public float[][] vel;
	private EZMem vU;
	private EZMem vV;
	// scratch
	private float[][] vTmp;
	private EZMem vUt;
	private EZMem vVt;
	// pressure divergence
	private float[] pressure;
	private EZMem pDiv;
	
	// density
	public float[][] density;
	private EZMem denTmp;
	private EZMem den;
	// forces
	public float[][] forces;
	private EZMem fU;
	private EZMem fV;
	
	// image
	private int[] img;

	public Fluid(int w, int h, float dt, float gSize, float visc) {
		W = w;
		H = h;
		this.dt = dt;
		this.gSize = gSize;
		SIZE = W * H;
		int meshSize = (W + 2) * (H + 2);
		deftWSize = new long[] {W, H};
		vel = new float[2][meshSize];
		vTmp = new float[2][meshSize];
		forces = new float[2][meshSize];
		pressure = new float[meshSize];
		density = new float[2][meshSize];
		img = new int[SIZE];
		
		// set up cl
		EZCL cl = new EZCL();
		cl.addContexts(new int[][] {{}, {0}});
		nv = cl.contexts.get(0);
		nv.addCommandQueues();
		nv.addKernels(new String[] {EZCL.fromFile("fluid.cl")}, null, "applyForce", "4advect", "3diffuse", "pressureSolver");
		nv.addMemoryObjects(vel[0], vel[1], vTmp[0], vTmp[1]);
		nv.addMemoryObjects(forces[0], forces[1]);
		nv.addMemoryObjects(pressure, density[0], density[1]);
		
		// buffers
		vU = nv.getMemObjects(0);
		vV = nv.getMemObjects(1);
		vUt = nv.getMemObjects(2);
		vVt = nv.getMemObjects(3);
		fU = nv.getMemObjects(4);
		fV = nv.getMemObjects(5);
		pDiv = nv.getMemObjects(6);
		denTmp = nv.getMemObjects(7);
		den = nv.getMemObjects(8);
		
		// setup arguments
		
		// set force
		nv.setKernelArguments("0applyForce", vU, vV, fU, fV, dt);
		// set source
		nv.setKernelArguments("1applyForce", den, vVt, denTmp, fV, dt);
		
		// velocity diffusion
		nv.setKernelArguments("0diffuse", vU, vV, vUt, vVt, (gSize * gSize) / (visc * dt));
		nv.setKernelArguments("1diffuse", vUt, vVt, vU, vV, (gSize * gSize) / (visc * dt));
		// velocity advect
		nv.setKernelArguments("0advect", vUt, vVt, vUt, vU, dt, gSize);
		nv.setKernelArguments("1advect", vUt, vVt, vVt, vV, dt, gSize);
		nv.setKernelArguments("3advect", vU, vV, vU, vUt, dt, gSize);
		nv.setKernelArguments("4advect", vU, vV, vV, vVt, dt, gSize);
		
		// density diffusion
		nv.setKernelArguments("2diffuse", den, vV, denTmp, vVt, (gSize * gSize) / (visc * dt));
		nv.setKernelArguments("3diffuse", denTmp, vVt, den, vV, (gSize * gSize) / (visc * dt));
		// density advect
		nv.setKernelArguments("2advect", vU, vV, denTmp, den, dt, gSize);
		
		nv.setKernelArguments("divergence", vU, vV, pDiv, gSize);
		
		nv.setKernelArguments("0pressureSolver", vUt, vVt, pDiv, gSize * gSize);
		nv.setKernelArguments("1pressureSolver", vVt, vUt, pDiv, gSize * gSize);
		
		nv.setKernelArguments("elimPGrad", vU, vV, vUt, gSize * gSize);
		
		for (int i = 0; i < 9; i++)
			nv.writeBuffer(i, density[1]);
	}
	
	public void source() {
		nv.writeBuffer(denTmp, density[0]);
		nv.runKernel(0, "1applyForce", deftWSize);
	}
	
	public void force() {
		nv.writeBuffer(fU, forces[0]);
		nv.writeBuffer(fV, forces[1]);
		nv.runKernel(0, "0applyForce", deftWSize);
	}
	
	public void diffuse() {
		for (int i = 0; i < 20; i++) {
			nv.runKernel(0, "0diffuse", deftWSize);
			nv.runKernel(0, "1diffuse", deftWSize);
		}
	}
	
	public void vel_diffuse() {
		for (int i = 0; i < 20; i++) {
			nv.runKernel(0, "0diffuse", deftWSize);
			nv.runKernel(0, "1diffuse", deftWSize);
		}
		nv.runKernel(0, "0diffuse", deftWSize);
	}
	
	public void den_diffuse() {
		for (int i = 0; i < 20; i++) {
			nv.runKernel(0, "2diffuse", deftWSize);
			nv.runKernel(0, "3diffuse", deftWSize);
		}
		nv.runKernel(0, "2diffuse", deftWSize);
	}
	
	public void advect() {
		nv.runKernel(0, "advect", deftWSize);
	}
	
	public void vel_advect() {
		nv.runKernel(0, "3advect", deftWSize);
		nv.runKernel(0, "4advect", deftWSize);
		nv.runKernel(0, "0advect", deftWSize);
		nv.runKernel(0, "1advect", deftWSize);
	}
	
	public void den_advect() {
		nv.runKernel(0, "2advect", deftWSize);
	}
	
	public void project() {
		nv.runKernel(0, "divergence", deftWSize);
//		nv.readBuffer(pDiv, vTmp[0]);
		for (int i = 0; i < 20; i++) {
			nv.runKernel(0, "0pressureSolver", deftWSize);
			
			nv.runKernel(0, "1pressureSolver", deftWSize);
		}
//		nv.readBuffer(vUt, vTmp[0]);
		
		nv.runKernel(0, "elimPGrad", deftWSize);
	}
	
	public void den_update() {
		source();
//		nv.writeBuffer(denTmp, density[1]);
		
		den_diffuse();
		den_advect();
//		for (int i = 0; i < density[1].length; i++)
//		if (density[1][i] != 0)
//			System.out.println(density[1][i]);
//		nv.readBuffer(den, density[1]);
//		nv.readBuffer(denTmp, density[0]);
//		System.out.println(density[1][269 * (W + 2) + 1]);
	}
	
	public void vel_update() {
		force();
		setBoundary();
		vel_diffuse();
		setBoundary();
		project();
		vel_advect();
		setBoundary();
		project();
		nv.readBuffer(vU, vel[0]);
//		System.out.println(vel[0][269 * (W + 2) + 1]);
	}
	
	public void update() {
		vel_update();
		den_update();
	}
	
	public int[] getImage() {
		
		nv.readBuffer(vV, vel[1]);
		nv.readBuffer(den, density[1]);
//		for (int i = 0; i < vel[0].length; i++)
//			if (vel[0][i] != 0)
//				System.out.println(vel[0][i]);
		// TODO debug

		// TODO \debug
		
		for (int i = 0; i < W; i++)
			for (int j = 0; j < H; j++) {
				int pos = pixelXY(i, j);
//				float value = (float) Math.sqrt(vel[0][pos] * vel[0][pos] + vel[1][pos] * vel[1][pos]);
				float value = density[1][pos]; // pressure
//				float value = vel[0][pos];
				value = Math.abs(value);
//				int gS = (int) Drawer.map(value, 0, 10, 0, 255);
//				if (value != 0)
//					System.out.println(value);
				int gS = (int) ((1 / (1 + Math.exp(-value * 0.5)) - 0.5) * 512);
//				int gS = (int) ((1 / (1 + Math.exp(-value * 0.001))) * 512);
				gS |= gS << 8;
				gS |= gS << 16;
//				gS = Color.HSBtoRGB(1.8f, 1, 1);
				img[j * W + i] = gS;
			}
		return img;
	}
	
	public int pixelXY(int x, int y) {
		return (y + 1) * (W + 2) + (x + 1);
	}
	
	private int pointXY(int x, int y) {
		return y * (W + 2) + x;
	}
	
	private void setBoundary() {
		for (int i = 90; i < 150; i++)
			for (int j = 240; j < 300; j++) {
				int dis = (i - 120) * (i - 120) + (j - 269) * (j - 269);
				if (400 < dis && dis < 441) {
					float x = (i - 120) / 20;
					float y = (j - 269) / 20;
					float projX = vel[0][pixelXY(i, j)] * x;
					float projY = vel[1][pixelXY(i, j)] * y;
					vel[0][pixelXY(i, j)] -= projX;
					vel[1][pixelXY(i, j)] -= projY;
				}
			}
	}
	
	public void piexelViolation(float[] arr) {
		for (int i = 0; i < W + 2; i++)
			for (int j = 0; j < H + 2; j++)
				if (arr[pointXY(i, j)] !=0 && (i == 0 || j == 0 || i == W + 1 || j == H + 1))
				System.out.println(i + ", " + j + ": " + arr[pointXY(i, j)]);
	}
}
