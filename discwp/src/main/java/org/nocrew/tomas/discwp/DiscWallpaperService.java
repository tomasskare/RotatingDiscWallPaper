package org.nocrew.tomas.discwp;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.GLU;
import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import net.rbgrn.opengl.*;

public class DiscWallpaperService extends GLWallpaperService {
    public final static int DEFAULT_NUM_DISCS = 20;
    public final static int DEFAULT_COLOR_BG = Color.BLACK;
    public final static int DEFAULT_COLOR_DISC = Color.RED;
    public final static int DEFAULT_COLOR_DISC2 = Color.YELLOW;
    public final static int DEFAULT_LIGHT_BRIGHTNESS = 50;
    public final static int DEFAULT_DISC_SHININESS = 50;
    public final static String DEFAULT_DISC_TYPE = "simple";

    public DiscWallpaperService() {
	super();
    }

    @Override
    public Engine onCreateEngine() {
        return new DiscEngine();
    }

    private class DiscEngine extends GLEngine {

	private DiscRenderer renderer;

	private float xOffset, yOffset;

        public DiscEngine() {
	    super();

	    if(renderer == null) {
		renderer = new DiscRenderer();
		setRenderer(renderer);
		setRenderMode(RENDERMODE_CONTINUOUSLY);
	    }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

	    if (renderer != null) {
                renderer.release();
		renderer = null;
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
				     float xStep, float yStep,
				     int xPixels, int yPixels) {
	    this.xOffset = xOffset;
	    this.yOffset = yOffset;

	    final float xOff = xOffset;
	    final float yOff = yOffset;

	    queueEvent(new Runnable(){
		    public void run() {
			renderer.setOffset(xOff, yOff);
		    }});
        }
    }

