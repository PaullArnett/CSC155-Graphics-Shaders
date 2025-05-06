#version 430

in vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec;
in vec4 shadow_coord;
in vec2 texCoord;

out vec4 fragColor;

uniform bool u_texture; 
uniform vec4 u_color;
 
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

layout (binding=1) uniform sampler2DShadow shadowTex;
uniform sampler2D textureMap;

void main(void)
{	vec3 L = normalize(varyingLightDir);
	vec3 N = normalize(varyingNormal);
	vec3 V = normalize(-v_matrix[3].xyz * mat3(v_matrix) - varyingVertPos);
	vec3 H = normalize(varyingHalfVec);
	
	float notInShadow = textureProj(shadowTex, shadow_coord);
	
	
	fragColor = globalAmbient * material.ambient
				+ light.ambient * material.ambient;


	//vec3 ambient = globalAmbient.rgb * material.ambient.rgb + light.ambient.rgb * material.ambient.rgb;

	if (u_texture)
	{
		fragColor = texture(textureMap, texCoord);
	}
	else
	{
		fragColor = u_color;
	}

	if (notInShadow == 1.0)
	{	fragColor += light.diffuse * material.diffuse * max(dot(L,N),0.0)
				+ light.specular * material.specular
				* pow(max(dot(H,N),0.0),material.shininess*3.0);
	}


	
}
