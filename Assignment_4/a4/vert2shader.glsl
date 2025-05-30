#version 430

layout (location=0) in vec3 vertPos;
layout (location=1) in vec3 vertNormal;
layout(location=2) in vec2 vertTexCoord;


out vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec; 
out vec4 shadow_coord;
out vec2 texCoord;
out float fogDepth;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};
struct Material
{	vec4 ambient, diffuse, specular;   
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform float alpha;
uniform float flipNormal;
layout (binding=1) uniform sampler2DShadow shadowTex;


void main(void)
{	//output the vertex position to the rasterizer for interpolation
	varyingVertPos = (m_matrix * vec4(vertPos,1.0)).xyz;
        
	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	
	//if rendering a back-face, flip the normal
	if (flipNormal < 0) varyingNormal = -varyingNormal;

	// calculate the half vector (L+V)
	varyingHalfVec = (varyingLightDir-varyingVertPos).xyz;
	
	shadow_coord = shadowMVP * vec4(vertPos,1.0);
	
 	texCoord = vertTexCoord;

	gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
	vec4 dist = v_matrix * m_matrix * vec4(vertPos,1.0);
	fogDepth = -dist.z;
}
