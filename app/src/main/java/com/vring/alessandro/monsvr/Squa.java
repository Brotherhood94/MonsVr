package com.vring.alessandro.monsvr;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created by alessandro on 14/07/17.
 */

public abstract class Squa {
    protected static final String VERTEX_SHADER_CODE =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    //"attribute vec4 vColor;" +
                    "attribute vec2 a_texCoord;" +
                    //"varying vec4 aColor;" +
                    "varying vec2 v_texCoord;" +
                    "void main() {" +
                    //"aColor = vColor;" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "v_texCoord = a_texCoord;"+
                    "}";


    protected static float[] SQUARE_VERTICES ={
            //X,Y,Z,
            -1.0f,1.0f,0.0f,    //0, Top Left
            -1.0f,-1.0f,0.0f,   //1, Bottom Left
            1.0f,-1.0f,0.0f,    //2, Bottom Right
            1.0f,1.0f,0.0f      //3, Top Right
    };
    protected static short[] SQUARE_INDICES ={
            0,1,2,
            0,2,3
    };

    protected float SQUARE_COLORS[] = {
            //R,G,B,A
            1.0f, 0.0f, 1.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f,
            1.0f, 1.0f, 0.0f, 1.0f,
            1.0f, 0.0f, 0.0f, 1.0f
    };

    protected float UVS[] = {
            0.0f, 0.0f, //Top Left
            0.0f, 1.0f, //Bottom Left
            1.0f, 1.0f, //Bottom Right
            1.0f, 0.0f  //Top Right
    };

    protected static final int COORDS_PER_VERTEX = 3;
    protected final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    protected static final int COORDS_PER_COLORS = 4;
    protected final int colorStride = COORDS_PER_COLORS * 4; // 4 bytes per vertex

    protected float color[] = { 0.0f, 0.0f, 1.0f, 0.4f };

    protected FloatBuffer vertexBuffer;
    protected ShortBuffer indexBuffer;
    protected FloatBuffer colorBuffer;
    protected FloatBuffer uvBuffer;


    protected int mProgram;
    protected int mPositionHandle;
    protected int mColorHandle;
    protected int mTexCoordLoc;
    protected int mSamplerLoc;
    protected int mMVPMatrixHandle;

    private String name;
    protected float model[];

    protected IntBuffer texture;

    private float ds, ub;
    private boolean enabled = false, enabled2 = false;
    int mode = 0;
    protected Context context;

    public Squa(float[] modelPosition, Context context){
        this.context = context;
        ByteBuffer vbb = ByteBuffer.allocateDirect(SQUARE_VERTICES.length * 4); //Float 4 bytes
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(SQUARE_VERTICES);
        vertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(SQUARE_INDICES.length *2); //Short 2 bytes
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(SQUARE_INDICES);
        indexBuffer.position(0);


        ByteBuffer uvbb = ByteBuffer.allocateDirect(UVS.length * 4);
        uvbb.order(ByteOrder.nativeOrder());
        uvBuffer = uvbb.asFloatBuffer();
        uvBuffer.put(UVS);
        uvBuffer.position(0);

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, getFragment());

        mProgram = GLES30.glCreateProgram();    //Creo un programma OpenGL vuoto
        GLES30.glAttachShader(mProgram, vertexShader); //Aggiungo il vertexShader al programma
        GLES30.glAttachShader(mProgram, fragmentShader);
        GLES30.glLinkProgram(mProgram);    //Creo l'eseguibile del programma OpenGL

        GLES30.glDeleteShader(vertexShader);
        GLES30.glDeleteShader(fragmentShader);

        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");    //Returns the location of an attribute variable
        mTexCoordLoc = GLES30.glGetAttribLocation(mProgram, "a_texCoord");
        mSamplerLoc = GLES30.glGetUniformLocation(mProgram, "s_texture");

        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");        //get handle to shape's trnasformation matrix


        GLES30.glEnable (GLES30.GL_BLEND);
        GLES30.glBlendFunc (GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES30.GL_FLOAT, false, vertexStride, vertexBuffer); //https://www.khronos.org/registry/OpenGL-Refpages/es2.0/xhtml/glVertexAttribPointer.xml
        GLES30.glVertexAttribPointer(mTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, uvBuffer);

        model = new float[16];
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, modelPosition[0], modelPosition[1], -4f);

        setTexture();
    }

    /*@Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        Squa s;
        boolean check;
        for(int i = 0; i < squalist.size(); i++){
            check = isLookingAtObject((s = squalist.get(i)).getModel());
            Log.d(TAG, ""+check);
            if(check)
                s.setEnabled();
        }
    }
*/
    public void setName(String s){
        name = s;
    }

    public String getName(){
        return name;
    }

    //vid.idraw(modelView,view,rightVector,modelViewProjection,perspective,upVector);

    public void idraw(float[] modelView, float[] view, float[] rightVector, float[] modelViewProjection, float[] perspective, float[] upVector){
        if(enabled) {
            Matrix.setIdentityM(model, 0);
            ds += -rightVector[1] * 2;
            ds = ds % 360;
            Log.d("ds", " "+ds);
            Matrix.rotateM(model, 0, ds, 0, 1f, 0);
            //Matrix.translateM(model, 0, 0, 0, ub+3f);
            Matrix.translateM(model, 0, 0, 0, -4f-ub);
        }
        if(enabled2){
            Matrix.setIdentityM(model, 0);
            if(ds >= -90 && ds <= -360)
                ub = ub + upVector[1]/10;
            else
                ub = ub - upVector[1]/10;
            //Log.d("headView Y", ""+ ub);
            Matrix.rotateM(model, 0, ds, 0, 1f, 0);
            Matrix.translateM(model, 0, 0, 0, -4f-ub);
        }
        Matrix.multiplyMM(modelView, 0, view, 0, model, 0);
        /*if(enabled) {
            Matrix.rotateM(model, 0, rightVector[1] * 2, 0, 1f, 0); //"Ottimo"
            pos+=upVector[2]/10;
        }
        Matrix.translateM(modelView, 0, 0, 0, pos+3f);
        */
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        draw(modelViewProjection);
    }

    public void setEnabled(){
        Log.i("Enable", " "+getName());
        mode++;
        mode = mode%3;
        if(mode==1) {
            enabled2 = false;
            enabled = !enabled;
        }else if(mode==2){
            enabled = false;
            enabled2 = !enabled2;
        }else{
            enabled = false;
            enabled2 = false;
        }
    }

    public abstract void draw(float[] mvpMatrix);

    public abstract String getFragment();

    public abstract void setTexture();

    public float[] getModel(){
        return this.model;
    }


    protected int loadShader(int type, String code){
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, code);
        GLES30.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e("Square", "Error compiling shader: " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }
        return shader;
    }
}
