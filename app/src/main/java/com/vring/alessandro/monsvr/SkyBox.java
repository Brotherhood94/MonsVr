package com.vring.alessandro.monsvr;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created by alessandro on 14/07/17.
 */

public class SkyBox {

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

    protected final String FRAGMENT_SHADER_CODE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;" +
                    //"uniform vec4 aColor;" +con l'altra combinazione serve questo
                    //"varying vec4 aColor;" +
                    "varying vec2 v_texCoord;" +
                    "uniform samplerExternalOES s_texture;" +
                    "void main() {" +
                    //"  gl_FragColor = aColor;" +
                    "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
                    "}";

    protected static float[] CUBE_VERTICES ={
            // front
            -1.0f, -1.0f,  1.0f,
            1.0f, -1.0f,  1.0f,
            1.0f,  1.0f,  1.0f,
            -1.0f,  1.0f,  1.0f,
            // back
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f,  1.0f, -1.0f,
            -1.0f,  1.0f, -1.0f,
    };

    protected static short[] CUBE_INDICES ={
            // front
            0, 1, 2,
            2, 3, 0,
            // top
            1, 5, 6,
            6, 2, 1,
            // back
            7, 6, 5,
            5, 4, 7,
            // bottom
            4, 0, 3,
            3, 7, 4,
            // left
            4, 5, 1,
            1, 0, 4,
            // right
            3, 2, 6,
            6, 7, 3,
    };

    protected float UVS[] = {

            1.0f, 1.0f,  //top right    "    "
            0.0f, 1.0f,
            0.0f, 0.0f,  //bottom left of texture
            1.0f, 0.0f,  //bottom right "    "

            0.0f, 0.0f,  //top right    "    "
            1.0f, 0.0f,
            1.0f, 1.0f,  //bottom left of texture
            0.0f, 1.0f,  //bottom right "    "


    };

    protected static final int COORDS_PER_VERTEX = 3;
    protected final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex
    protected FloatBuffer vertexBuffer;
    protected ShortBuffer indexBuffer;
    protected FloatBuffer uvBuffer;

    protected int mProgram;
    protected int mPositionHandle;
    protected int mTexCoordLoc;
    protected int mSamplerLoc;
    protected int mMVPMatrixHandle;

    protected float model[];
    private SurfaceTexture mSurface = null;
    private SimpleExoPlayer exoplayer;

    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
    protected IntBuffer texture;


    public SkyBox(float[] modelPosition){
        ByteBuffer vbb = ByteBuffer.allocateDirect(CUBE_VERTICES.length * 4); //Float 4 bytes
        vbb.order(ByteOrder.nativeOrder());
        vertexBuffer = vbb.asFloatBuffer();
        vertexBuffer.put(CUBE_VERTICES);
        vertexBuffer.position(0);

        ByteBuffer ibb = ByteBuffer.allocateDirect(CUBE_INDICES.length *2); //Short 2 bytes
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(CUBE_INDICES);
        indexBuffer.position(0);

        ByteBuffer uvbb = ByteBuffer.allocateDirect(UVS.length * 4);
        uvbb.order(ByteOrder.nativeOrder());
        uvBuffer = uvbb.asFloatBuffer();
        uvBuffer.put(UVS);
        uvBuffer.position(0);
        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);

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
        Matrix.translateM(model, 0, modelPosition[0], modelPosition[1], 0);

        setTexture();
    }

    //DA METTERE UN VIDEO PIÙ LUNGO COME SFONDO
    public void setPlayer(Uri uri, Context context){
        mSurface = new SurfaceTexture(texture.get(0));
        Surface surface = new Surface(mSurface);
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        exoplayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(context),
                new DefaultTrackSelector(videoTrackSelectionFactory),
                new DefaultLoadControl());
        exoplayer.setVolume(0);
        exoplayer.setVideoSurface(surface);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "MonsVr"), bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        File rootsd = Environment.getExternalStorageDirectory();
        File dcim = new File(rootsd.getAbsolutePath() + "/DCIM/MonsVr/video3602.mp4");
        //Uri uri = Uri.parse("http://192.168.1.105:8090/live.hls");
        MediaSource dashMediaSource = new ExtractorMediaSource(Uri.fromFile(dcim), dataSourceFactory,
                extractorsFactory, null, null); //Ripere finchè non lo trova ONLINE
        LoopingMediaSource loopingSource = new LoopingMediaSource(dashMediaSource);
        exoplayer.prepare(loopingSource);
        exoplayer.setPlayWhenReady(true);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void setTexture(){   //A zii ci sta che il type me lo sorvrascriva sempre. O ne uso un altro o lo carico e scarico di continuo
        GLES30.glUseProgram(mProgram);

        GLES30.glUniform1i(mSamplerLoc,2);

        texture = IntBuffer.allocate(1);
        GLES30.glGenBuffers(1, texture);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + 2);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture.get(0));
        GLES30.glBindSampler(2,mSamplerLoc);

        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    }


    public void draw(float[] mvpMatrix){
        mSurface.updateTexImage();
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + 2);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES,texture.get(0));

        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0); //Specify the value of a uniform variable for the current program object

        GLES30.glEnableVertexAttribArray(mPositionHandle); //Permette di gestire i vertici dei triangoli
        GLES30.glEnableVertexAttribArray(mSamplerLoc);

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, CUBE_INDICES.length, GLES30.GL_UNSIGNED_SHORT, indexBuffer);
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mSamplerLoc);
    }

    public float[] getModel(){
        return this.model;
    }

    public void seek(int ms){
        exoplayer.seekTo(ms);
    }

    public void releasePlayer(){
        exoplayer.release();
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
