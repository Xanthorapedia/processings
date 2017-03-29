// xmp = x of mass points
// x = pos, v = vel, a = acc
// G_m1_m2_dt = G * m1 * m2 * dt

#define get(array, i, j) array[i * 2 + j]
uint toRGB(double value, double min, double max);

__kernel void update_a(
	__global double* xmp,
	__global double* x,
	__global double* v,
	__global double* a,
	__global int* buf,
	double G_m1_m2,
	double dt,
	int w,
	int h
	)
{
	// masspoint index
	unsigned int iprtcl = get_global_id(0);
	// particle index
	unsigned int ipoint = get_global_id(1);
	
	// calculate pos diff
	double dx = get(xmp, ipoint, 0) - get(x, iprtcl, 0); //xmp[0][ipoint] - x[0][iprtcl];
	double dy = get(xmp, ipoint, 1) - get(x, iprtcl, 1); //xmp[1][ipoint] - x[1][iprtcl];
	
	// F
	double drCube = dx * dx + dy * dy; // pow(sqrt(dx * dx + dy * dy), 3);
	double F = G_m1_m2 / drCube;
	
	double Fx = F * dx;
	double Fy = F * dy;
	
	// acceleration
	get(a, iprtcl, 0) += Fx / 1000;
	get(a, iprtcl, 1) += Fy / 1000;
	
	int ix = get(xmp, ipoint, 0) + w / 2;
	int iy = get(xmp, ipoint, 1) + h / 2;
	buf[iy * w + ix] = 0xFFFFFFFF;
	buf[(iy - 1) * w + ix - 1] = 0xFFFFFFFF;
	buf[(iy + 1) * w + ix - 1] = 0xFFFFFFFF;
	buf[(iy - 1) * w + ix + 1] = 0xFFFFFFFF;
	buf[(iy + 1) * w + ix + 1] = 0xFFFFFFFF;
}

__kernel void update_v(
	__global double* xmp,
	__global double* x,
	__global double* v,
	__global double* a,
	__global int* buf,
	double G_m1_m2,
	double dt,
	int w,
	int h
	)
{
	unsigned int ipoint = get_global_id(0);
	// velocity
	get(v, ipoint, 0) = get(a, ipoint, 0);
	get(v, ipoint, 1) = get(a, ipoint, 1);
	
	// clear previous
	int ix = (int) get(x, ipoint, 0) + (w / 2);
	int iy = (int) get(x, ipoint, 1) + (h / 2);
	if (0 <= ix && ix < w && 0 <= iy && iy < h) {
		buf[iy * w + ix] = 0x00000000;
	}
	
	get(x, ipoint, 0) += get(v, ipoint, 0) * dt;
	get(x, ipoint, 1) += get(v, ipoint, 1) * dt;
}

__kernel void draw(
	__global double* xmp,
	__global double* x,
	__global double* v,
	__global double* a,
	__global int* buf,
	double G_m1_m2,
	double dt,
	int w,
	int h
	)
{
	unsigned int ipoint = get_global_id(0);
	int ix = (int) get(x, ipoint, 0) + (w / 2);
	int iy = (int) get(x, ipoint, 1) + (h / 2);
	if (0 <= ix && ix < w && 0 <= iy && iy < h) {
		double vx = get(v, ipoint, 0);
		double vy = get(v, ipoint, 1);
		buf[iy * w + ix] = toRGB(sqrt(vx * vx + vy * vy), 0, 500);
	} else {
		int last = get_global_size(0);
		ulong low32 = (ulong)get(x, last, 0);
		ulong high32 = (ulong)get(x, last, 1);
		ulong seed = ((high32 << 32) & low32) + ipoint;
		
		seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
		uint result = seed >> 16;
		get(x, ipoint, 0) = (double)result / 4294967294 * 4*w - w /2;
		
		seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
		result = seed >> 16;
		get(x, ipoint, 1) = (double)result / 4294967294 * h - h / 2;
		
		get(x, last, 0) = seed & ((1L << 32) - 1);
		get(x, last, 1) = seed >> 32;
	}
}

uint toRGB(double value, double min, double max) {
	value = (value - min) / (max - min);
	uint r, g, b;
	if (value < 0.25) {
		r = 0x00;
		b = 0xFF;
		g = (int) (0xFF * value * 4);
	} else if (value < 0.5) {
		r = 0x00;
		b = (int) (0xFF * (0.5 - value) * 4);
		g = 0xFF;
	} else if (value < 0.75) {
		r = (int) (0xFF * (value - 0.5) * 4);
		b = 0x00;
		g = 0xFF;
	} else {
		r = 0xFF;
		b = 0x00;
		g = (int) (0xFF * (1 - value) * 4);
	}
	return 0xFF000000 | (r << 16) | (g << 8) | b;
}