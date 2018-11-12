package com.vring.alessandro.monsvr;

/**
 * Created by alessandro on 03/07/17.
 */

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;


public class MonsVrActivity extends GvrActivity implements  GvrView.StereoRenderer, SensorEventListener {

    private static final String TAG = "MonsVrActivity";
    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f; //mi allunga l'orizzonte
    private static final float CAMERA_Z = 5.0f;  //mi sposta indietro
    private static final float MAX_MODEL_DISTANCE = 3f;
    protected float[] modelPosition;

    private GvrView gvrView;

    private float[] camera;

    private float[] view;
    private float[] modelView;
    private float[] modelViewProjection;

    private static Context context;

    private SquaPlayer vid,vid2;
    private SquaBitmap imm;
    private SkyBox skyb;


    private boolean enablecard = false;

    private float[] headRotation;
    private float[] headView;
    private float[] rightVector;
    private float[] upVector;
    private float[] tranVector;

    private float[] tempPosition;
    private static final float[] POS_MATRIX_MULTIPLY_VEC = {0, 0, 0, 1.0f};
    private static final float YAW_LIMIT = 0.6f; //dovrebbero cambaire in funzione della distanza
    private static final float PITCH_LIMIT = 0.6f;

    private SensorManager mSensorManager;
    private Sensor magnetometer;

    private LinkedList<Squa> squalist;

    private int actual = 0, max = 0;


    public void initializeGvrView(){
        setContentView(R.layout.common_ui);

        gvrView = (GvrView) findViewById(R.id.gvr_view);
        gvrView.setEGLConfigChooser(8,8,8,8,16,8);
        gvrView.setRenderer(this);
        gvrView.setTransitionViewEnabled(true); //Enables/disables the transition view used to prompt the user to place their phone into a GVR viewer.
        gvrView.enableCardboardTriggerEmulation(); //Enables emulation of Cardboard-style touch trigger input when using a Daydream headset.
        if (gvrView.setAsyncReprojectionEnabled(true)) {
            AndroidCompat.setSustainedPerformanceMode(this, true);
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
        }
        setGvrView(gvrView); //Associates a GVR view with this activity.
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        initializeGvrView();
        context = this;
        camera = new float[16];
        view = new float[16];
        modelView = new float[16];
        modelViewProjection = new float[16];
        modelPosition = new float[] {0.0f, 0.0f, -MAX_MODEL_DISTANCE };
        headRotation = new float[4];
        headView = new float[16];
        rightVector = new float[3];
        upVector = new float[3];
        tranVector = new float[3];
        tempPosition = new float[4];

        squalist = new LinkedList<Squa>();

        mSensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        // Position the eye behind the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 0.01f;

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = 0.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;
        headTransform.getQuaternion(headRotation, 0);
        headTransform.getRightVector(rightVector,0);
        headTransform.getUpVector(upVector,0);
        headTransform.getTranslation(tranVector,0);
        headTransform.getHeadView(headView, 0);

        // Set the view matrix. This matrix can be said to represent the camera position.
        Matrix.setLookAtM(camera, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        //Log.i(TAG, "modelpos"+modelPosition[0]);
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);      //Sfondo diventa blu e fa la scia
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

        //Matrix.translateM(vid.getModel(), 0, (float) Math.sin(x),0,(float) Math.cos(x)); //"Ottimo"
        //imm.idraw(modelView,view,rightVector,modelViewProjection,perspective,headView);
        vid.idraw(modelView,view,rightVector,modelViewProjection,perspective,headView);
        vid2.idraw(modelView,view,rightVector,modelViewProjection,perspective,headView);// da valutare

        /*Log.d("headView X", ""+ headView[0]);
        Log.d("headView Y", ""+ headView[1]);
        Log.d("headView Z", ""+ headView[2]);*/
        Matrix.multiplyMM(modelView, 0, view, 0, skyb.getModel(), 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        skyb.draw(modelViewProjection);
    }


    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");
        /*TextView textView = new TextView(context);
        textView.layout(0, 0, 300, 500); //text box size 300px x 500px
        textView.setTextSize(5);
        textView.setTextColor(Color.WHITE);
        textView.setText("This is the second sample text which will be wrapped within the text box.");
        textView.setDrawingCacheEnabled(true);
        textView.requestFocus();
        textView.setEnabled(true);
        InputMethodManager im = (InputMethodManager)context.getSystemService(context.INPUT_METHOD_SERVICE);
        im.toggleSoftInput(InputMethodManager.SHOW_FORCED,InputMethodManager.HIDE_IMPLICIT_ONLY);
        im.showSoftInput(textView,InputMethodManager.HIDE_IMPLICIT_ONLY);
        imm.setBitmap(textView);*/
        //imm.ok = true;
        Squa s;
        boolean check;
        for(int i = 0; i < squalist.size(); i++){
            check = isLookingAtObject((s = squalist.get(i)).getModel());
            Log.d(TAG, ""+check);
            if(check) {
                s.setEnabled();
                break;
            }
        }
    }


    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Log.i(TAG, "onSurfaceCreated");
        //PER GLI SCHERMI NUOVI, CI DEVE ESSERE UNA ROTAZIONE DIVERSA E NON PROFONDITA'
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glClearColor(0.0f, 1.0f, 0.0f, 0.0f); // Dark background so text shows up well.


