package code;

import java.nio.*;
import java.lang.Math;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import org.joml.*;
import org.joml.Vector3f;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Code extends JFrame implements GLEventListener, KeyListener 
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[4];
	
	private float cameraX, cameraY, cameraZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float pyrLocX, pyrLocY, pyrLocZ;
	private float yaw = -90.0f;   
	private float pitch = 0.0f; 
	private float rotationSpeed = 5.0f; 
	private float cameraSpeed = 0.5f;

	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f); // Initial direction
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);     // Fixed up vector
	private Vector3f cameraRight = new Vector3f();                  // Right direction

	
	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private int mvLoc, pLoc;
	private float aspect;

	public Code()
	{
		setTitle("Assignment 2");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);

		myCanvas.addKeyListener(this); // Attach key listener to canvas
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();

		setVisible(true);
		setFocusable(true);
		requestFocus();
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(renderingProgram);

		mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// Construct View Matrix with rotation
		vMat.identity();
		vMat.lookAt(
			new Vector3f(cameraX, cameraY, cameraZ),      // Camera position
			new Vector3f(cameraX, cameraY, cameraZ).add(cameraFront), // Look-at target
			cameraUp  // Up direction
		);



		// draw the cube using buffer #0
		mMat.translation(cubeLocX, cubeLocY, cubeLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		
		// draw the pyramid using buffer #1
		mMat.translation(pyrLocX, pyrLocY, pyrLocZ);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glDisableVertexAttribArray(1);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 12);

		// draw the axis lines using buffer #2
		mvMat.identity();
		mvMat.mul(vMat);  // if you want the axes to be fixed relative to the world, you might not apply mMat here
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		// Bind the axis color buffer and set attribute 1 (make sure your vertex shader supports this)
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(1, 4, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glDrawArrays(GL_LINES, 0, 6);



	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		renderingProgram = Utils.createShaderProgram("code/vertShader.glsl", "code/fragShader.glsl");
		setupVertices();
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
		cubeLocX = 0.0f; cubeLocY = -2.0f; cubeLocZ = 0.0f;
		pyrLocX = 2.0f; pyrLocY = 2.0f; pyrLocZ = 0.0f;
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		float[] cubePositions =
		{	-1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f,  1.0f, -1.0f, -1.0f,  1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, 1.0f, -1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f, 1.0f,  1.0f, -1.0f,
			1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f,  1.0f,  1.0f, 1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f, -1.0f, -1.0f,  1.0f, -1.0f, -1.0f,  1.0f,  1.0f,
			-1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f, -1.0f,
			1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f,  1.0f,
			-1.0f,  1.0f, -1.0f, 1.0f,  1.0f, -1.0f, 1.0f,  1.0f,  1.0f,
			1.0f,  1.0f,  1.0f, -1.0f,  1.0f,  1.0f, -1.0f,  1.0f, -1.0f
		};
		
		float[] pyramidPositions = {
			// Front triangle
			-1.0f, -1.0f,  1.0f,  // A: left-front base
			 1.0f, -1.0f,  1.0f,  // B: right-front base
			 0.0f,  1.0f,  1.0f,  // Apex for front triangle
		
			// Right triangle
			 1.0f, -1.0f,  1.0f,  // B: right-front base
			 1.0f, -1.0f, -1.0f,  // C: right-back base
			 1.0f,  1.0f,  0.0f,  // Apex for right triangle
		
			// Back triangle
			 1.0f, -1.0f, -1.0f,  // C: right-back base
			-1.0f, -1.0f, -1.0f,  // D: left-back base
			 0.0f,  1.0f, -1.0f,  // Apex for back triangle
		
			// Left triangle
			-1.0f, -1.0f, -1.0f,  // D: left-back base
			-1.0f, -1.0f,  1.0f,  // A: left-front base
			-1.0f,  1.0f,  0.0f   // Apex for left triangle
		};

		float[] axisPositions = {
            0.0f, 0.0f, 0.0f,  10.0f, 0.0f, 0.0f, // X-axis (Red)
            0.0f, 0.0f, 0.0f,  0.0f, 10.0f, 0.0f, // Y-axis (Green)
            0.0f, 0.0f, 0.0f,  0.0f, 0.0f, 10.0f  // Z-axis (Blue)
        };

		float[] axisColors = {
			// X-axis: Red
			1.0f, 0.0f, 0.0f, 1.0f,  // start vertex
			1.0f, 0.0f, 0.0f, 1.0f,  // end vertex
			// Y-axis: Green
			0.0f, 1.0f, 0.0f, 1.0f,  // start vertex
			0.0f, 1.0f, 0.0f, 1.0f,  // end vertex
			// Z-axis: Blue
			0.0f, 0.0f, 1.0f, 1.0f,  // start vertex
			0.0f, 0.0f, 1.0f, 1.0f   // end vertex
		};

		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer pyrBuf = Buffers.newDirectFloatBuffer(pyramidPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrBuf.limit()*4, pyrBuf, GL_STATIC_DRAW);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);  // Bind the third buffer for axis
		FloatBuffer axisBuf = Buffers.newDirectFloatBuffer(axisPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, axisBuf.limit() * 4, axisBuf, GL_STATIC_DRAW);

		// Bind and upload axis colors (vbo[3])
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer axisColorBuf = Buffers.newDirectFloatBuffer(axisColors);
		gl.glBufferData(GL_ARRAY_BUFFER, axisColorBuf.limit() * 4, axisColorBuf, GL_STATIC_DRAW);

	}
	@Override
	public void keyPressed(KeyEvent e) {
		updateCameraVectors(); // Ensure direction vectors are updated
	
		switch (e.getKeyCode()) {
			// Move forward/backward
			case KeyEvent.VK_W:
				cameraX += cameraFront.x * cameraSpeed;
				cameraY += cameraFront.y * cameraSpeed;
				cameraZ += cameraFront.z * cameraSpeed;
				break;
			case KeyEvent.VK_S:
				cameraX -= cameraFront.x * cameraSpeed;
				cameraY -= cameraFront.y * cameraSpeed;
				cameraZ -= cameraFront.z * cameraSpeed;
				break;
			// Move left/right (strafe)
			case KeyEvent.VK_A:
				cameraX -= cameraRight.x * cameraSpeed;
				cameraY -= cameraRight.y * cameraSpeed;
				cameraZ -= cameraRight.z * cameraSpeed;
				break;
			case KeyEvent.VK_D:
				cameraX += cameraRight.x * cameraSpeed;
				cameraY += cameraRight.y * cameraSpeed;
				cameraZ += cameraRight.z * cameraSpeed;
				break;
			// Move up/down
			case KeyEvent.VK_Q:
				cameraY += cameraSpeed;
				break;
			case KeyEvent.VK_E:
				cameraY -= cameraSpeed;
				break;
			// Rotate camera
			case KeyEvent.VK_LEFT:
				yaw -= rotationSpeed;
				break;
			case KeyEvent.VK_RIGHT:
				yaw += rotationSpeed;
				break;
			case KeyEvent.VK_UP:
				pitch += rotationSpeed;
				break;
			case KeyEvent.VK_DOWN:
				pitch -= rotationSpeed;
				break;
		}
		// Limit pitch to prevent flipping
		pitch = Math.max(-89.0f, Math.min(89.0f, pitch));

		updateCameraVectors();
		myCanvas.display();
	}

	private void updateCameraVectors() {
		// Calculate the new front vector
		float yawRad = (float) Math.toRadians(yaw);
		float pitchRad = (float) Math.toRadians(pitch);
	
		cameraFront.x = (float) (Math.cos(yawRad) * Math.cos(pitchRad));
		cameraFront.y = (float) Math.sin(pitchRad);
		cameraFront.z = (float) (Math.sin(yawRad) * Math.cos(pitchRad));
	
		cameraFront.normalize();  // Ensure it's a unit vector
	
		// Recalculate the right vector as the cross product of front and world up
		cameraRight.set(cameraFront).cross(cameraUp).normalize();
	}

    @Override
    public void keyReleased(KeyEvent e) {}
    @Override
    public void keyTyped(KeyEvent e) {}
    

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}
}