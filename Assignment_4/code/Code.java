package code;

import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GLContext;
import org.joml.*;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

public class Code extends JFrame implements GLEventListener, KeyListener
{	private GLCanvas myCanvas;
	private int renderingProgram1, renderingProgram2;
	private int vao[] = new int[1];
	private int vbo[] = new int[5];

	// model stuff
	private ImportedModel pyramid;
	private Torus myTorus;
	private int numPyramidVertices, numTorusVertices, numTorusIndices;
	
	// location of torus, pyramid, light, and camera
	private Vector3f torusLoc = new Vector3f(1.6f, 0.0f, -0.3f);
	private Vector3f pyrLoc = new Vector3f(-1.0f, 0.1f, 0.3f);
	private Vector3f lightLoc = new Vector3f(-3.8f, 2.2f, 1.1f);

	//camera stuff
	private float cameraX, cameraY, cameraZ;
	private Vector3f cameraFront = new Vector3f(0.0f, 0.0f, -1.0f); // Initial direction
	private Vector3f cameraUp = new Vector3f(0.0f, 1.0f, 0.0f);     // Fixed up vector
	private Vector3f cameraRight = new Vector3f();   
	private float yaw = -90.0f;   
	private float pitch = -15.0f; 
	private float rotationSpeed = 3.0f; 
	private float cameraSpeed = 0.3f;

	//booleans
	private boolean showAxes = true;
	private boolean lightEnabled = true;
	private boolean lightMove = false;
	
