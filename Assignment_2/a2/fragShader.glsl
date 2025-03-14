#version 430

in vec2 tc;
in vec3 vColor;
out vec4 color;

uniform sampler2D s;    // Texture sampler (used for shuttle and crown)
uniform bool useTexture;

void main(void)
{
    if (useTexture)
    {
        color = texture(s, tc);
    }
    else
    {
        color = vec4(vColor, 1.0);
    }
}