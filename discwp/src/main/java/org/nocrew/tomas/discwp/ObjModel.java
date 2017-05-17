package org.nocrew.tomas.discwp;

import android.content.Context;
import android.graphics.Color;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class ObjModel {

    private static Random rand = new Random();
    private static int[] vboHandle;
    private static final int VBO_VERTICES = 0;
    private static final int VBO_NORMALS  = 1;
    private static Model model;

    private float xRotate = 0.0f, yRotate = 0.0f, zRotate = 0.0f;
    private float xRotateSpeed = 0.0f, yRotateSpeed = 0.0f;
    private float xTrans = 0.0f, yTrans = 0.0f, zTrans = 0.0f;
    private float xTransSpeed = 0.0f, yTransSpeed = 0.0f, zTransSpeed = 0.0f;

    private Context context;
    private float xMax, yMax;
    private int colorDisc, colorDisc2;
    private float[] col = new float[4];
    private float[] specCol = new float[4];

    public ObjModel(Context context, GL10 gl, boolean reinitVBO,
		    String type,
		    float xMaxRange, float yMaxRange,
		    int coldisc, int coldisc2,
		    int shininess) {
	GL11 gl11 = (GL11)gl;

	this.context = context;
	xMax = xMaxRange;
	yMax = yMaxRange;
	colorDisc = coldisc;
	colorDisc2 = coldisc2;

	if(type.equals("simple")) {
	    shininess += 4;
	    shininess /= 5;
	}

	setColor();
	setSpecularColor(shininess);
	
	xTrans = (xMax - 1.0f) * 2.0f * rand.nextFloat() - xMax;
	yTrans = (yMax - 2.0f) * 2.0f * rand.nextFloat() - yMax;
	zTrans = -5.0f - 1.0f * rand.nextFloat();

	xTransSpeed = 0.005f + 0.01f * rand.nextFloat();
	yTransSpeed = 0.005f + 0.01f * rand.nextFloat();
	zTransSpeed = 0.01f * (rand.nextFloat() - 0.5f);
	zRotate = 90.0f + 20.0f * rand.nextFloat();
	xRotateSpeed = 0.5f + 1.5f * rand.nextFloat();

	if(vboHandle != null && !reinitVBO)
	    return;

	vboHandle = new int[2];

	model = new Model(type);

	gl11.glGenBuffers(2, vboHandle, 0);

	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle[VBO_VERTICES]);
	gl11.glBufferData(GL11.GL_ARRAY_BUFFER,
			  model.numFaces * 3 * 3 * Float.SIZE / 8,
			  model.verts, GL11.GL_STATIC_DRAW);

	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle[VBO_NORMALS]);
	gl11.glBufferData(GL11.GL_ARRAY_BUFFER,
			  model.numFaces * 3 * 3 * Float.SIZE / 8,
			  model.normals, GL11.GL_STATIC_DRAW);

	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
    }

    private void setColor() {
	float rnd = rand.nextFloat();
	col[0] = colorFloatRandom(Color.red(colorDisc),
				  Color.red(colorDisc2),
				  rnd);
	col[1] = colorFloatRandom(Color.green(colorDisc),
				  Color.green(colorDisc2),
				  rnd);
	col[2] = colorFloatRandom(Color.blue(colorDisc),
				  Color.blue(colorDisc2),
				  rnd);
	col[3] = colorFloatRandom(Color.alpha(colorDisc),
				  Color.alpha(colorDisc2),
				  rnd);
    }

    private void setSpecularColor(int shininess) {
	specCol[0] = (float)shininess / 100.0f;
	specCol[1] = (float)shininess / 100.0f;
	specCol[2] = (float)shininess / 100.0f;
	specCol[3] = 1.0f;
    }

    private float colorFloat(int component) {
	return (float)component / 255.0f;
    }

    private float colorFloatRandom(int comp1, int comp2, float rnd) {
	float f1 = colorFloat(comp1);
	float f2 = colorFloat(comp2);
	return rnd * f1 + (1.0f - rnd) * f2;
    }

    public void update(float diffTime) {
	xRotate += xRotateSpeed * diffTime / 16.667;
	yRotate += yRotateSpeed * diffTime / 16.667;
	xTrans += xTransSpeed * diffTime / 16.667;
	yTrans += yTransSpeed * diffTime / 16.667;
	zTrans += zTransSpeed * diffTime / 16.667;

	if(xTrans > xMax || yTrans > yMax) {
	    xTrans = -xMax;
	    yTrans = -yMax + yMax * rand.nextFloat();
	    zTrans = -7.0f - 2.0f * rand.nextFloat();
	    xTransSpeed = 0.005f + 0.01f * rand.nextFloat();
	    yTransSpeed = 0.005f + 0.01f * rand.nextFloat();
	    zTransSpeed = 0.01f * (rand.nextFloat() - 0.5f);
	    zRotate = 90.0f + 20.0f * rand.nextFloat();
	    xRotateSpeed = 0.5f + 1.5f * rand.nextFloat();
	    setColor();
	}
    }

    public void draw(GL10 gl) {
	GL11 gl11 = (GL11)gl;

	gl.glPushMatrix();

	gl.glTranslatef(xTrans, yTrans, zTrans);

	gl.glRotatef(zRotate, 0, 0, 1);
	gl.glRotatef(xRotate, 1, 0, 0);

	gl.glScalef(2.0f, 2.0f, 2.0f);

	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle[VBO_VERTICES]);
	gl11.glVertexPointer(3, GL10.GL_FLOAT, 0, 0);
	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);

	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, vboHandle[VBO_NORMALS]);
	gl11.glNormalPointer(GL10.GL_FLOAT, 0, 0);
	gl11.glBindBuffer(GL11.GL_ARRAY_BUFFER, 0);
	
	gl.glMaterialfv(GL10.GL_FRONT_AND_BACK,
			GL10.GL_AMBIENT_AND_DIFFUSE, col, 0);
	gl.glMaterialfv(GL10.GL_FRONT_AND_BACK,
			GL10.GL_SPECULAR, specCol, 0);
	gl.glMaterialf(GL10.GL_FRONT_AND_BACK,
		       GL10.GL_SHININESS, 50.0f);

	gl.glDrawArrays(GL10.GL_TRIANGLES, 0, model.numFaces * 3);

	gl.glPopMatrix();
    }



    private class Model {
	public int numFaces;
	public FloatBuffer verts;
	public FloatBuffer normals;

	public Model(String type) {
	    int res;

	    if(type.equals("simple"))
		res = R.raw.modelsimple;
	    else if(type.equals("fancy"))
		res = R.raw.modelfancy;
	    else
		res = R.raw.modelfancyhq;

	    InputStream ins = context.getResources().openRawResource(res);
	    DataInputStream dins = new DataInputStream(ins);
	    try {
		numFaces = dins.readInt();
		verts = FloatBuffer.allocate(numFaces * 3 * 3);
		normals = FloatBuffer.allocate(numFaces * 3 * 3);
		for(int i = 0 ; i < numFaces * 3 * 3 ; i++) {
		    verts.put(dins.readFloat());
		}
		for(int i = 0 ; i < numFaces * 3 * 3 ; i++) {
		    normals.put(dins.readFloat());
		}
		ins.close();
	    } catch(IOException ex) {
	    }
	    verts.position(0);
	    normals.position(0);
	}
    }

}
