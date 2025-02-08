package code;

import javax.swing.*;

import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_VERSION;
import static com.jogamp.opengl.GL2ES2.GL_SHADING_LANGUAGE_VERSION;
import static com.jogamp.opengl.GL4.*;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import java.awt.event.KeyListener;


public class Code extends JFrame implements GLEventListener, KeyListener
{	
	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private boolean circle = true;
	private float x, y, radius;
	private float z = 1;
	private int w, colorCounter;
	private float inc = 0.01f;
	private double tf, elapsedTime, startTime;
	private float[] colors = {
		0.0f, 0.0f, 1.0f, 1.0f, 
		0.0f, 0.0f, 1.0f, 1.0f,
		0.0f, 0.0f, 1.0f, 1.0f 
	};

	public Code()
	{	setTitle("Chapter 2 - program 6");
		setSize(700, 700);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		this.setVisible(true);

		myCanvas.setFocusable(true);
		myCanvas.addKeyListener(this);

		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	@Override
	public void keyPressed(java.awt.event.KeyEvent e) {
		switch (e.getKeyCode()) {
			case java.awt.event.KeyEvent.VK_1:
				circle = !circle;
				x = 0;
				y = 0;
				startTime = System.currentTimeMillis();
				break;
			case java.awt.event.KeyEvent.VK_2: 
				if (colorCounter == 0) {
					colors = new float[]{
						1.0f, 1.0f, 0.0f, 1.0f,  
						1.0f, 1.0f, 0.0f, 1.0f, 
						1.0f, 1.0f, 0.0f, 1.0f
					};
				}	
				else if(colorCounter == 1 ){
					colors = new float[]{
						0.5f, 0.0f, 1.0f, 1.0f,  
						0.5f, 0.0f, 1.0f, 1.0f, 
						0.5f, 0.0f, 1.0f, 1.0f
					};
				}
				else if(colorCounter == 2 ){
					colors = new float[]{
						1.0f, 0.0f, 0.0f, 1.0f,  
						0.0f, 1.0f, 0.0f, 1.0f, 
						0.0f, 0.0f, 1.0f, 1.0f
					};
				}
				colorCounter = (colorCounter + 1) % 3;
				break;
			case java.awt.event.KeyEvent.VK_3: 
				z = z + 0.25f;
				break;
			case java.awt.event.KeyEvent.VK_4: 
				if(z >= 0.5f){
				z = z - 0.25f;
				}
				break;
			case java.awt.event.KeyEvent.VK_5: 
				w = (w+1) % 4;
				break;
		}
	}
	@Override
	public void keyReleased(java.awt.event.KeyEvent e) {}

	@Override
	public void keyTyped(java.awt.event.KeyEvent e) {}

	
	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(renderingProgram);

		elapsedTime = System.currentTimeMillis() - startTime; // elapsedTime, startTime, and tf
 		tf = elapsedTime / 1000.0;  // would all be declared of type double.

		if(circle){
			x = 2 * radius * (float)Math.cos(tf);
			y = 2 * radius * (float)Math.sin(tf);
		}
		else {
			x += inc;  
			if (x > 1.0f) inc = -(float)tf; 
			if (x < -1.0f) inc = (float)tf; 
			startTime = System.currentTimeMillis();
		}
		int offsetLoc = gl.glGetUniformLocation(renderingProgram, "offset");
		gl.glProgramUniform4f(renderingProgram, offsetLoc, x, y, z, w);

		// Get location of 'colors' uniform in shader
		int colorsLoc = gl.glGetUniformLocation(renderingProgram, "colors");

		// Send array of colors to shader
		gl.glUniform4fv(colorsLoc, 3, colors, 0);

		gl.glDrawArrays(GL_TRIANGLES,0,3);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		renderingProgram = Utils.createShaderProgram("code/vertShader.glsl", "code/fragShader.glsl");
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		radius = 0.3f;
		startTime = System.currentTimeMillis();



		// Get and display OpenGL version
		String openGLVersion = gl.glGetString(GL_VERSION);
		String joglVersion = Package.getPackage("com.jogamp.opengl").getImplementationVersion();
		System.out.println("OpenGL Version: " + openGLVersion);
		System.out.println("JOGL Version: " + joglVersion);
	}

	public static void main(String[] args) { new Code(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}


}