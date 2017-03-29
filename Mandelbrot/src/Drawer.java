import processing.core.PApplet;
import processing.core.PImage;

import org.jocl.*;
import static org.jocl.CL.*;

import java.awt.Color;

public class Drawer extends PApplet {
	static PImage img;
	static cl_kernel kernel;
	static final int W = 1000;
	static final int H = 700;
	static final int Size = H * W;
	static cl_mem arg;
	static cl_mem hu;
	static int[] buf = new int[Size];
	static double zoom = 100;
	static double iter = 360;
	static double cenX = -1.3;
	static double cenY = 0.061;
	static int[] hue = new int[360];
	static int k = 0;

	public static void main(String[] args) {
		
		for (int i = 0; i < 360; i++) {
			hue[i] = Color.HSBtoRGB(i/360f + 0.0f, 1, 1);
			//System.out.println(hue[i]);
		}
		
		EZCL.initCL();
		kernel = EZCL.buildKernels(new String[]{EZCL.fromFile("src.cl")}, null)[0];
		
		
		arg = CL.clCreateBuffer(EZCL.context, CL_MEM_READ_WRITE, Size * Sizeof.cl_int, Pointer.to(buf), null);
		hu  = CL.clCreateBuffer(EZCL.context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, 360 * Sizeof.cl_int, Pointer.to(hue), null);
		
		//EZCL.setKnlArg(kernel, arg);
		EZCL.setKnlArg1(kernel, W, H, -1.4142135, 0.0, zoom, (int)iter, arg, hu);
		EZCL.executeKernel(kernel, 2, new long[]{W, H}, null);
		EZCL.readBuf(arg, Size * Sizeof.cl_int, Pointer.to(buf));
		PApplet p = new PApplet();
		img = p.createImage(W, H, RGB);
		img.pixels = buf;
		img.updatePixels();
//		System.out.println(buf[1]);
//		int sum = 0;
//		for (int i = 0; i< buf.length; i++)
//			sum+= buf[i];
//		System.out.println(sum);
		PApplet.main("Drawer");
	}
	
	public void settings() {
		size(1000, 700);
	}
	
	public void setup() {
		background(50);
		
	}

	public void draw() {
		background(50);
		zoom *= 1.01;
		iter *= 1.002;
		EZCL.setKnlArg1(kernel, W, H, cenX, cenY, zoom, (int)iter, arg, hu);
		EZCL.executeKernel(kernel, 2, new long[]{W, H}, null);
		EZCL.readBuf(arg, Size * Sizeof.cl_int, Pointer.to(buf));
		img.pixels = buf;
		img.updatePixels();
		image(img, 0, 0);
//		k++;
		//colorMode(RGB, 255);
//		for (int i = 0; i < H; i++) {
//			for (int j = 0; j < W; j++) {
//				//stroke(buf[i * W + j]);
//				stroke(hue[j % 360]);
//				point(j, i);
//			}
//		}
		
		text("zoom = " + zoom + "\niter = " + (int)iter, 100, 100);
		//saveFrame("C:\\Users\\gds12\\workspace\\367\\Mandelbrot\\img\\m-####.png");
	}
}
