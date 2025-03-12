#version 430

layout (location = 0) in vec3 position;
layout (location = 1) in vec4 inColor;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

out vec4 vertColor; // Pass to fragment shader

void main(void) {
    gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
    vertColor = inColor;
}
