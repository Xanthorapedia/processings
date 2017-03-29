#define N 720
#define get(array, x, y) array[(y) * (N + 2) + (x)]

void boundary() {
    get_global_size(0);
}

// regulates index
inline int2 indexReg(int i, int j) {
    int w = get_global_size(0);
    int h = get_global_size(1);
    
    // if tries to get over boundary, get boundary
    if (i < -1) i = -1;
    else if (i > w) i = w;
    if (j < -1) i = -1;
    else if (j > h) j = h;
    
    return (int2) (i, j);
}

// returns the value at (i, j)
inline float getVal(__global float* u, int i, int j) {
	int w = get_global_size(0);
    int h = get_global_size(1);
    if (i < 0 || i >= w || j < 0 || j >= h || (i - 120) * (i - 120) + (j - 269) * (j - 269) < 400)
        return 0;
    return get(u, i + 1, j + 1);
}

// sets the value at (i, j)
inline void setVal(__global float* u, int i, int j, float val) {
    int w = get_global_size(0);
    int h = get_global_size(1);
    
    // if tries to set boundary, do nothing
    if (i >= 0 && i < w&& j >= 0 && j < h || (i - 120) * (i - 120) + (j - 269) * (j - 269) < 400) {
        get(u, i + 1, j + 1) = val;
    }
}

// returns the velocity vector at (i, j)
inline float2 getVel(__global float* u, __global float* v, int i, int j) {
    return (float2) (getVal(u, i, j), getVal(v, i, j));
}

// sets the velocity at (i, j)
inline void setVel(__global float* u, __global float* v, int i, int j, float2 vel) {
    int w = get_global_size(0);
    int h = get_global_size(1);
    
    // if tries to set boundary, do nothing
    if (i >= 0 && i < w && j >= 0 && j < h) {
        get(u, i + 1, j + 1) = vel.x;
        get(v, i + 1, j + 1) = vel.y;
    }
}

__kernel void divergence(__global float* u, __global float* v,
        __global float* div, float gSize) {
            
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float diff = getVal(u, i + 1, j) - getVal(u, i - 1, j) + 
                 getVal(v, i, j + 1) - getVal(v, i, j - 1);
                 
    if (diff == 0)
        return;
    
    setVal(div, i, j, diff / (gSize * 2));
}

// p0 = old pressure filed; p = updated vector filed; dt = dt;
// a = solution constant (dx * dx)
__kernel void pressureSolver(__global float* p0, __global float* p,
        __global float* div, float a) {
            
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float value = getVal(p0, i, j);
    
    // the sum of its neighbors
    float sumNbour = getVal(p0, i + 1, j) + getVal(p0, i - 1, j) +
                     getVal(p0, i, j + 1) + getVal(p0, i, j - 1);
    
    // update value
    value = (sumNbour - a * getVal(div, i, j)) / 4;
    setVal(p, i, j, value);
    //setVal(p, i, j, a * getVal(div, i, j));
}

// u, v = speed; dt = dt; gSize = grid size
__kernel void advect(__global float* u, __global float* v, 
		__global float* d0, __global float* d, float dt, float gSize) {
    int i = get_global_id(0);
    int j = get_global_id(1);
    int w = get_global_size(0);
    int h = get_global_size(1);
    
    // back track (vel unit = grid / dt)
    float2 vel = getVel(u, v, i, j);// / gSize;
    float2 prevX = ((float2) (i, j)) - dt * vel;
    
    // -1, w, h = boundary
    if (prevX.x < -1)
        prevX.x = -1;
    else if (prevX.x > w)
        prevX.x = w;
    if (prevX.y < -1)
        prevX.y = -1;
    else if (prevX.y > h)
        prevX.y = h;
    
    // previous indices
    int i0 = floor(prevX.x);
    int j0 = floor(prevX.y);
    
    // all values are real-world lengths
    float d00 = getVal(d0, i0, j0);
    float d01 = getVal(d0, i0, j0 + 1);
    float d10 = getVal(d0, i0 + 1, j0);
    float d11 = getVal(d0, i0 + 1, j0 + 1);
    
    // dx, dy = prevX's relative difference to (i0, j0) reference, calculating bilinear
    float dx = prevX.x - i0;
    float dy = prevX.y - j0;
    float value0 = (1 - dx) * d00 + dx * d10;
    float value1 = (1 - dx) * d01 + dx * d11;
    float value = (1 - dy) * value0 + dy * value1;
    
    setVal(d, i, j, value);
    
    //boundary();
}

// u0, v0 = old speed; u, v = updated speed; dt = dt; gSize = grid size;
// a = (gSize * gSize) / (viscosity * dt), solution constant
__kernel void diffuse(__global float* u0, __global float* v0,
        __global float* u, __global float* v, float a) {
            
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float2 vel = getVel(u0, v0, i, j);
    
    // the sum of its neighbors
    float2 sumNbour = getVel(u0, v0, i - 1, j) + getVel(u0, v0, i + 1, j) +
                      getVel(u0, v0, i, j + 1) + getVel(u0, v0, i, j - 1);
    //float2 sumNbour = getVel(u, v0, i - 1, j) + getVel(u, v0, i + 1, j) +
    //                  getVel(u, v0, i, j + 1) + getVel(u, v0, i, j - 1);
    
    // update vel
    vel = (sumNbour + a * vel) / (a + 4);
    //vel += (sumNbour + a * vel) / a / (1 + 4 / a);
    setVel(u, v, i, j, vel);
}

// applies force to a set of specified point
// u, v = original velocity; Fu, Fv = force vector
__kernel void applyForce(__global float* u, __global float* v,
        __global float* Fu, __global float* Fv, float dt) {
            
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float2 delta = getVel(u, v, i, j) + getVel(Fu, Fv, i, j) * dt;
    
    //u[(j + 1) * (N + 2) + (i + 1)] = delta.x;
    //v[(j + 1) * (N + 2) + (i + 1)] = delta.y;
    setVel(u, v, i, j, delta);
}

// eliminates pressure gradient in velocity field
// u, v = velocity field; p = pressure field; gSize = grid size
__kernel void elimPGrad(__global float* u, __global float* v,
        __global float* p, float gSize) {
            
    int i = get_global_id(0);
    int j = get_global_id(1);
    
    float2 vel = getVel(u, v, i, j);
    
    float2 diff = (float2) (
        (getVal(p, i + 1, j) - getVal(p, i - 1, j)), 
        (getVal(p, i, j + 1) - getVal(p, i, j - 1)));
    vel -= diff / (gSize * 2);
    
    setVel(u, v, i, j, vel);
}
