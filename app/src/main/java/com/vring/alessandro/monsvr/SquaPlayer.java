package com.vring.alessandro.monsvr;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES30;
import android.os.Environment;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.nio.IntBuffer;

/**
 * Created by alessandro on 14/07/17.
 */

public class SquaPlayer extends Squa {

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

    private SurfaceTexture mSurface = null;
    private SimpleExoPlayer exoplayer;
    private static int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    public SquaPlayer(float[] modelPosition, Context context){
        super(modelPosition, context);
    }

    public String getFragment(){
        return FRAGMENT_SHADER_CODE;
    }

    public void draw(float[] mvpMatrix){
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + 0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES,texture.get(0));
        mSurface.updateTexImage();
        GLES30.glUseProgram(mProgram);

        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0); //Specify the value of a uniform variable for the current program object

        GLES30.glEnableVertexAttribArray(mPositionHandle); //Permette di gestire i vertici dei triangoli
        GLES30.glEnableVertexAttribArray(mSamplerLoc);

        //Draw the square
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, SQUARE_INDICES.length, GLES30.GL_UNSIGNED_SHORT, indexBuffer);
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mSamplerLoc);
    }

    public void setPlayer(Uri uri, Context context){
        mSurface = new SurfaceTexture(texture.get(0));
        Surface surface = new Surface(mSurface);
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        exoplayer = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(context),
                new DefaultTrackSelector(videoTrackSelectionFactory),
                new DefaultLoadControl());
        exoplayer.setVideoSurface(surface);
        exoplayer.setVolume(0);
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "MonsVr"), bandwidthMeter);
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        File rootsd = Environment.getExternalStorageDirectory();
        File dcim = new File(rootsd.getAbsolutePath() + "/DCIM/Camera/test.mp4");
        //Uri uri = Uri.parse("http://192.168.1.105:8090/live.hls");
        MediaSource dashMediaSource = new ExtractorMediaSource(Uri.fromFile(dcim), dataSourceFactory,
                extractorsFactory, null, null); //Ripere finchè non lo trova ONLINE
        exoplayer.prepare(dashMediaSource);
        exoplayer.setPlayWhenReady(true);

    }

    public void setTexture(){   //A zii ci sta che il type me lo sorvrascriva sempre. O ne uso un altro o lo carico e scarico di continuo
        texture = IntBuffer.allocate(1);
        GLES30.glGenBuffers(1, texture);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + 0);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture.get(0));

        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glBindTexture(GL_TEXTURE_EXTERNAL_OES, 0);
    }

    public void releasePlayer(){
        exoplayer.release();
    }


}
