#version 430
layout(triangles) in;
layout(triangle_strip, max_vertices = 3) out;

// VS→GS varyings (arrays):
in  vec3  varyingNormal[];
in  vec3  varyingLightDir[];
in  vec3  varyingVertPos[];  
in  vec3  varyingHalfVec[];
in  vec4  shadow_coord[];
in  vec2  texCoord[];
in  float fogDepth[];

// GS→FS varyings:
out vec3  varyingNormalG;
out vec3  varyingLightDirG;
out vec3  varyingVertPosG;
out vec3  varyingHalfVecG;
out vec4  shadow_coordG;
out vec2  texCoordG;
out float fogDepthG;

// uniforms you already have:
uniform mat4 v_matrix;
uniform mat4 p_matrix;

uniform bool u_explode;

void main() {

    for(int i = 0; i < 3; ++i) {

        vec4 viewPos;
        if(u_explode)
        {
            vec3 avgN = normalize((varyingNormal[0] + varyingNormal[1] + varyingNormal[2]) / 3.0);
            vec4 worldPosOffset = vec4(varyingVertPos[i] + avgN * 0.8, 1.0);
            viewPos = v_matrix * worldPosOffset;
        }
        else
        {
            viewPos = v_matrix * vec4(varyingVertPos[i], 1.0);
        }

        gl_Position = p_matrix * viewPos;

        varyingNormalG   = varyingNormal[i];
        varyingLightDirG = varyingLightDir[i];
        varyingVertPosG  = varyingVertPos[i];
        varyingHalfVecG  = varyingHalfVec[i];
        shadow_coordG    = shadow_coord[i];
        texCoordG        = texCoord[i];
        fogDepthG        = fogDepth[i];

        EmitVertex();
    }
    EndPrimitive();
}
