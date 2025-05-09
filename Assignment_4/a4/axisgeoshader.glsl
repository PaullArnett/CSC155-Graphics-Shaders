#version 430 core

layout(lines) in;
layout(line_strip, max_vertices = 2) out;

uniform vec4 u_color;

out vec4 gsColor;

void main() {
    for(int i = 0; i < 2; ++i) {
        gsColor   = u_color;
        gl_Position = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}