	// white light properties
	private float[] globalAmbient = new float[] { 0.7f, 0.7f, 0.7f, 1.0f };
	private float[] lightAmbient = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };
	private float[] lightDiffuse = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
	private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
		
	// gold material
	private float[] GmatAmb = Utils.goldAmbient();
	private float[] GmatDif = Utils.goldDiffuse();
	private float[] GmatSpe = Utils.goldSpecular();
	private float GmatShi = Utils.goldShininess();
	
	// bronze material
	private float[] BmatAmb = Utils.bronzeAmbient();
	private float[] BmatDif = Utils.bronzeDiffuse();
	private float[] BmatSpe = Utils.bronzeSpecular();
	private float BmatShi = Utils.bronzeShininess();
	
	private float[] thisAmb, thisDif, thisSpe, matAmb, matDif, matSpe;
	private float thisShi, matShi;
	
	// shadow stuff
	private int scSizeX, scSizeY;
	private int [] shadowTex = new int[1];
	private int [] shadowBuffer = new int[1];
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();

	// allocate variables for display() function
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f pMat = new Matrix4f();  // perspective matrix
	private Matrix4f vMat = new Matrix4f();  // view matrix
	private Matrix4f mMat = new Matrix4f();  // model matrix
	private Matrix4f mvMat = new Matrix4f(); // model-view matrix
	private Matrix4f invTrMat = new Matrix4f(); // inverse-transpose
	private int mLoc, vLoc, pLoc, nLoc, sLoc;
	private int globalAmbLoc, ambLoc, diffLoc, specLoc, posLoc, mambLoc, mdiffLoc, mspecLoc, mshiLoc;
	private float aspect;
	private Vector3f currentLightPos = new Vector3f();
	private float[] lightPos = new float[3];
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

	// models + textures
	private ImportedModel campfireModel;
	private int numCampfireVertices, campfireTexture, campfireVAO;
	private int[] campfireVBO;
	private Vector3f campfireLoc = new Vector3f(-2f,0,2f);

	private ImportedModel pandaModel;
	private int    numPandaVertices, pandaTexture, pandaVAO;
	private int[]  pandaVBO;
	private Vector3f pandaLoc = new Vector3f(0f,0f,0f);

	private ImportedModel flyModel;
	private int    numFlyVertices, flyTexture, flyVAO;
	private int[]  flyVBO;
	private Vector3f flyLoc = new Vector3f(1,0,-1);

	private int floorVAO;
	private int[] floorVBO;
	private int floorTexture;
	private Vector3f floorLoc = new Vector3f(0,-.05f,0);

	private int skyboxVAO;
	private int[] skyboxVBO;
	private int skyboxTexture;

	private int axisVAO;
    private int axisVBO;
	
	public Code()
	{	setTitle("Chapter8 - program 1");
		setSize(800, 800);
		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);
		myCanvas.addKeyListener(this);
		this.add(myCanvas);
		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}

	public void display(GLAutoDrawable drawable)
	{	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		
        // Update view matrix based on camera position and direction
        vMat.identity();
        vMat.lookAt(
            new Vector3f(cameraX, cameraY, cameraZ),     
            new Vector3f(cameraX, cameraY, cameraZ).add(cameraFront),cameraUp);
		
		lightVmat.identity().setLookAt(currentLightPos, origin, up);	// vector from light to origin
		lightPmat.identity().setPerspective((float) Math.toRadians(90.0f), aspect, 0.1f, 100.0f);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadowBuffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowTex[0], 0);
	
		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);	//  for reducing
		gl.glPolygonOffset(3.0f, 5.0f);		//  shadow artifacts

		passOne();
		
		gl.glDisable(GL_POLYGON_OFFSET_FILL);	// artifact reduction, continued
		
		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
	
		gl.glDrawBuffer(GL_FRONT);
		
		passTwo();
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passOne()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(renderingProgram1);
		
		// draw the panda 
		mMat.identity();
		mMat.translate(pandaLoc.x(), pandaLoc.y(), pandaLoc.z());
		mMat.rotateY((float)Math.toRadians(135.0f));

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat).mul(lightVmat).mul(mMat);
		sLoc = gl.glGetUniformLocation(renderingProgram1, "shadowMVP");
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		gl.glBindVertexArray(pandaVAO);
		// positions → attrib 0
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		// normals → attrib 1
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, numPandaVertices);
		gl.glBindVertexArray(0);

		// draw the campfire
		
		mMat.identity();
		mMat.translate(campfireLoc.x(), campfireLoc.y(), campfireLoc.z());
		mMat.scale(0.1f, 0.1f, 0.1f);
		//mMat.rotateX((float)Math.toRadians(30.0f));
		//mMat.rotateY((float)Math.toRadians(40.0f));

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		

		gl.glBindVertexArray(campfireVAO);
		// positions → attrib 0
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		// normals → attrib 1
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, numCampfireVertices);

		// ------ draw the floor -------
		
		mMat.identity();
		mMat.translate(floorLoc.x(), floorLoc.y(), floorLoc.z());
		mMat.scale(10f, 10f, 10f);
		//mMat.rotateX((float)Math.toRadians(30.0f));
		//mMat.rotateY((float)Math.toRadians(40.0f));

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));
		

		gl.glBindVertexArray(floorVAO);
		// positions → attrib 0
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		// normals → attrib 1
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
	
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
	}
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
	public void passTwo()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		gl.glUseProgram(renderingProgram2);


		// after glUseProgram(renderingProgram2):
		int shadowLoc = gl.glGetUniformLocation(renderingProgram2, "shadowTex");
		gl.glUniform1i(shadowLoc, 1);
		// bind the shadow map into unit 1:
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);


		
		mLoc = gl.glGetUniformLocation(renderingProgram2, "m_matrix");
		vLoc = gl.glGetUniformLocation(renderingProgram2, "v_matrix");
		pLoc = gl.glGetUniformLocation(renderingProgram2, "p_matrix");
		nLoc = gl.glGetUniformLocation(renderingProgram2, "norm_matrix");
		sLoc = gl.glGetUniformLocation(renderingProgram2, "shadowMVP");

		// draw the Panda
			
		thisAmb = BmatAmb; //bronze
		thisDif = BmatDif;
		thisSpe = BmatSpe;
		thisShi = BmatShi;
		
		//vMat.identity().setTranslation(-cameraX, -cameraY, -cameraZ);
		currentLightPos.set(campfireLoc.x, campfireLoc.y + 3f,campfireLoc.z);
		installLights(renderingProgram2);

		mMat.identity();
	    mMat.translate(pandaLoc.x(), pandaLoc.y(), pandaLoc.z());
		//mMat.rotateY((float)Math.toRadians(25.0f));
		mMat.rotateY((float)Math.toRadians(135.0f));
		
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);
		
		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));


		int uTexLoc = gl.glGetUniformLocation(renderingProgram2, "u_texture");
		gl.glUniform1i(uTexLoc, 1);    


	    gl.glBindVertexArray(pandaVAO);
	    // positions
	    gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[0]);
	    gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(0);

	    // normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// text coords
		gl.glBindBuffer(GL_ARRAY_BUFFER, pandaVBO[1]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// bind the panda texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, pandaTexture);
		int texLoc = gl.glGetUniformLocation(renderingProgram2, "textureMap");
		gl.glUniform1i(texLoc, 0);
		 
		gl.glDrawArrays(GL_TRIANGLES, 0, numPandaVertices);
		gl.glBindVertexArray(0);

		// draw the campfire  
		
		thisAmb = GmatAmb; // the pyramid is gold
		thisDif = GmatDif;
		thisSpe = GmatSpe;
		thisShi = GmatShi;
		
		mMat.identity();
		mMat.translate(campfireLoc.x(), campfireLoc.y(), campfireLoc.z());
		mMat.scale(0.1f, 0.1f, 0.1f);
		//mMat.rotateX((float)Math.toRadians(30.0f));
		//mMat.rotateY((float)Math.toRadians(40.0f));
		
		currentLightPos.set(campfireLoc.x, campfireLoc.y + 3f,campfireLoc.z);
		installLights(renderingProgram2);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		
		gl.glBindVertexArray(campfireVAO);
	    // positions
	    gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[0]);
	    gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(0);

	    // normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// text coords
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[1]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// bind the panda texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, campfireTexture);
		texLoc = gl.glGetUniformLocation(renderingProgram2, "textureMap");
		gl.glUniform1i(texLoc, 0);
		 
		gl.glDisable(GL_CULL_FACE);
		gl.glDrawArrays(GL_TRIANGLES, 0, numCampfireVertices);
		gl.glBindVertexArray(0);
		gl.glEnable(GL_CULL_FACE);

		// --------- draw the Floor ---------  
		
		thisAmb = GmatAmb; //  is gold
		thisDif = GmatDif;
		thisSpe = GmatSpe;
		thisShi = GmatShi;
		
		mMat.identity();
		mMat.translate(floorLoc.x(), floorLoc.y(), floorLoc.z());
		mMat.scale(10f, 10f, 10f);
		//mMat.rotateX((float)Math.toRadians(30.0f));
		//mMat.rotateY((float)Math.toRadians(40.0f));
		
		currentLightPos.set(campfireLoc.x, campfireLoc.y + 3f,campfireLoc.z);
		installLights(renderingProgram2);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);
		
		mMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		
		gl.glBindVertexArray(floorVAO);
	    // positions
	    gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[0]);
	    gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
	    gl.glEnableVertexAttribArray(0);

	    // normals
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[2]);
		gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		// text coords
		gl.glBindBuffer(GL_ARRAY_BUFFER, floorVBO[1]);
		gl.glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		// bind the texture
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, floorTexture);
		texLoc = gl.glGetUniformLocation(renderingProgram2, "textureMap");
		gl.glUniform1i(texLoc, 0);
		 

		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		gl.glBindVertexArray(0);

		// ----------Axis Lines--------------
		if(showAxes) {
			// 1. reset model matrix to identity so axes sit at world origin
			mMat.identity();
			gl.glUniformMatrix4fv(mLoc, 1, false, mMat.get(vals));
			gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
			gl.glUniformMatrix4fv(pLoc, 1, false, pMat.get(vals));
			
			// 2. tell the shader dont sample textures, use u_color instead
			uTexLoc = gl.glGetUniformLocation(renderingProgram2, "u_texture");
			gl.glUniform1i(uTexLoc, 0);
			int colorLoc = gl.glGetUniformLocation(renderingProgram2, "u_color");
			
			Matrix4f axisMat = new Matrix4f().identity();
			axisMat.scale(5f);
			gl.glUniformMatrix4fv(mLoc, 1, false, axisMat.get(vals));

			// 4. bind your axis VAO and draw 3 lines (6 verts)
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
			gl.glBindVertexArray(0);
			
			// 5. turn texturing back on for subsequent draws
			gl.glUniform1i(uTexLoc, 1);
		}
	}

	public void init(GLAutoDrawable drawable)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		renderingProgram1 = Utils.createShaderProgram("code/vert1shader.glsl", "code/frag1shader.glsl");
		renderingProgram2 = Utils.createShaderProgram("code/vert2shader.glsl", "code/frag2shader.glsl");

		campfireModel  = new ImportedModel("Campfire.obj");
		numCampfireVertices = campfireModel.getNumVertices();
		campfireTexture = Utils.loadTexture("campfire.png");

		pandaModel     = new ImportedModel("panda.obj");
		numPandaVertices = pandaModel.getNumVertices();
		pandaTexture   = Utils.loadTexture("pandatx.jpg");

		flyModel       = new ImportedModel("fly.obj");
		numFlyVertices = flyModel.getNumVertices();
		flyTexture     = Utils.loadTexture("fly.jpg");

		floorTexture   = Utils.loadTexture("ground.jpg");
		skyboxTexture  = Utils.loadTexture("Night Sky.png");

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupVertices();
		//setupSkyboxVertices();
		setupCampfireVertices();       
		setupPandaVertices();
		//setupFlyVertices();
		setupFloorVertices();
		setupAxisLines();

		setupShadowBuffers();

		cameraX = 0.0f; cameraY = 3.0f; cameraZ = 8.0f;
		updateCameraVectors();
				
		b.set(
			0.5f, 0.0f, 0.0f, 0.0f,
			0.0f, 0.5f, 0.0f, 0.0f,
			0.0f, 0.0f, 0.5f, 0.0f,
			0.5f, 0.5f, 0.5f, 1.0f);
	}
	
	private void setupShadowBuffers()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
		scSizeX = myCanvas.getWidth();
		scSizeY = myCanvas.getHeight();
	
		gl.glGenFramebuffers(1, shadowBuffer, 0);
	
		gl.glGenTextures(1, shadowTex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadowTex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32,
						scSizeX, scSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
		
		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

		gl.glBindTexture(GL_TEXTURE_2D, floorTexture);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
	}

	private void setupVertices()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();
	
		// pyramid definition
		ImportedModel pyramid = new ImportedModel("pyr.obj");
		numPyramidVertices = pyramid.getNumVertices();
		Vector3f[] vertices = pyramid.getVertices();
		Vector3f[] normals = pyramid.getNormals();
		
		float[] pyramidPvalues = new float[numPyramidVertices*3];
		float[] pyramidNvalues = new float[numPyramidVertices*3];
		
		for (int i=0; i<numPyramidVertices; i++)
		{	pyramidPvalues[i*3]   = (float) (vertices[i]).x();
			pyramidPvalues[i*3+1] = (float) (vertices[i]).y();
			pyramidPvalues[i*3+2] = (float) (vertices[i]).z();
			pyramidNvalues[i*3]   = (float) (normals[i]).x();
			pyramidNvalues[i*3+1] = (float) (normals[i]).y();
			pyramidNvalues[i*3+2] = (float) (normals[i]).z();
		}

		// torus definition
		myTorus = new Torus(0.6f, 0.4f, 48);
		numTorusVertices = myTorus.getNumVertices();
		numTorusIndices = myTorus.getNumIndices();
		vertices = myTorus.getVertices();
		normals = myTorus.getNormals();
		int[] indices = myTorus.getIndices();
		
		float[] torusPvalues = new float[vertices.length*3];
		float[] torusNvalues = new float[normals.length*3];

		for (int i=0; i<numTorusVertices; i++)
		{	torusPvalues[i*3]   = (float) vertices[i].x();
			torusPvalues[i*3+1] = (float) vertices[i].y();
			torusPvalues[i*3+2] = (float) vertices[i].z();
			torusNvalues[i*3]   = (float) normals[i].x();
			torusNvalues[i*3+1] = (float) normals[i].y();
			torusNvalues[i*3+2] = (float) normals[i].z();
		}

		// buffers definition
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(5, vbo, 0);

		//  put the Torus vertices into the first buffer,
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(torusPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
		
		//  load the pyramid vertices into the second buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		FloatBuffer pyrVertBuf = Buffers.newDirectFloatBuffer(pyramidPvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrVertBuf.limit()*4, pyrVertBuf, GL_STATIC_DRAW);
		
		// load the torus normal coordinates into the third buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		FloatBuffer torusNorBuf = Buffers.newDirectFloatBuffer(torusNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, torusNorBuf.limit()*4, torusNorBuf, GL_STATIC_DRAW);
		
		// load the pyramid normal coordinates into the fourth buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		FloatBuffer pyrNorBuf = Buffers.newDirectFloatBuffer(pyramidNvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, pyrNorBuf.limit()*4, pyrNorBuf, GL_STATIC_DRAW);
		
		gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[4]);
		IntBuffer idxBuf = Buffers.newDirectIntBuffer(indices);
		gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, idxBuf.limit()*4, idxBuf, GL_STATIC_DRAW);
	}
	
	private void installLights(int renderingProgram)
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		lightPos[0]=currentLightPos.x(); lightPos[1]=currentLightPos.y(); lightPos[2]=currentLightPos.z();
		
		// set current material values
		matAmb = thisAmb;
		matDif = thisDif;
		matSpe = thisSpe;
		matShi = thisShi;
		
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
		gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
		gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
		gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
		gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
		gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, matAmb, 0);
		gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, matDif, 0);
		gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, matSpe, 0);
		gl.glProgramUniform1f(renderingProgram, mshiLoc, matShi);
	}

	// ----------- Vertice Functions --------------------
	private void setupCampfireVertices()
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
	
		int[] fireVAO = new int[1];
		gl.glGenVertexArrays(1, fireVAO, 0);
		campfireVAO = fireVAO[0];
		gl.glBindVertexArray(campfireVAO);
	
		campfireVBO = new int[3];
		gl.glGenBuffers(3, campfireVBO, 0);
	
		// Positions buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
	
		// Texture coordinates buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[1]);
		FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
		gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);
	
		// Normals buffer
		gl.glBindBuffer(GL_ARRAY_BUFFER, campfireVBO[2]);
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
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		setupShadowBuffers();
	}
}