        squalist.add(vid = new SquaPlayer(modelPosition, context));
        vid.setPlayer(null, this);
        vid.setName("Video");

        squalist.add(vid2 = new SquaPlayer(modelPosition, context));
        vid2.setPlayer(null, this);
        vid2.setName("Video");
        /*Matrix.rotateM(vid.getModel(),0,45,0,1,0);
        Matrix.translateM(vid.getModel(),0,0,0,4f);*/
        /*squalist.add(vid2 = new SquaPlayer(modelPosition));
        vid2.setPlayer(null, this);
        vid2.setName("Video");

        */
        squalist.add(imm = new SquaBitmap(modelPosition, context));
        //imm.setBitmap(context.getResources(), R.drawable.sample); //Vedere se si può alleggerire il background
        Matrix.rotateM(imm.getModel(), 0, 180f, 0, 1f, 0); //"Ottimo"
        imm.setName("Immagine");



        skyb = new SkyBox(modelPosition);
        skyb.setPlayer(null,this);
        skyb.seek(10000);
        //Matrix.translateM(skyb.getModel(),0,0f,0f,-0.1f);
        Matrix.scaleM(skyb.getModel(),0,20f,20f,20f);
    }

    @Override
    public void onRendererShutdown() {
        vid.releasePlayer();
        skyb.releasePlayer();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {}

    private boolean isLookingAtObject(float[] model) {
        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(modelView, 0, headView, 0, model, 0);
        Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

        float pitch = (float) Math.atan2(tempPosition[1], -tempPosition[2]);
        float yaw = (float) Math.atan2(tempPosition[0], -tempPosition[2]);
        Log.d("pitch", ""+pitch);
        Log.d("yaw", ""+yaw);
        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] mGeomagnetic = new float[6];
        if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED)
            mGeomagnetic = sensorEvent.values;
        actual = (int) Math.sqrt(Math.pow(mGeomagnetic[0],2) + Math.pow(mGeomagnetic[1],2) + Math.pow(mGeomagnetic[2],2));
        /*Log.d("actual", ""+actual);
        Log.d("MAx", ""+max);*/
        if(actual > max)
            max = actual;
        else
            if(Math.abs(actual-max)>400) {
                onCardboardTrigger();
                actual = max = 0;
            }
        //onCardboardTrigger();
        /*Log.d("X", ""+mGeomagnetic[3]);
        Log.d("Y", ""+mGeomagnetic[4]);
        Log.d("Z", ""+mGeomagnetic[5]);*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

