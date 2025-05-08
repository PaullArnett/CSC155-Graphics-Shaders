#version 430 core

// Tell the driver to only feed us lines
layout(lines) in;
// And spit them back out as a 2-vertex line strip
layout(line_strip, max_vertices = 2) out;

// We’ll use the existing fragment shader’s uniform for color
uniform vec4 u_color;

// Pass the color along to the FS stage
out vec4 gsColor;

void main() {
    // For each of the two endpoints of the incoming line...
    for(int i = 0; i < 2; ++i) {
        gsColor   = u_color;
        // gl_in[i].gl_Position was already computed by your vertex shader
        gl_Position = gl_in[i].gl_Position;
        EmitVertex();
    }
    EndPrimitive();
}
