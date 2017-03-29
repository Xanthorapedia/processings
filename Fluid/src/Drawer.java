import processing.core.*;

public class Drawer extends PApplet {

	static final int W = 720;
	static final int H = 540;
	int prevPos = 0;
	int prevX = -1;
	int prevY = -1;
	PImage image;
	Fluid fluid;

	public static void main(String[] args) {
		PApplet.main("Drawer");
	}
	
	public void settings() {
		fluid = new Fluid(W, H, 0.07f, 0.9f, 1E-20f);
		image = this.createImage(W, H, RGB);
		size(W, H);
		for (int i = 269; i < 270; i ++) {
//			for (int j = 1; j < W + 1; j++)
//				fluid.vel[0][i * (W + 2) + 0] = 6000;
//			fluid.vel[0][(i + 1) * (W + 2) + 20] = 500;
			fluid.density[0][i * (W + 2) + 1] = 8000;
//			fluid.density[1][i * (W + 2) + 1] = 50000000;
		}
		//TODO this works
		for (int i = 268; i < 269; i ++) {
//			for (int j = 1; j < W + 1; j++)
//				fluid.vel[0][i * (W + 2) + 0] = 6000;
//			fluid.vel[0][(i + 1) * (W + 2) + 20] = 500;
			fluid.forces[0][i * (W + 2) + 1] = 800000;
//			fluid.forces[1][i * (W + 2) + 1] = -800000;
		}
//		for (int i = 1; i < H; i ++) {
//			for (int j = 1; j < W; j++)
//				fluid.forces[0][i * (W + 2) + j] = 6000;
////			fluid.vel[0][(i + 1) * (W + 2) + 20] = 500;
////			fluid.forces[0][i * (W + 2) + 1] = 800000;
////			fluid.forces[1][i * (W + 2) + 1] = -800000;
//		}
//		for (int i = 1; i < H + 2; i ++) {
//			for (int j = 1; j < W + 1; j++)
//				fluid.vel[0][i * (W + 2) + j] = 200;
////			fluid.vel[0][(i + 1) * (W + 2) + 20] = 500;
////			fluid.forces[1][i * (W + 2) + 0] = 500;
//		}
//		fluid.forces[0][fluid.pixelXY(360, 270)] = -500000;
//		fluid.forces[1][fluid.pixelXY(360, 270)] = -500000;
//		
//		fluid.forces[0][fluid.pixelXY(361, 270)] = 500000;
//		fluid.forces[1][fluid.pixelXY(361, 270)] = -500000;
//		
//		fluid.forces[0][fluid.pixelXY(360, 271)] = -500000;
//		fluid.forces[1][fluid.pixelXY(360, 271)] = 500000;
//		
//		fluid.forces[0][fluid.pixelXY(361, 271)] = 500000;
//		fluid.forces[1][fluid.pixelXY(361, 271)] = 500000;
	}
	
	public void setup() {
		frameRate(30);
		background(0);
	}
	
	public void draw() {
//		if (mousePressed) {
//			int pos = mouseY * (W + 2) + mouseX;
//			fluid.density[0][prevPos] = 0;
//			fluid.density[0][pos] = 80000;
//			prevPos = pos;
//		}
		
		fluid.update();
		
		for (int i = 269; i < 270; i ++) {
//			fluid.density[1][i * (W + 2) + 1] = 0;
//			fluid.forces[0][i * (W + 2) + 1] = 0;
		}
		
		image.pixels = fluid.getImage();
		image.updatePixels();
		image(image, 0, 0);
		//background(255);
		for (int i = 0; i < W; i += 10)
			for (int j = 0; j < H; j += 10) {
//				line(i, j, i + fluid.vel[0][j * (W + 2) + i] * 0.1f, j + fluid.vel[1][j * (W + 2) + i] * 0.1f);
			}
	}
	
	public void mousePressed() {
		if (prevX == -1) {
			prevX = mouseX;
			prevY = mouseY;
		}
		fluid.forces[0][prevPos] = 0;
		fluid.forces[1][prevPos] = 0;
	}
	
	public void mouseReleased() {
		prevPos = prevY * (W + 2) + prevX;
		fluid.forces[0][prevPos] = (mouseX - prevX) * 50000;
		fluid.forces[1][prevPos] = (mouseY - prevY) * 50000;
		if (prevPos == -1)
			prevPos = mouseY * (W + 2) + mouseX;
		prevX = prevY = -1;
		fluid.update();
//		fluid.forces[0][pos] = (mouseX - prevX) * 0;
//		fluid.forces[1][pos] = (mouseY - prevY) * 0;
	}
}
