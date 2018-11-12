package com.vring.alessandro.monsvr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.IntBuffer;

/**
 * Created by alessandro on 14/07/17.
 */

public class SquaBitmap extends Squa{

    private Bitmap bmp;
    public boolean ok = false;
    private TextView textView;

    private final String FRAGMENT_SHADER_CODE =
            "precision mediump float;" +
                    //"uniform vec4 aColor;" +con l'altra combinazione serve questo
                    //"varying vec4 aColor;" +
                    "varying vec2 v_texCoord;" +
                    "uniform sampler2D s_texture;" +
                    "void main() {" +
                    //"  gl_FragColor = aColor;" +
                    "  gl_FragColor = texture2D( s_texture, v_texCoord );" +
                    "}";

    public SquaBitmap(float[] modelPosition, Context context){
        super(modelPosition, context);
    }

    public String getFragment(){
        return FRAGMENT_SHADER_CODE;
    }

    public void draw(float[] mvpMatrix){

        GLES30.glUseProgram(mProgram);
        //setBitmap2();
        //bmp.recycle();
        if(ok)
            setBitmap2();
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture.get(0));
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0); //Specify the value of a uniform variable for the current program object

        GLES30.glEnableVertexAttribArray(mPositionHandle); //Permette di gestire i vertici dei triangoli
        GLES30.glEnableVertexAttribArray(mSamplerLoc);

        //Draw the square
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, SQUARE_INDICES.length, GLES30.GL_UNSIGNED_SHORT, indexBuffer);
        GLES30.glDisableVertexAttribArray(mPositionHandle);
        GLES30.glDisableVertexAttribArray(mSamplerLoc);
    }

    public void setBitmap(){
        bmp = Bitmap.createBitmap(449, 500, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0);
        Canvas canvas = new Canvas(bmp);
        textView = new TextView(context);
        textView.layout(0, 0, 300, 500); //text box size 300px x 500px
        textView.setTextSize(5);
        textView.setTextColor(Color.BLUE);
        textView.setDrawingCacheEnabled(true);
        textView.setText("CIAOOOo");
        canvas.drawBitmap(textView.getDrawingCache(), 50, 50, null);
        textView.requestFocus();
        textView.setEnabled(true);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.get(0));
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        //GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
       /* bmp = Bitmap.createBitmap(449, 500, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0);
        Canvas canvas = new Canvas(bmp);
        EditText et = new EditText(context);
        et.setTextSize(10);
        et.setTextColor(Color.RED);
        et.setText("CIAO MONDO");
        canvas.drawBitmap(et.getDrawingCache(),50,50,null);*/

    }
    /*editText.setCursorVisible(false);*/

    public void setBitmap2(){   //A zii ci sta che il type me lo sorvrascriva sempre. O ne uso un altro o lo carico e scarico di continuo
        bmp = Bitmap.createBitmap(449, 500, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(0);
        Canvas canvas = new Canvas(bmp);
        EditText editText = new EditText(context);
        editText.setEnabled(true);
        /*TextView textView = new TextView(context);
        textView.layout(0, 0, 300, 500); //text box size 300px x 500px
        textView.setTextSize(5);
        textView.setTextColor(Color.RED);
        textView.setDrawingCacheEnabled(true);*/
        editText.setDrawingCacheEnabled(true);
        editText.setTextSize(10);
        editText.setTextColor(Color.RED);
        editText.layout(0,0,300,500);
        editText.setFocusable(true);

        editText.setText("This is the second sample text which will be wrapped within the text box.");
        canvas.drawBitmap(editText.getDrawingCache(), 50, 50, null);
        textView.requestFocus();
        textView.setEnabled(true);

        //GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.get(0));
        GLUtils.texSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, bmp);
        bmp.recycle();
        ok = false;


    }

    public void setTexture(){   //A zii ci sta che il type me lo sorvrascriva sempre. O ne uso un altro o lo carico e scarico di continuo

        texture = IntBuffer.allocate(1);
        GLES30.glGenBuffers(1, texture);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.get(0));
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        //GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        setBitmap();

    }


}
