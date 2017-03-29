import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
public class Gravity extends PApplet{
	ArrayList<MassPoint> points = new ArrayList<MassPoint>();
	ArrayList<PVector> click = new ArrayList<PVector>(); 
	
	public static void main(String[] arg) {
		PApplet.main("Gravity");
	}
	
	public void settings() {
		  size(800, 600);
	}

	public void setup() {
	  background(51);
	  PVector[] v = new PVector[3];
	  v[0] = new PVector(random(0, 50), random(0, 50));
	  v[0] = new PVector(0, 0);
	  v[1] = new PVector(random(0, 50), random(0, 50));
	  v[2] = PVector.add(v[0], v[1]);
	  v[2].mult(-1);
	  for (int i = 0; i < 2E4; i++) {
	    //PVector x = new PVector(random(- width / 2, width / 2), random(- height / 2, height / 2));
	    
	    //float k = random(0, 2 * PI);
	    //PVector x = new PVector(10 * cos(k), 10 * sin(k));
	    
	    float x1 = random(-10, 10);
	    float y1 = random(-10, 10);
	    PVector x = (x1 > y1)?  new PVector(10, x1) : new PVector(y1, 10);
	    MassPoint p = new MassPoint(x);
	    //p.setV(v[i + 1]);
	    points.add(p);
	  }
	  click.add(null);
	  //click.add(new PVector(-100, 0));
	}

	public void mouseClicked() {
	  click.add(new PVector(mouseX - width / 2, mouseY - height / 2));
	}

	public void draw() {
	  background(51);
	  colorMode(HSB);
	  translate(width / 2, height / 2);
	  //scale(3);
	  for (MassPoint point : points) {
	    if (mousePressed)
	      click.set(0, new PVector(mouseX - width / 2, mouseY - height / 2));
	    else
	      click.set(0, null);
	    point.update(click, 5E8f);
	    //ArrayList<PVector> point2 = new ArrayList<PVector>();
	    //for (MassPoint point1 : points)
	    //if(point1 != point)
	    //  point2.add(point1.getX());
	    //point.update(point2, 5E8f);
	    PVector x = point.getX();
	    PVector v = point.getV();
	    stroke(map(v.mag(), 0, 1000, 0, 130), 255, 255);
	    //fill(map(v.mag(), 0, 100, 0, 360), 255, 255);
	    //ellipse(x.x, x.y, 10, 10);
	    point(x.x, x.y);
	    //line(x.x, x.y, x.x + v.x, x.y + v.y);
	  }
	  //System.out.print(new PVector(mouseX, mouseY));
	  scale(10);
	}

	class MassPoint {
	  private PVector x;
	  private PVector v;
	  private PVector a;
	  private static final float mass = 1E-2f;
	  private static final float dt = 0.01f;
	  private static final float G = 1E-2f;
	  private static final float G_mass = G * mass;
	  
	  MassPoint(PVector x) {
	    this.x = new PVector(x.x, x.y);
	    this.v = new PVector(0, 0);
	    this.a = new PVector(0, 0);
	  }
	  
	  PVector getX() {
	    return x;
	  }
	  
	  PVector getV() {
	    return v;
	  }
	  
	  void setV(PVector V) {
	    v.set(V);
	  }
	  
	  void update(ArrayList<PVector> position, float mass) {
	    // F = G * m1 * m2 / (r * r) * r_hat
	    PVector F = new PVector(0, 0);
	    for (PVector pos : position) {
	      if (pos == null)
	        continue;
	      PVector r = PVector.sub(pos, x);
	      float rMagCub = r.mag();
	      rMagCub *= rMagCub;// * rMagCub;
	      F.add(PVector.mult(r, G_mass * mass / rMagCub));
	    }
	    //if (F.mag() > 1E8)
	    //  F.normalize();
	    v = F;
	    // a = F /m
	    //a.set(PVector.div(F, MassPoint.mass));
	    //// dv = a * dt
	    //v.add(PVector.mult(a, dt));
	    // dx = a * dt
	    x.add(PVector.mult(v, dt));
	  }
	}
}
