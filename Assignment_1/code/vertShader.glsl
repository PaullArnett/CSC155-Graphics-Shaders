#version 430

uniform vec4 offset;
uniform vec4 colors[3];

out vec4 fragColor;

void main(void)
<<<<<<< HEAD
{
    if (offset.w == 0){
      if (gl_VertexID == 0)
          gl_Position = vec4( 0.25*offset.z + offset.x, -0.05*offset.z + offset.y, 0.0, 1.0);
      else if (gl_VertexID == 1) 
          gl_Position = vec4(-0.25*offset.z + offset.x, -0.05*offset.z + offset.y, 0.0, 1.0);
      else 
          gl_Position = vec4( 0.0*offset.z + offset.x, 0.10*offset.z + offset.y, 0.0, 1.0);
    }
    else if (offset.w == 1){
      if (gl_VertexID == 0) 
          gl_Position = vec4( -0.05*offset.z + offset.x, -0.25*offset.z + offset.y, 0.0, 1.0);
      else if (gl_VertexID == 1) 
          gl_Position = vec4(-0.05*offset.z + offset.x, 0.25*offset.z + offset.y, 0.0, 1.0);
      else 
          gl_Position = vec4( 0.1*offset.z + offset.x, 0.0*offset.z + offset.y, 0.0, 1.0);
    }
    else if (offset.w == 2){
      if (gl_VertexID == 0) 
          gl_Position = vec4( 0.25*offset.z + offset.x, 0.05*offset.z + offset.y, 0.0, 1.0);
      else if (gl_VertexID == 1) 
          gl_Position = vec4(-0.25*offset.z + offset.x, 0.05*offset.z + offset.y, 0.0, 1.0);
      else 
          gl_Position = vec4( 0.0*offset.z + offset.x, -0.10*offset.z + offset.y, 0.0, 1.0);
    }
    else if (offset.w == 3){
      if (gl_VertexID == 0) 
          gl_Position = vec4( 0.05*offset.z + offset.x, -0.25*offset.z + offset.y, 0.0, 1.0);
      else if (gl_VertexID == 1) 
          gl_Position = vec4( 0.05*offset.z + offset.x, 0.25*offset.z + offset.y, 0.0, 1.0);
      else 
          gl_Position = vec4( -0.1*offset.z + offset.x, 0.0*offset.z + offset.y, 0.0, 1.0);
    }

    if (gl_VertexID == 0)
      fragColor = colors[0];
    else if (gl_VertexID == 1)
      fragColor = colors[1];
    else 
      fragColor = colors[2];
=======
{ if (gl_VertexID == 0) gl_Position = vec4( 0.25+offset,-0.2, 0.0, 1.0);
  else if (gl_VertexID == 1) gl_Position = vec4(-0.25+offset,-0.2, 0.0, 1.0);
  else gl_Position = vec4( offset, 0, 0.0, 1.0);
>>>>>>> 22e68e69b76fb17709e5260020be1d23826586b1
}