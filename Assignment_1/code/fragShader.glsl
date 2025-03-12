#version 430

//applies color given by the verShader

in vec4 fragColor;
out vec4 color;

void main(void)
{
	color = fragColor;
}