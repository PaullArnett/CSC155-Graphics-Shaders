package a2;

import java.io.*;
import java.lang.Math;
import java.nio.*;
import javax.swing.*;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.common.nio.Buffers;
import org.joml.*;
import org.joml.Vector3f;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import com.jogamp.opengl.util.FPSAnimator;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[12];
	private float cameraX, cameraY, cameraZ;
	private float objLocX, objLocY, objLocZ;
	private float sphLocX, sphLocY, sphLocZ;
	private float cubeLocX, cubeLocY, cubeLocZ;
	private float crownLocX, crownLocY, crownLocZ;

	private float yaw = -90.0f;   
	private float pitch = -15.0f; 
	private float rotationSpeed = 3.0f; 
	private float cameraSpeed = 0.3f;
	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f); // Initial direction
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);     // Fixed up vector
	private Vector3f cameraRight = new Vector3f();                  // Right direction
	private float crownAngle = 0.0f;
	private float shuttleOrbitAngle, cubeOrbitAngle;
	private boolean showAxes = true;
	private long lastTime = System.currentTimeMillis();
	
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private int mvLoc, pLoc;
	private float aspect;
	
	private int shuttleTexture;
	private int crownTexture;
	private int earthTexture;
	private int bonnieTexture;
	private int numObjVertices;
	private ImportedModel myModel;
	private Sphere mySphere;
	private int numSphereVerts;

	public Code()
	{	setTitle("Assignment 2");
		setSize(600, 600);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		myCanvas.addKeyListener(this); 
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();

		// FPS Animator for object movement
		FPSAnimator animator = new FPSAnimator(myCanvas, 60);
		animator.start();

		setVisible(true);
		setFocusable(true);
		this.setVisible(true);
	}

	public void display(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glUseProgram(renderingProgram);

		// Calculate elapsed time
		long currentTime = System.currentTimeMillis();
		float deltaTime = (currentTime - lastTime) / 1000.0f;  // in seconds
		lastTime = currentTime;

		// Update crown rotation 
		crownAngle += 30.0f * deltaTime;
		if (crownAngle >= 360.0f) {
			crownAngle -= 360.0f;
		}
		// Update the orbit angle
		shuttleOrbitAngle += 45.0f * deltaTime;
		if (shuttleOrbitAngle >= 360.0f) {
			shuttleOrbitAngle -= 360.0f;
		}
		// Update the orbit angle
		cubeOrbitAngle += 10.0f * deltaTime;
		if (cubeOrbitAngle >= 360.0f) {
			cubeOrbitAngle -= 360.0f;
		}

		int mvLoc = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
		int pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		int useTextureLoc = gl.glGetUniformLocation(renderingProgram, "useTexture");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		// Construct View Matrix with rotation
		vMat.identity();
		vMat.lookAt(
			new Vector3f(cameraX, cameraY, cameraZ),     
			new Vector3f(cameraX, cameraY, cameraZ).add(cameraFront), 
			cameraUp  // Up direction
		);

		//Draw Shuttle
		mMat.identity();
		mMat.rotateY((float)Math.toRadians(shuttleOrbitAngle));
		mMat.translate(objLocX, objLocY, objLocZ);
		mMat.scale(1.5f);
		mMat.rotateY((float)Math.toRadians(45.0f));

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glUniform1i(useTextureLoc, 1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shuttleTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, myModel.getNumVertices());

		// Draw the Crown
		mMat.identity();
		mMat.translation(crownLocX, crownLocY, crownLocZ);
		mMat.rotateY((float)Math.toRadians(crownAngle));
		mMat.scale(0.8f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glUniform1i(useTextureLoc, 1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
	
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, crownTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 12);

		// Draw XYZ axes with RGB
		if (showAxes) {
			mMat.identity();
			mvMat.identity();
			mvMat.mul(vMat);  
			mvMat.mul(mMat);
			gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
			gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

			gl.glUniform1i(useTextureLoc, 0);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
			gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(0);

			gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
			gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(2);
			gl.glDisableVertexAttribArray(1);

			gl.glEnable(GL_DEPTH_TEST);
			gl.glDepthFunc(GL_LEQUAL);
			gl.glDrawArrays(GL_LINES, 0, 6);
		}

		//Draw Sphere
		mMat.identity();
		mMat.translate(sphLocX, sphLocY, sphLocZ);
		mMat.scale(1.5f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glUniform1i(useTextureLoc, 1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, earthTexture);

		gl.glFrontFace(GL_CCW);
		gl.glDrawArrays(GL_TRIANGLES, 0, numSphereVerts);

		// draw the cube
		mMat.identity();
		mMat.rotateY((float)Math.toRadians(cubeOrbitAngle));
		mMat.translate(cubeLocX, cubeLocY, cubeLocZ);  // First, translate the cube to its position
		mMat.scale(0.3f);
		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));

		gl.glUniform1i(useTextureLoc, 1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, bonnieTexture);

		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		myModel = new ImportedModel("shuttle.obj");
		renderingProgram = Utils.createShaderProgram("a2/vertShader.glsl", "a2/fragShader.glsl");

		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();

		//initial locations
		cameraX = 0.0f; cameraY = 3.0f; cameraZ = 10.0f;
		objLocX = 5.0f; objLocY = 0.0f; objLocZ = 0.0f;
		crownLocX = 0.0f; crownLocY = 2.0f; crownLocZ = 0.0f;
		sphLocX = 0.0f; sphLocY = 0.0f; sphLocZ = 0.0f;
		cubeLocX = -2.0f; cubeLocY = 0.0f; cubeLocZ = 0.0f;

		shuttleTexture = Utils.loadTexture("spstob_1.jpg");
		crownTexture = Utils.loadTexture("Crown.jpg");
		earthTexture = Utils.loadTexture("earth.jpg");
		bonnieTexture = Utils.loadTexture("bonnie.jpg");

		//have the bonnie texture implent tiling
		gl.glBindTexture(GL_TEXTURE_2D, bonnieTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		updateCameraVectors();
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		//Shuttle vertices
		int numShuttleVerts = myModel.getNumVertices();
		Vector3f[] shuttleVertices = myModel.getVertices();
		Vector2f[] shuttleTexCoords = myModel.getTexCoords();
		Vector3f[] shuttleNormals = myModel.getNormals();
		
		float[] shuttlePvalues = new float[numShuttleVerts * 3];
		float[] shuttleTvalues = new float[numShuttleVerts * 2];
		float[] shuttleNvalues = new float[numShuttleVerts * 3];
		
		for (int i = 0; i < numShuttleVerts; i++) {
			shuttlePvalues[i*3]   = shuttleVertices[i].x();
			shuttlePvalues[i*3+1] = shuttleVertices[i].y();
			shuttlePvalues[i*3+2] = shuttleVertices[i].z();
			shuttleTvalues[i*2]   = shuttleTexCoords[i].x();
			shuttleTvalues[i*2+1] = shuttleTexCoords[i].y();
			shuttleNvalues[i*3]   = shuttleNormals[i].x();
			shuttleNvalues[i*3+1] = shuttleNormals[i].y();
			shuttleNvalues[i*3+2] = shuttleNormals[i].z();
		}

		// Sphere vertices
		mySphere = new Sphere(96);
		numSphereVerts = mySphere.getIndices().length;
	
		int[] indices = mySphere.getIndices();
		Vector3f[] vert = mySphere.getVertices();
		Vector2f[] tex  = mySphere.getTexCoords();
		Vector3f[] norm = mySphere.getNormals();
		
		float[] pvalues = new float[indices.length*3];
		float[] tvalues = new float[indices.length*2];
		float[] nvalues = new float[indices.length*3];
		
		for (int i=0; i<indices.length; i++)
		{	pvalues[i*3] = (float) (vert[indices[i]]).x;
			pvalues[i*3+1] = (float) (vert[indices[i]]).y;
			pvalues[i*3+2] = (float) (vert[indices[i]]).z;
			tvalues[i*2] = (float) (tex[indices[i]]).x;
			tvalues[i*2+1] = (float) (tex[indices[i]]).y;
			nvalues[i*3] = (float) (norm[indices[i]]).x;
			nvalues[i*3+1]= (float)(norm[indices[i]]).y;
			nvalues[i*3+2]=(float) (norm[indices[i]]).z;
		}

		//crown vertices
		float[] crownPositions = {
			// Front triangle
			-1.0f, -1.0f,  1.0f, 
			 1.0f, -1.0f,  1.0f, 
			 0.0f,  1.0f,  1.0f, 
		
			// Right triangle
			 1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f, -1.0f, 
			 1.0f,  1.0f,  0.0f, 
		
			// Back triangle
			 1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f, -1.0f, 
			 0.0f,  1.0f, -1.0f, 
		
			// Left triangle
			-1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f,  1.0f, 
			-1.0f,  1.0f,  0.0f   
		};
		
		// Crown texture coordinates
		float[] crownTexCoords = {
			// Front triangle
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.5f, 1.0f,
			// Right triangle
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.5f, 1.0f,
			// Back triangle
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.5f, 1.0f,
			// Left triangle
			0.0f, 0.0f,
			1.0f, 0.0f,
			0.5f, 1.0f
		};

		// Axis lines
		float[] axisPositions = {
			// X-axis (red)
			0.0f, 0.0f, 0.0f,
			10.0f, 0.0f, 0.0f,
			// Y-axis (green)
			0.0f, 0.0f, 0.0f,
			0.0f, 10.0f, 0.0f,
			// Z-axis (blue)
			0.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 10.0f
		};

		// Axis colors
		float[] axisColors = {
			// X-axis: red 
			1.0f, 0.0f, 0.0f,
			1.0f, 0.0f, 0.0f,
			// Y-axis: green 
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			// Z-axis: blue 
			0.0f, 0.0f, 1.0f,
			0.0f, 0.0f, 1.0f
		};

		float[] cubePositions = {
			// Front face
			-1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f,  1.0f,  
			 1.0f,  1.0f,  1.0f,  
			 1.0f,  1.0f,  1.0f,  
			-1.0f,  1.0f,  1.0f,  
			-1.0f, -1.0f,  1.0f,  
		
			// Back face 
			-1.0f, -1.0f, -1.0f, 
			-1.0f,  1.0f, -1.0f,  
			 1.0f,  1.0f, -1.0f,  
			 1.0f,  1.0f, -1.0f,  
			 1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f, -1.0f,  
		
			// Left face 
			-1.0f,  1.0f,  1.0f,  
			-1.0f,  1.0f, -1.0f, 
			-1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f, -1.0f, 
			-1.0f, -1.0f,  1.0f, 
			-1.0f,  1.0f,  1.0f, 
		
			// Right face 
			 1.0f,  1.0f,  1.0f,  
			 1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f, -1.0f, 
			 1.0f, -1.0f, -1.0f,  
			 1.0f,  1.0f, -1.0f,  
			 1.0f,  1.0f,  1.0f,  
		
			// Top face 
			-1.0f,  1.0f, -1.0f,  
			 1.0f,  1.0f, -1.0f,  
			 1.0f,  1.0f,  1.0f, 
			 1.0f,  1.0f,  1.0f,  
			-1.0f,  1.0f,  1.0f,  
			-1.0f,  1.0f, -1.0f,  
		
			// Bottom face 
			-1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f,  1.0f,  
			 1.0f, -1.0f, -1.0f,  
			-1.0f, -1.0f, -1.0f   
		};

		float[] cubeTexCoords = {
			// Front face
			0.0f, 0.0f,   
			2.0f, 0.0f,  
			2.0f, 2.0f,   
			2.0f, 2.0f,  
			0.0f, 2.0f,   
			0.0f, 0.0f,  
		
			// Back face
			0.0f, 0.0f,   
			2.0f, 0.0f,   
			2.0f, 2.0f,   
			2.0f, 2.0f, 
			0.0f, 2.0f,   
			0.0f, 0.0f,   
		
			// Left face
			0.0f, 0.0f,   
			2.0f, 0.0f,   
			2.0f, 2.0f,  
			2.0f, 2.0f,   
			0.0f, 2.0f,   
			0.0f, 0.0f,  
		
			// Right face
			0.0f, 0.0f,   
			2.0f, 0.0f,   
			2.0f, 2.0f,   
			2.0f, 2.0f, 
			0.0f, 2.0f,   
			0.0f, 0.0f, 

			// Top face
			0.0f, 0.0f,  
			2.0f, 0.0f,   
			2.0f, 2.0f,   
			2.0f, 2.0f,  
			0.0f, 2.0f,   
			0.0f, 0.0f,   

			// Bottom face
			0.0f, 0.0f,  
			2.0f, 0.0f, 
			2.0f, 2.0f,   
			2.0f, 2.0f,   
			0.0f, 2.0f,   
			0.0f, 0.0f  
		};
		
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
		
		// Shuttle positions
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer shuttleVertBuf = Buffers.newDirectFloatBuffer(shuttlePvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleVertBuf.limit()*4, shuttleVertBuf, GL_STATIC_DRAW);

		// Shuttle texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer shuttleTexBuf = Buffers.newDirectFloatBuffer(shuttleTvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleTexBuf.limit()*4, shuttleTexBuf, GL_STATIC_DRAW);

		// Shuttle normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer shuttleNorBuf = Buffers.newDirectFloatBuffer(shuttleNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, shuttleNorBuf.limit()*4, shuttleNorBuf, GL_STATIC_DRAW);

		//crown positions
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer crownBuf = Buffers.newDirectFloatBuffer(crownPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, crownBuf.limit()*4, crownBuf, GL_STATIC_DRAW);

		// Crown tex coords 
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[4]);
		FloatBuffer crownTexBuf = Buffers.newDirectFloatBuffer(crownTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, crownTexBuf.limit() * 4, crownTexBuf, GL_STATIC_DRAW);

		// axis positions 
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[5]);
		FloatBuffer axisPosBuf = Buffers.newDirectFloatBuffer(axisPositions);
		gl.glBufferData(GL_ARRAY_BUFFER, axisPosBuf.limit() * 4, axisPosBuf, GL_STATIC_DRAW);

		// axis colors 
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[6]);
		FloatBuffer axisColorBuf = Buffers.newDirectFloatBuffer(axisColors);
		gl.glBufferData(GL_ARRAY_BUFFER, axisColorBuf.limit() * 4, axisColorBuf, GL_STATIC_DRAW);

		//sphere positions
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[7]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		//sphere tex coords
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[8]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

		//sphere normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[9]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);

		//cube postitions
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[10]);
		FloatBuffer cubeBuf = Buffers.newDirectFloatBuffer(cubePositions);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeBuf.limit()*4, cubeBuf, GL_STATIC_DRAW);

		// cube tex coords 
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[11]);
		FloatBuffer cubeTexBuf = Buffers.newDirectFloatBuffer(cubeTexCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, cubeTexBuf.limit() * 4, cubeTexBuf, GL_STATIC_DRAW);
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
			// Move left/right 
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
			//turn on off the axis lines
			case KeyEvent.VK_SPACE:
				showAxes = !showAxes; 
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

		cameraFront.normalize();  
		cameraRight.set(cameraFront).cross(cameraUp).normalize();
	}

	@Override
	public void keyReleased(KeyEvent e) {}
	@Override
	public void keyTyped(KeyEvent e) {}

	public static void main(String[] args) { new Code(); }
	public void dispose(GLAutoDrawable drawable) {}
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{	float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);
	}
}