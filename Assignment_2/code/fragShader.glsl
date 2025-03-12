#version 430

in vec4 vertColor;
out vec4 fragColor;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

void main(void)
{   fragColor = vertColor;
}
