#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec3 varyingHalfVector;
in vec2 varyingTexCoord;
in float fogDepth;

uniform sampler2D texture_sampler;
uniform bool u_skybox;
uniform int u_colorFlag;   // 0 = texture sampling, 1 = use solid color
uniform vec4 u_color;

out vec4 fragColor;

struct PositionalLight
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	vec3 position;
};

struct Material
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 m_matrix;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform mat4 norm_matrix;

void main(void)
{	
	if(u_skybox)
    {
        // For skybox mode, simply sample the texture.
        fragColor = texture(texture_sampler, varyingTexCoord);
		//fragColor = vec4(0.0, 0.0, 1.0, 1.0);
    }
	else if(u_colorFlag == 1) {
        fragColor = u_color;
    } 
    else
    {
		// normalize the light, normal, and view vectors:
		vec3 L = normalize(varyingLightDir);
		vec3 N = normalize(varyingNormal);
		vec3 V = normalize(-v_matrix[3].xyz - varyingVertPos);
		
		// get the angle between the light and surface normal:
		float cosTheta = dot(L,N);
		
		// halfway vector varyingHalfVector was computed in the vertex shader,
		// and interpolated prior to reaching the fragment shader.
		// It is copied into variable H here for convenience later.
		vec3 H = normalize(varyingHalfVector);
		
		// get angle between the normal and the halfway vector
		float cosPhi = dot(H,N);

		// compute ADS contributions (per pixel):
		vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
		vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
		vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess*3.0);
		fragColor = vec4((ambient + diffuse + specular), 1.0);

		// Combine lighting contributions
		vec3 lightColor = ambient + diffuse + specular;
		
		// Sample the texture with the interpolated texture coordinates
		vec4 texColor = texture(texture_sampler, varyingTexCoord);
		
		// Multiply the lighting with the texture for the final color
		fragColor = vec4(lightColor, 1.0) * texColor;
	}

	//Fog
	vec4 fogColor = vec4(0.7, 0.8, 0.9, 1.0);	// bluish gray
	float fogStart = 0.2;
	float fogEnd = 0.8;

	float fogFactor = clamp(((fogEnd-fFogDepth)/(fogEnd-fogStart)), 0.0, 1.0);
	fragColor = mix(fogColor,fragColor,fogFactor);


}
