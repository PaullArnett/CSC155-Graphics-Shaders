#version 430

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 tex_coord;  // For shuttle/crown textured objects
layout (location = 2) in vec3 vertexColor;  // For axis lines

out vec2 tc;
out vec3 vColor;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;
uniform bool useTexture;

void main(void)
{
    gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
    tc = tex_coord;
    vColor = vertexColor;
}