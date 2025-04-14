#version 430

layout (location = 0) in vec3 vertPos;
layout (location = 1) in vec2 vertTexCoord; 
layout (location = 2) in vec3 vertNormal;
out vec3 varyingNormal;
out vec3 varyingLightDir;
out vec3 varyingVertPos;
out vec3 varyingHalfVector;
out vec2 varyingTexCoord;

// Uniform flag: if true, we are rendering a skybox.
uniform bool u_skybox;
uniform int u_colorFlag;
uniform vec4 u_color;

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
	varyingTexCoord = vertTexCoord;
    
    if(u_skybox)
    {
        // For the skybox, ignore the model matrix.
        // Remove the translation part from the view matrix so the cube always stays centered around the camera.
        //mat3 viewRotation = mat3(v_matrix);
        //mat4 viewNoTrans = mat4(viewRotation);
        gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos, 1.0);
        
        // No lighting for the skybox; set dummy values.
        varyingNormal = vec3(0.0);
        varyingLightDir = vec3(0.0);
        varyingVertPos = vec3(0.0);
        varyingHalfVector = vec3(0.0);
    }
    else
    {
		varyingVertPos = (m_matrix * vec4(vertPos,1.0)).xyz;
		varyingLightDir = light.position - varyingVertPos;
		varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
		
		varyingHalfVector = normalize(normalize(varyingLightDir) + normalize(-varyingVertPos)).xyz;

		gl_Position = p_matrix * v_matrix * m_matrix * vec4(vertPos,1.0);
	}
}
