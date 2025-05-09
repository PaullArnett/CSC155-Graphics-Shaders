#version 430

in vec3 varyingNormalG, varyingLightDirG, varyingVertPosG, varyingHalfVecG;
in vec4 shadow_coordG;
in vec2 texCoordG;
in float fogDepthG;

out vec4 fragColor;

uniform bool u_texture; 
uniform bool u_skybox; 
uniform bool u_fog; 
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
uniform float alpha;
uniform float flipNormal;

layout (binding=1) uniform sampler2DShadow shadowTex;
uniform sampler2D textureMap;

float lookup(float x, float y)
{  	float t = textureProj(shadowTex, shadow_coordG + vec4(x * 0.001 * shadow_coordG.w,
                                                         y * 0.001 * shadow_coordG.w,
                                                         -0.01, 0.0));
	return t;
}

void main(void)
{	
	if (u_skybox)
	{
		fragColor = texture(textureMap, texCoordG);
	}
	else if (!u_texture)
	{
		fragColor = u_color;
	}
	else
	{
		float shadowFactor=0.0;

		vec3 L = normalize(varyingLightDirG);
		vec3 N = normalize(varyingNormalG);
		vec3 V = normalize(-v_matrix[3].xyz - varyingVertPosG);
		vec3 H = normalize(varyingHalfVecG);
		

		float swidth = 2.5;
		vec2 o = mod(floor(gl_FragCoord.xy), 2.0) * swidth;
		shadowFactor += lookup(-1.5*swidth + o.x,  1.5*swidth - o.y);
		shadowFactor += lookup(-1.5*swidth + o.x, -0.5*swidth - o.y);
		shadowFactor += lookup( 0.5*swidth + o.x,  1.5*swidth - o.y);
		shadowFactor += lookup( 0.5*swidth + o.x, -0.5*swidth - o.y);
		shadowFactor = shadowFactor / 4.0;

		// hi res PCF
		float width = 2.5;
		float endp = width * 3.0 + width/2.0;
		for (float m=-endp ; m<=endp ; m=m+width)
		{	for (float n=-endp ; n<=endp ; n=n+width)
			{	shadowFactor += lookup(m,n);
		}	}
		shadowFactor = shadowFactor / 64.0;
	
		// this would produce normal hard shadows
	//	shadowFactor = lookup(0.0, 0.0);

		vec4 shadowColor = globalAmbient * material.ambient
					+ light.ambient * material.ambient;

		if (u_texture)
		{
			shadowColor = texture(textureMap, texCoordG);
		}
		
		vec4 lightedColor = light.diffuse * material.diffuse * max(dot(L,N),0.0)
					+ light.specular * material.specular
					* pow(max(dot(H,N),0.0),material.shininess*3.0);
		
		fragColor = vec4((shadowColor.xyz + shadowFactor*(lightedColor.xyz)),1.0);
	}

	if(u_fog)
	{
		//Fog
		vec4 fogColor = vec4(0.6, 0.45, 0.35, 1.0);
		float fogStart = 8.0;
		float fogEnd = 30.0;

		float fogFactor = clamp(((fogEnd-fogDepthG)/(fogEnd-fogStart)), 0.0, 1.0);
		fragColor = mix(fogColor,fragColor,fogFactor);
	}

	//transparency
	fragColor = vec4(fragColor.xyz, alpha);
}
