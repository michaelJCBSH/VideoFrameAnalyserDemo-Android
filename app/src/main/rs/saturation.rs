#pragma version(1)
#pragma rs java_package_name(com.bignerdranch.android.eora3d)
#pragma rs_fp_relaxed
//#include "rs_debug.rsh"

const static float3 gMonoMult = {0.333f, 0.333f, 0.334f};
//const char* message = "new value"


/*
RenderScript kernel that performs saturation manipulation.
*/
uchar4 __attribute__((kernel)) saturation(uchar4 in)
{
    float4 f4 = rsUnpackColor8888(in);
    float3 result = dot(f4.rgb, gMonoMult);
    return rsPackColorTo8888(result);
}

//f4 {0.588235, 0.196078, 0.000000, 1.000000}
//result {0.261176, 0.261176, 0.261176}
//result mix {0.588235, 0.196078, 0.000000}

//saturationValue 1.000000, 0x3f800000


//f4 {0.588235, 0.196078, 0.000000, 1.000000}
//result {0.261176, 0.261176, 0.261176}
//result mix {0.627482, 0.188267, -0.031341}


//f4 {0.588235, 0.196078, 0.000000, 1.000000}
//result {0.261176, 0.261176, 0.261176}
//saturationValue 1.380000, 0x3fb0a3d7
//result mix {0.712518, 0.171341, -0.099247}