    private class DiscRenderer
	implements GLSurfaceView.Renderer,
		   SharedPreferences.OnSharedPreferenceChangeListener {

	private GL10 mgl;
	private SharedPreferences prefs;

	private float[] ambientComponent0 = {0.3f, 0.3f, 0.3f, 1.0f};
	private float[] diffuseComponent0 = {1.0f, 1.0f, 1.0f, 1.0f};
	private float[] specularComponent0 = {1.0f, 1.0f, 1.0f, 1.0f};
	private float[] lightPosition0 =    {1f, 1f, 5f, 0f};

	private int numDiscs = DEFAULT_NUM_DISCS;
	private int colorBg = DEFAULT_COLOR_BG;
	private int colorDisc = DEFAULT_COLOR_DISC;
	private int colorDisc2 = DEFAULT_COLOR_DISC2;
	private int shininess = DEFAULT_DISC_SHININESS;
	private String discType = DEFAULT_DISC_TYPE;

	private ObjModel[] disc;
	private boolean reinitVBO = true;

	private long lastDrawTime = 0;

	private float xMax, yMax;
	private float xOffset, yOffset;

	public DiscRenderer() {
	    super();

	    prefs = DiscWallpaperService.this.getSharedPreferences("prefs", 0);
	    prefs.registerOnSharedPreferenceChangeListener(this);
	    onSharedPreferenceChanged(prefs, null);
	}

        public void onSharedPreferenceChanged(SharedPreferences prefs,
					      String key) {
	    numDiscs = prefs.getInt("pref_number_discs", DEFAULT_NUM_DISCS);
	    discType = prefs.getString("pref_disc_type", DEFAULT_DISC_TYPE);
	    colorBg = prefs.getInt("pref_color_bg", DEFAULT_COLOR_BG);
	    colorDisc = prefs.getInt("pref_color_disc", DEFAULT_COLOR_DISC);
	    colorDisc2 = prefs.getInt("pref_color_disc2", DEFAULT_COLOR_DISC2);
	    shininess = prefs.getInt("pref_disc_shininess",
				     DEFAULT_DISC_SHININESS);
	    int bright = prefs.getInt("pref_light_brightness",
				      DEFAULT_LIGHT_BRIGHTNESS);
	    float light = (float)bright / 100.0f;
	    diffuseComponent0[0] = light;
	    diffuseComponent0[1] = light;
	    diffuseComponent0[2] = light;
	    light += 0.5f;
	    if(light > 1.0f)
		light = 1.0f;
	    specularComponent0[0] = light;
	    specularComponent0[1] = light;
	    specularComponent0[2] = light;
        }

	private void setOffset(float xOffset, float yOffset) {
	    this.xOffset = xOffset - 0.5f;
	    this.yOffset = yOffset - 0.5f;
	}

	private float colorFloat(int component) {
	    return (float)component / 255.0f;
	}

	public void onDrawFrame(GL10 gl) {
	    gl.glClearColor(colorFloat(Color.red(colorBg)),
			    colorFloat(Color.green(colorBg)),
			    colorFloat(Color.blue(colorBg)),
			    1.0f);
	    gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

	    gl.glMatrixMode(GL10.GL_MODELVIEW);
	    gl.glLoadIdentity();

	    gl.glTranslatef(-xOffset * 3.0f, -yOffset * 3.0f, -5.0f);

	    long newTime = System.nanoTime();
	    long diffTime = newTime - lastDrawTime;
	    if(diffTime > (long)1000000 * 5000)
		diffTime = 1000000 * 16;
	    lastDrawTime = newTime;

	    for(int i = 0 ; i < numDiscs ; i++) {
		disc[i].update((float)diffTime / 1000000.0f);
		disc[i].draw(gl);
	    }
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
	    mgl = gl;

	    if(height > width) {
		xMax = 10.0f;
		yMax = xMax +
		    3.0f * ((float)height / (float)width) / (1184.0f/720.0f);
	    } else {
		yMax = 10.0f;
		xMax = yMax +
		    4.0f * ((float)height / (float)width) / (1184.0f/720.0f);
	    }

	    disc = new ObjModel[numDiscs];
	    for(int i = 0 ; i < numDiscs ; i++) {
		disc[i] = new ObjModel(DiscWallpaperService.this,
				       gl, reinitVBO, discType,
				       xMax, yMax,
				       colorDisc, colorDisc2,
				       shininess);
		reinitVBO = false;
	    }

	    lastDrawTime = System.nanoTime();

	    gl.glViewport(0, 0, width, height);

	    gl.glMatrixMode(GL10.GL_PROJECTION);
	    gl.glLoadIdentity();
	    GLU.gluPerspective(gl, 60f, (float)width/(float)height, 1f, 100f);

	    gl.glMatrixMode(GL10.GL_MODELVIEW);
	    gl.glLoadIdentity();
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	    mgl = gl;

	    reinitVBO = true;

	    gl.glClearDepthf(1f);
	    gl.glEnable(GL10.GL_DEPTH_TEST);
	    gl.glDepthFunc(GL10.GL_LEQUAL);

	    gl.glDisable(GL10.GL_CULL_FACE);
	    gl.glFrontFace(GL10.GL_CCW);

	    gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
	    gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);

	    gl.glEnable(GL10.GL_BLEND);
	    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

	    gl.glEnable(GL10.GL_LIGHTING);
	    gl.glEnable(GL10.GL_RESCALE_NORMAL);

	    gl.glShadeModel(GL10.GL_SMOOTH);
	    gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	    gl.glEnable(GL10.GL_LINE_SMOOTH);

	    gl.glEnable(GL10.GL_LIGHT0);
	    gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambientComponent0, 0);
	    gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diffuseComponent0, 0);
	    gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, specularComponent0, 0);
	    gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPosition0, 0);

	    float[] ambientLightRGB = {0.3f, 0.3f, 0.3f, 1.0f};
	    gl.glLightModelfv(GL10.GL_LIGHT_MODEL_AMBIENT, ambientLightRGB, 0);
	    gl.glLightModelx(GL10.GL_LIGHT_MODEL_TWO_SIDE, GL10.GL_TRUE);
	}

	private void release() {
	    if(prefs != null) {
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		prefs = null;
	    }
	}
    }

}