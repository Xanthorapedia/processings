import static org.jocl.CL.CL_MAP_READ;
import static org.jocl.CL.CL_MAP_WRITE;
import static org.jocl.CL.CL_MEM_READ_WRITE;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

import processing.core.PApplet;
import processing.core.PImage;

public class GravityCL extends PApplet {
	ArrayList<Double> click = new ArrayList<Double>();
	static final int NUM = (int) 2E5;
	static final int SIZE = NUM * 2 + 2;
	static final int W = 1000;
	static final int H = 600;

	static double[] pos = new double[SIZE];
	static double[] vel = new double[SIZE];
	static double[] acc = new double[SIZE];
	static double[] massPoints ;//= new double[] {0,0,-150, 0, 150, 0, 0, -150, 0, 150, -106.066017, -106.066017, -106.066017, 106.066017, 106.066017, -106.066017, 106.066017, 106.066017};
	
	static int[] buf = new int[W * H * Sizeof.cl_int];
	
	static cl_kernel[] kernels;
	static cl_mem p;
	static cl_mem x;
	static cl_mem v;
	static cl_mem a;
	static cl_mem imgBuf;
	
	static PApplet applet = new PApplet();
	static PImage img;

	public static void main(String[] arg) {
		img = applet.createImage(W, H, RGB);
		
		EZCL.initCL();
		kernels = EZCL.buildKernels(new String[] { EZCL.fromFile("gravity.cl") }, null);
		int size = 170;
		
		int num = 17;
		massPoints = new double[num * 2 + 2];
		for (int i = 0; i < num; i++) {
			massPoints[i * 2 + 1] = size * Math.cos(Math.PI * 2 / num * i);
			massPoints[i * 2 + 0] = size * Math.sin(Math.PI * 2 / num * i);
		}
		massPoints[num * 2 + 1] = 0;
		massPoints[num * 2 + 0] = 0;
		size = 100;
//		for (int i = 0; i < NUM; i++) {
//			double x1 = Math.random() * 2 * size - size;
//			double y1 = Math.random() * 2 * size - size;
//			if (x1 > y1) {
//				pos[i * 2 + 0] = size;
//				pos[i * 2 + 1] = x1;
//			} else {
//				pos[i * 2 + 0] = y1;
//				pos[i * 2 + 1] = size;
//			}
//			vel[i * 2 + 0] = (Math.random() - 0.5);
//			vel[i * 2 + 1] = (Math.random() - 0.5);
//		}
		
		for (int i = 0; i < NUM; i ++) {
			pos[i * 2 + 0] = size * Math.cos(((double)i / NUM) * Math.PI * 2);
			pos[i * 2 + 1] = size * Math.sin(((double)i / NUM) * Math.PI * 2);
		}
		
		p = newBuffer(60 * Sizeof.cl_double, Pointer.to(massPoints));
		x = newBuffer(SIZE * Sizeof.cl_double, Pointer.to(pos));
		v = newBuffer(SIZE * Sizeof.cl_double, Pointer.to(vel));
		a = newBuffer(SIZE * Sizeof.cl_double, Pointer.to(acc));
		imgBuf = newBuffer(W * H * Sizeof.cl_int, Pointer.to(buf));
		
		for (int i = 0; i < 3; i++)
			EZCL.setKnlArg1(kernels[i], p, x, v, a, imgBuf, 1.9E5, 0.01, W, H);
		//EZCL.setKnlArg1(kernels[2], x, v, imgBuf, W, H);
		
		PApplet.main("GravityCL");
	}

	public void settings() {
		size(W, H);
	}

	public void setup() {
		background(51);
		stroke(255);

		// click.add(new PVector(-100, 0));
	}

	public void mouseClicked() {
		click.add((double) (mouseX - width / 2));
		click.add((double) (mouseY - height / 2));
		
		//ByteBuffer b = editBegin(p, click.size() * Sizeof.cl_double);
		massPoints = new double[click.size()];
		for (int i = 0; i < click.size(); i++) {
			massPoints[i] = click.get(i);
		}
		//editEnd(p, b);
		
		p = newBuffer(massPoints.length * 2 * Sizeof.cl_double, Pointer.to(massPoints));
		
			EZCL.setKnlArg1(kernels[0], p, x, v, a, imgBuf, 5E5, 0.01, W, H);
	}
	
//	public void mousePressed() {
//		click.add( (double) (mouseX - width / 2));
//		click.add( (double) (mouseY - height / 2));
//		ByteBuffer b = CL.clEnqueueMapBuffer(EZCL.commandQueue, p, true, CL_MAP_WRITE | CL_MAP_READ, 0, click.size() * Sizeof.cl_double, 0, null, null, null);
//		massPoints = new double[click.size()];
//		for (int i = 0; i < click.size(); i++) {
//			massPoints[i] = click.get(i);
//		}
//	}
//	
//	public void mouseReleased() {
//		click.remove(0);
//		click.remove(0);
//		p = CL.clCreateBuffer(EZCL.context, CL_MEM_READ_WRITE | CL.CL_MEM_USE_HOST_PTR, SIZE * Sizeof.cl_double, Pointer.to(click), null);
//	}

	public void draw() {
		//background(51);
		//translate(width / 2, height / 2);
		// scale(3);
		
		update();

		// create mass points array
		

		//scale(10);
	}

	public void update() {
		background(0);
//		ByteBuffer r = editBegin(a, SIZE * Sizeof.cl_double);
//		ByteBuffer q = editBegin(v, SIZE * Sizeof.cl_double);
//		ByteBuffer p = editBegin(x, SIZE * Sizeof.cl_double);
		
//		for (int i = 0; i < buf.length; i++) {
//		  buf[i] = toRGB((double)i / 800, 0, 600);// & 0xFF0000FF;
//		}
//		toRGB(50, 0, 100);
		
		EZCL.executeKernel(kernels[0], 2, new long[]{NUM, massPoints.length / 2}, null);
//		EZCL.readBuf(a, SIZE * Sizeof.cl_double, Pointer.to(acc));
		EZCL.executeKernel(kernels[1], 1, new long[]{NUM}, null);
//		EZCL.readBuf(v, SIZE * Sizeof.cl_double, Pointer.to(vel));
		EZCL.executeKernel(kernels[2], 1, new long[]{NUM}, null);
		//EZCL.readBuf(x, SIZE * Sizeof.cl_double, Pointer.to(pos));
		EZCL.readBuf(imgBuf, W * H * Sizeof.cl_int, Pointer.to(buf));
		buf[0] = 0xFF000000;
		
		img.pixels = buf;
		img.updatePixels();
		image(img, 0, 0);
		
//		editEnd(x, p);
//		editEnd(v, q);
//		editEnd(a, r);
	}
	
	public static cl_mem newBuffer(int size, Pointer p) {
		return CL.clCreateBuffer(EZCL.context, CL_MEM_READ_WRITE | CL.CL_MEM_COPY_HOST_PTR, size, p, null);
	}
	
	public static ByteBuffer editBegin(cl_mem memObj, int size) {
		return CL.clEnqueueMapBuffer(EZCL.commandQueue, memObj, true, CL_MAP_WRITE | CL_MAP_READ, 0, size, 0, null, null, null);
	}
	
	public static void editEnd(cl_mem memObj, ByteBuffer buf) {
		CL.clEnqueueUnmapMemObject(EZCL.commandQueue, memObj, buf, 0, null, null);
	}
}
