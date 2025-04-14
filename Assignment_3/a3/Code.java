package a3;

import java.io.*;
import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import org.joml.*;
import com.jogamp.opengl.util.FPSAnimator;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram;
	private int vao[] = new int[1];
	private int vbo[] = new int[4];

	private float cameraX, cameraY, cameraZ;
	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f); // Initial direction
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);     // Fixed up vector
	private Vector3f cameraRight = new Vector3f();   
	private float yaw = -90.0f;   
	private float pitch = -15.0f; 
	private float rotationSpeed = 3.0f; 
	private float cameraSpeed = 0.3f;


	private ImportedModel campfireModel;
	private int numCampfireVertices;
	private int campfireTexture;
	private Vector3f campfireLoc = new Vector3f(0, 0, 0);

	private ImportedModel pandaModel;
	private int numPandaVertices;
	private int pandaTexture;
	private int pandaVAO;
	private int[] pandaVBO;
	private Vector3f pandaLoc = new Vector3f(.5f, 0.0f, -.5f);

	private int floorVAO;
	private int[] floorVBO;
	private int floorTexture;

	private int skyboxVAO;
    private int[] skyboxVBO;
    private int skyboxTexture;

	private int axisVAO;
    private int axisVBO;

	private ImportedModel flyModel;
    private int numFlyVertices;
    private int flyTexture;
    private int flyVAO;
    private int[] flyVBO;
    private Vector3f flyLoc = new Vector3f(1.0f, 0.0f, -1.0f); 

	private Vector3f initialLightLoc = new Vector3f(5.0f, 2.0f, 2.0f);
	private float amt = 0.0f;
	private double prevTime;
	private double elapsedTime;
	private long lastTime = System.currentTimeMillis();
	private boolean showAxes = true;
	private boolean lightEnabled = true;
	private boolean lightMove;

	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];


	// white light properties
	float[] globalAmbient = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
	float[] lightAmbient = new float[] { 0.1f, 0.1f, 0.1f, 1.0f };
	float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	float[] matAmb = Utils.bronzeAmbient();
	float[] matDif = Utils.goldDiffuse();
	float[] matSpe = Utils.goldSpecular();
	float matShi = Utils.goldShininess();

	//a fire like ambience
	float[] fireAmbient = new float[] { 1.0f, 0.4f, 0.1f, 1.0f };

	// glossy wood material
	float[] matAmbwood = new float[] { 0.4f, 0.3f, 0.2f, 1.0f };
	float[] matDifwood = new float[] { 0.6f, 0.4f, 0.25f, 1.0f };
	float[] matSpewood = new float[] { 0.3f, 0.3f, 0.3f, 1.0f };
	float matShiwood = 25.0f;

	public Code()
	{	setTitle("Assignment 3");
		setSize(800, 800);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		this.add(myCanvas);
		myCanvas.addKeyListener(this); 
		myCanvas.setFocusable(true);
		myCanvas.requestFocus();

		FPSAnimator animator = new FPSAnimator(myCanvas, 60);
		animator.start();

		setVisible(true);
		setFocusable(true);
		this.setVisible(true);
	}

	public void display(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
       	gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        // Update view matrix based on camera position and direction
        vMat.identity();
        vMat.lookAt(
            new Vector3f(cameraX, cameraY, cameraZ),     
            new Vector3f(cameraX, cameraY, cameraZ).add(cameraFront), 
            cameraUp
        );
        
        // --- Draw Skybox ---
        gl.glDepthMask(false); 
        gl.glDisable(GL_CULL_FACE); // draw both sides of the cube
        
        gl.glUseProgram(renderingProgram);
		mLoc = gl.glGetUniformLocation(renderingProgram, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram, "norm_matrix");
		int u_skyboxLoc = gl.glGetUniformLocation(renderingProgram, "u_skybox");

        // skybox model is centered on the camera
        Matrix4f skyboxModel = new Matrix4f().identity().translate(cameraX, cameraY, cameraZ);
        gl.glUniformMatrix4fv(mLoc, 1, false, skyboxModel.get(vals));
        gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
        gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
        
        gl.glBindVertexArray(skyboxVAO);
		gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);

        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, skyboxTexture);
		int samplerLoc = gl.glGetUniformLocation(renderingProgram, "texture_sampler");

		gl.glUniform1i(u_skyboxLoc, 1);
        gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glUniform1i(u_skyboxLoc, 0);
        gl.glDepthMask(true); 
        gl.glEnable(GL_CULL_FACE);

       // --- Draw Campfire ---
		Matrix4f campfireMat = new Matrix4f().identity();
		campfireMat.translate(campfireLoc.x(), campfireLoc.y(), campfireLoc.z());
		campfireMat.scale(0.08f); // scale campfire
		currentLightPos.set(campfireLoc);
		installLights();

		// Calculate inverse-transpose for correct normal transformation
		Matrix4f invTrCampfire = new Matrix4f();
		campfireMat.invert(invTrCampfire);
		invTrCampfire.transpose(invTrCampfire);

		// Send uniforms for the campfire
		gl.glUniformMatrix4fv(mLoc, 1, false, campfireMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrCampfire.get(vals));

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, campfireTexture);
		samplerLoc = gl.glGetUniformLocation(renderingProgram, "texture_sampler");
		gl.glUniform1i(samplerLoc, 0);

		gl.glDisable(GL_CULL_FACE);
		gl.glDrawArrays(GL_TRIANGLES, 0, numCampfireVertices);
		gl.glEnable(GL_CULL_FACE);

        
        // --- Draw Panda ---
        Matrix4f pandaMat = new Matrix4f().identity();
        pandaMat.translate(pandaLoc.x(), pandaLoc.y(), pandaLoc.z());
        pandaMat.scale(0.75f);
        pandaMat.rotateY((float)Math.toRadians(135.0f));
        Matrix4f invTrPanda = new Matrix4f();
        pandaMat.invert(invTrPanda);
        invTrPanda.transpose(invTrPanda);
        gl.glUniformMatrix4fv(mLoc, 1, false, pandaMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrPanda.get(vals));
        
        gl.glBindVertexArray(pandaVAO);
        gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[2]);
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);
        
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, pandaTexture);
        samplerLoc = gl.glGetUniformLocation(renderingProgram, "texture_sampler");
        gl.glUniform1i(samplerLoc, 0);
        gl.glDrawArrays(GL_TRIANGLES, 0, numPandaVertices);

		// --- Draw Fly ---
		Matrix4f flyMat = new Matrix4f(campfireMat);  // copy parent's transformation

		// Compute delta-time to update the orbit angle
		long currentTime = System.currentTimeMillis();
		float dt = (currentTime - lastTime) / 1000.0f;
		float orbitSpeed = 270.0f; // degrees per second
		amt += orbitSpeed * dt;
		amt %= 360;
		lastTime = currentTime;

		// Create a rotation about the Y axis for orbiting and a translation to set the orbit distance
		Matrix4f orbitRot = new Matrix4f().rotationY((float)Math.toRadians(amt));
		Matrix4f orbitTrans = new Matrix4f().translate(3f, 3.5f, 0.0f);

		flyMat.mul(orbitRot).mul(orbitTrans);
		flyMat.scale(0.2f);
		flyMat.rotateY(90f);

		Matrix4f invTrFly = new Matrix4f();
		flyMat.invert(invTrFly);
		invTrFly.transpose(invTrFly);

		gl.glUniformMatrix4fv(mLoc, 1, false, flyMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrFly.get(vals));

		gl.glBindVertexArray(flyVAO);
		gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, flyTexture);
		samplerLoc = gl.glGetUniformLocation(renderingProgram, "texture_sampler");
		gl.glUniform1i(samplerLoc, 0);
		gl.glDrawArrays(GL_TRIANGLES, 0, numFlyVertices);
        
        // --- Draw Floor ---
        Matrix4f floorMat = new Matrix4f().identity();
        floorMat.translate(0.0f, -0.05f, 0.0f);
		floorMat.scale(10f);

        Matrix4f invTrFloor = new Matrix4f();
        floorMat.invert(invTrFloor);
        invTrFloor.transpose(invTrFloor);
        gl.glUniformMatrix4fv(mLoc, 1, false, floorMat.get(vals));
        gl.glUniformMatrix4fv(nLoc, 1, false, invTrFloor.get(vals));
        
        gl.glBindVertexArray(floorVAO);
        gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[0]);
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(0);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[1]);
        gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(1);
        
        gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[2]);
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        gl.glEnableVertexAttribArray(2);
        
        gl.glActiveTexture(GL_TEXTURE0);
        gl.glBindTexture(GL_TEXTURE_2D, floorTexture);
        samplerLoc = gl.glGetUniformLocation(renderingProgram, "texture_sampler");
        gl.glUniform1i(samplerLoc, 0);
        gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		// --- Draw XYZ Axis Lines ---
		if (showAxes){
			int colorFlagLoc = gl.glGetUniformLocation(renderingProgram, "u_colorFlag");
			int colorLoc = gl.glGetUniformLocation(renderingProgram, "u_color");
			gl.glUniform1i(colorFlagLoc, 1);
			
			Matrix4f axisMat = new Matrix4f().identity();
			axisMat.scale(5f);
			gl.glUniformMatrix4fv(mLoc, 1, false, axisMat.get(vals));
			gl.glBindVertexArray(axisVAO);
			// X-axis in Red
			gl.glUniform4f(colorLoc, 1.0f, 0.0f, 0.0f, 1.0f);
			gl.glDrawArrays(GL_LINES, 0, 2);
			// Y-axis in Green
			gl.glUniform4f(colorLoc, 0.0f, 1.0f, 0.0f, 1.0f);
			gl.glDrawArrays(GL_LINES, 2, 2);
			// Z-axis in Blue
			gl.glUniform4f(colorLoc, 0.0f, 0.0f, 1.0f, 1.0f);
			gl.glDrawArrays(GL_LINES, 4, 2);
			
			gl.glUniform1i(colorFlagLoc, 0);
		}
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		campfireModel = new ImportedModel("Campfire.obj");
		numCampfireVertices = campfireModel.getNumVertices();

		pandaModel = new ImportedModel("panda.obj");
		numPandaVertices = pandaModel.getNumVertices();

		flyModel = new ImportedModel("fly.obj");
        numFlyVertices = flyModel.getNumVertices();

		pandaTexture = Utils.loadTexture("pandatx.jpg");
		campfireTexture = Utils.loadTexture("campfire.png");
		floorTexture = Utils.loadTexture("ground.jpg");
		skyboxTexture = Utils.loadTexture("Night Sky.png");
		flyTexture = Utils.loadTexture("fly.jpg");

		gl.glBindTexture(GL_TEXTURE_2D, floorTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

		renderingProgram = Utils.createShaderProgram("a3/vertShader.glsl", "a3/fragShader.glsl");

		prevTime = System.currentTimeMillis();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(90.0f), aspect, 0.1f, 1000.0f);

		setupSkyboxVertices();
		System.out.println("skyboxVAO = " + skyboxVAO);
		setupVertices();
		setupPandaVertices();
		setupFloorVertices();
		setupFlyVertices();
		setupAxisLines();

		cameraX = 0.0f; cameraY = 2.0f; cameraZ = 4.0f;
		updateCameraVectors();
	}
	
	private void installLights()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		
		lightPos[0]=currentLightPos.x()+.5f;
		lightPos[1]=currentLightPos.y()+2f;
		lightPos[2]=currentLightPos.z()+.5f;
		
		// get the locations of the light and material fields in the shader
		globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
		ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
		diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
		specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
		posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
		mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
		mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
		mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
		mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");
	
		//  set the uniform light and material values in the shader
		gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);

		if (lightEnabled) {
			gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
			gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
			gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		} else {
			float[] zeroLight = new float[] { 0f, 0f, 0f, 0f };
			gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, zeroLight, 0);
			gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, zeroLight, 0);
			gl.glProgramUniform4fv(renderingProgram, specLoc, 1, zeroLight, 0);
		}

		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	//ended up just being the campfire
	private void setupVertices()
	{	    
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int numVertices = campfireModel.getNumVertices();
		Vector3f[] vertices = campfireModel.getVertices();
		Vector2f[] texCoords = campfireModel.getTexCoords();
		Vector3f[] normals = campfireModel.getNormals();
	
		float[] pvalues = new float[numVertices * 3];
		float[] tvalues = new float[numVertices * 2];
		float[] nvalues = new float[numVertices * 3];
	
		for (int i = 0; i < numVertices; i++) {
			pvalues[i * 3]     = vertices[i].x();
			pvalues[i * 3 + 1] = vertices[i].y();
			pvalues[i * 3 + 2] = vertices[i].z();
	
			tvalues[i * 2]     = texCoords[i].x();
			tvalues[i * 2 + 1] = texCoords[i].y();
	
			nvalues[i * 3]     = normals[i].x();
			nvalues[i * 3 + 1] = normals[i].y();
			nvalues[i * 3 + 2] = normals[i].z();
		}
	
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);
	
		// Positions buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
	
		// Texture coordinates buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
	
		// Normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);
	}

	private void setupPandaVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int numVertices = pandaModel.getNumVertices();
		Vector3f[] vertices = pandaModel.getVertices();
		Vector2f[] texCoords = pandaModel.getTexCoords();
		Vector3f[] normals = pandaModel.getNormals();
	
		float[] pvalues = new float[numVertices * 3];
		float[] tvalues = new float[numVertices * 2];
		float[] nvalues = new float[numVertices * 3];
	
		for (int i = 0; i < numVertices; i++) {
			pvalues[i * 3]     = vertices[i].x();
			pvalues[i * 3 + 1] = vertices[i].y();
			pvalues[i * 3 + 2] = vertices[i].z();
	
			tvalues[i * 2]     = texCoords[i].x();
			tvalues[i * 2 + 1] = texCoords[i].y();
	
			nvalues[i * 3]     = normals[i].x();
			nvalues[i * 3 + 1] = normals[i].y();
			nvalues[i * 3 + 2] = normals[i].z();
		}
	
		int[] vaoId = new int[1];
		gl.glGenVertexArrays(1, vaoId, 0);
		pandaVAO = vaoId[0];
		gl.glBindVertexArray(pandaVAO);
	
		pandaVBO = new int[3];
		gl.glGenBuffers(3, pandaVBO, 0);
	
		// Positions buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
	
		// Texture coordinates buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[1]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
	
		// Normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[2]);
		FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);
	
		gl.glBindVertexArray(0);
	}

	private void setupSkyboxVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
		float[] cubeVertexPositions =
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
        // Texture coordinates for skybox
		float[] cubeTextureCoord =
		{	1.00f, 0.6666666f, 1.00f, 0.3333333f, 0.75f, 0.3333333f,	// back face lower right
			0.75f, 0.3333333f, 0.75f, 0.6666666f, 1.00f, 0.6666666f,	// back face upper left
			0.75f, 0.3333333f, 0.50f, 0.3333333f, 0.75f, 0.6666666f,	// right face lower right
			0.50f, 0.3333333f, 0.50f, 0.6666666f, 0.75f, 0.6666666f,	// right face upper left
			0.50f, 0.3333333f, 0.25f, 0.3333333f, 0.50f, 0.6666666f,	// front face lower right
			0.25f, 0.3333333f, 0.25f, 0.6666666f, 0.50f, 0.6666666f,	// front face upper left
			0.25f, 0.3333333f, 0.00f, 0.3333333f, 0.25f, 0.6666666f,	// left face lower right
			0.00f, 0.3333333f, 0.00f, 0.6666666f, 0.25f, 0.6666666f,	// left face upper left
			0.25f, 0.3333333f, 0.50f, 0.3333333f, 0.50f, 0.0000000f,	// bottom face upper right
			0.50f, 0.0000000f, 0.25f, 0.0000000f, 0.25f, 0.3333333f,	// bottom face lower left
			0.25f, 1.0000000f, 0.50f, 1.0000000f, 0.50f, 0.6666666f,	// top face upper right
			0.50f, 0.6666666f, 0.25f, 0.6666666f, 0.25f, 1.0000000f		// top face lower left
		};
        
        int[] temp = new int[1];
        gl.glGenVertexArrays(1, temp, 0);
        skyboxVAO = temp[0];
        gl.glBindVertexArray(skyboxVAO);
        
        skyboxVBO = new int[2];
        gl.glGenBuffers(2, skyboxVBO, 0);
        
        // Buffer for vertex positions
        gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[0]);
        FloatBuffer skyVertBuf = Buffers.newDirectFloatBuffer(cubeVertexPositions);
        gl.glBufferData(GL_ARRAY_BUFFER, skyVertBuf.limit() * 4, skyVertBuf, GL_STATIC_DRAW);
        
        // Buffer for texture coordinates
        gl.glBindBuffer(GL_ARRAY_BUFFER, skyboxVBO[1]);
        FloatBuffer skyTexBuf = Buffers.newDirectFloatBuffer(cubeTextureCoord);
        gl.glBufferData(GL_ARRAY_BUFFER, skyTexBuf.limit() * 4, skyTexBuf, GL_STATIC_DRAW);
        
        gl.glBindVertexArray(0);
    }

	private void setupFlyVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        int numVertices = flyModel.getNumVertices();
        Vector3f[] vertices = flyModel.getVertices();
        Vector2f[] texCoords = flyModel.getTexCoords();
        Vector3f[] normals = flyModel.getNormals();

        float[] pvalues = new float[numVertices * 3];
        float[] tvalues = new float[numVertices * 2];
        float[] nvalues = new float[numVertices * 3];

        for (int i = 0; i < numVertices; i++) {
            pvalues[i * 3] = vertices[i].x();
            pvalues[i * 3 + 1] = vertices[i].y();
            pvalues[i * 3 + 2] = vertices[i].z();

            tvalues[i * 2] = texCoords[i].x();
            tvalues[i * 2 + 1] = texCoords[i].y();

            nvalues[i * 3] = normals[i].x();
            nvalues[i * 3 + 1] = normals[i].y();
            nvalues[i * 3 + 2] = normals[i].z();
        }

        int[] vaoId = new int[1];
        gl.glGenVertexArrays(1, vaoId, 0);
        flyVAO = vaoId[0];
        gl.glBindVertexArray(flyVAO);

        flyVBO = new int[3];
        gl.glGenBuffers(3, flyVBO, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit() * 4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit() * 4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, flyVBO[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit() * 4, norBuf, GL_STATIC_DRAW);

        gl.glBindVertexArray(0);
	}

	private void setupFloorVertices() {
		GL4 gl = (GL4) GLContext.getCurrentGL();
	
		float[] vertices = {
			// First triangle
			-5.0f, 0.0f, -5.0f,
			-5.0f, 0.0f,  5.0f,
			 5.0f, 0.0f,  5.0f,
			// Second triangle
			 5.0f, 0.0f,  5.0f,
			 5.0f, 0.0f, -5.0f,
			-5.0f, 0.0f, -5.0f
		};
	
		float[] texCoords = {
			// For the first triangle
			0.0f, 0.0f,
			0.0f, 10.0f,
			10.0f, 10.0f,
			// For the second triangle
			10.0f, 10.0f,
			10.0f, 0.0f,
			0.0f, 0.0f
		};
	
		//all normals pointing up 
		float[] normals = {
			0.0f, 1.0f, 0.0f,  
			0.0f, 1.0f, 0.0f,  
			0.0f, 1.0f, 0.0f,  
			0.0f, 1.0f, 0.0f,  
			0.0f, 1.0f, 0.0f,  
			0.0f, 1.0f, 0.0f
		};
	
		int[] vaoId = new int[1];
		gl.glGenVertexArrays(1, vaoId, 0);
		floorVAO = vaoId[0];
		gl.glBindVertexArray(floorVAO);
	
		floorVBO = new int[3];
		gl.glGenBuffers(3, floorVBO, 0);
	
		// Positions
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[0]);
		FloatBuffer vertBuffer = Buffers.newDirectFloatBuffer(vertices);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuffer.limit() * 4, vertBuffer, GL_STATIC_DRAW);
	
		// Texture coordinates
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[1]);
		FloatBuffer texBuffer = Buffers.newDirectFloatBuffer(texCoords);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuffer.limit() * 4, texBuffer, GL_STATIC_DRAW);
	
		// Normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[2]);
		FloatBuffer normBuffer = Buffers.newDirectFloatBuffer(normals);
		gl.glBufferData(GL_ARRAY_BUFFER, normBuffer.limit() * 4, normBuffer, GL_STATIC_DRAW);
	
		gl.glBindVertexArray(0);
	}

	private void setupAxisLines() {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        float[] axisVertices = {
             0.0f, 0.0f, 0.0f,   1.0f, 0.0f, 0.0f, // X-axis
             0.0f, 0.0f, 0.0f,   0.0f, 1.0f, 0.0f, // Y-axis
             0.0f, 0.0f, 0.0f,   0.0f, 0.0f, 1.0f  // Z-axis
        };

        int[] temp = new int[1];
        gl.glGenVertexArrays(1, temp, 0);
        axisVAO = temp[0];
        gl.glBindVertexArray(axisVAO);

        int[] buffers = new int[1];
        gl.glGenBuffers(1, buffers, 0);
        axisVBO = buffers[0];
        gl.glBindBuffer(GL_ARRAY_BUFFER, axisVBO);
        FloatBuffer axisBuf = Buffers.newDirectFloatBuffer(axisVertices);
        gl.glBufferData(GL_ARRAY_BUFFER, axisBuf.limit() * 4, axisBuf, GL_STATIC_DRAW);

        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * 4, 0);
        gl.glEnableVertexAttribArray(0);

        gl.glBindVertexArray(0);
	}

	
	@Override
	public void keyPressed(KeyEvent e) {
		updateCameraVectors(); 

		switch (e.getKeyCode()) {
			// Move forward/backward
			case KeyEvent.VK_W:
			if(lightMove){
				campfireLoc.z -= cameraSpeed;
			}
			else{
				cameraX += cameraFront.x * cameraSpeed;
				cameraY += cameraFront.y * cameraSpeed;
				cameraZ += cameraFront.z * cameraSpeed;
			}
				break;
			case KeyEvent.VK_S:
			if(lightMove){
				campfireLoc.z += cameraSpeed;
			}
			else{
				cameraX -= cameraFront.x * cameraSpeed;
				cameraY -= cameraFront.y * cameraSpeed;
				cameraZ -= cameraFront.z * cameraSpeed;
			}
				break;
			// Move left/right 
			case KeyEvent.VK_A:
			if(lightMove){
				campfireLoc.x -= cameraSpeed;
			}
			else{
				cameraX -= cameraRight.x * cameraSpeed;
				cameraY -= cameraRight.y * cameraSpeed;
				cameraZ -= cameraRight.z * cameraSpeed;
			}
				break;
			case KeyEvent.VK_D:
			if(lightMove){
				campfireLoc.x += cameraSpeed;
			}
			else{
				cameraX += cameraRight.x * cameraSpeed;
				cameraY += cameraRight.y * cameraSpeed;
				cameraZ += cameraRight.z * cameraSpeed;
			}
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
			case KeyEvent.VK_L:
				showAxes = !showAxes; 
				break;
			//turn on light moving mode
			case KeyEvent.VK_SPACE:
				lightMove = !lightMove;
				break; 
			case KeyEvent.VK_1:
				lightEnabled = !lightEnabled;
				break;
		}
		// Limit pitch and cameraY to prevent flipping and going under floor
		pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
		cameraY = Math.max(.25f, cameraY);

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
	{	aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.setPerspective((float) Math.toRadians(90.0f), aspect, 0.1f, 1000.0f);
	}
}