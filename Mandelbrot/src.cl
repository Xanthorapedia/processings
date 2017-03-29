// H, W = height & width of the canvas
// cenX, cenY = center of the graph
// zoom = magfactor

__kernel void sample(
	int W, int H,
	const double cenX, double cenY,
	double zoom,
	const int MAX_ITER,
	__global int* iter,
	__global int* hue
	)
{
	unsigned int ix = get_global_id(0);
    unsigned int iy = get_global_id(1);
	
	// real (a, b) of the point
	double b = (ix - W / 2.0) / zoom + cenX;
	double a = (iy - H / 2.0) / zoom + cenY;
	
	int iteration = 0;
	int sMag = 0;
	double x = 0;
	double y = 0;
	
	while (iteration < MAX_ITER && sMag < 4) {
		double sX = x * x;
		double sY = y * y;
		
		y = 2 * x * y + a;
		x = sX - sY + b;
		
		sMag = sX + sY;
		iteration++;
	}
	iter[iy * W + ix] = hue[iteration];//(iteration == MAX_ITER) ? 0 : hue[(int)((float)iteration / MAX_ITER * 360)];
}