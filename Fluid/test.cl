__kernel void lll(__global float* a)
{
    a[get_global_id(0)] = 0;
}

int main () {
    
    return 0;
}