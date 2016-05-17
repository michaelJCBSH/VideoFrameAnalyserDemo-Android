#pragma version(1)
#pragma rs java_package_name(com.bignerdranch.android.eora3d)

const static float3 gMonoMult = {0.333f, 0.333f, 0.334f};
//const char* message = "new value"

float saturationValue = 0.f;

/*
RenderScript kernel that performs saturation manipulation.
*/
uchar4 __attribute__((kernel)) saturation(uchar4 in)
{
    float4 f4 = rsUnpackColor8888(in);
    float3 result = dot(f4.rgb, gMonoMult);

    //rsDebug("f4", f4);
    //rsDebug("result", result);
    result = mix( result, f4.rgb, saturationValue );
    //rsDebug("saturationValue", saturationValue);
    //rsDebug("result mix", result);
    return rsPackColorTo8888(result);
}
