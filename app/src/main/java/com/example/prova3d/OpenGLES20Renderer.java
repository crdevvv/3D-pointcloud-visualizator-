package com.example.prova3d;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.vecmath.Vector3f;

public class OpenGLES20Renderer implements GLSurfaceView.Renderer {


    private int mProgram;
    private int maPositionHandle;
    private FloatBuffer vertexBuffer;
    private FloatBuffer colorBuffer;

    private int muMVPMatrixHandle;
    private final float[] mMVPMatrix = new float[16];
    private final float[] mRotationZMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mRotationXMatrix = new float[16];
    private final float[] mTempMatrix = new float[16];
    private final float[] mTempMatrix2 = new float[16];
    private final float[] mTranslationMatrix = new float[16];

    private final float[] resultNear = new float[4];
    private final float[] resultFar = new float[4];

    public float mTranslX;

    public float mAngleZ;
    public float mAngleX;

    public final Object lock = new Object();
    public float mScaleFactor;
    public float[] vertices;
    public float[] colors;
    public List<Float> vert= new ArrayList<>();
    public List<Float> col = new ArrayList<>();


    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Matrix.setIdentityM(mTranslationMatrix,0);
        Matrix.setRotateM(mRotationZMatrix, 0, mAngleZ, 0f, 1.0f, 0f);
        Matrix.setRotateM(mRotationXMatrix, 0, mAngleX, 1f, 0f, 0f);
        Matrix.scaleM(mRotationZMatrix, 0, mScaleFactor, mScaleFactor, mScaleFactor);
        Matrix.scaleM(mRotationXMatrix, 0, mScaleFactor, mScaleFactor, mScaleFactor);
        Matrix.translateM(mTranslationMatrix,0,mTranslX,0,0);

        Matrix.multiplyMM(mTempMatrix, 0, mRotationZMatrix, 0, mRotationXMatrix, 0);
        Matrix.multiplyMM(mTempMatrix2, 0, mTempMatrix, 0, mTranslationMatrix, 0);

        Matrix.multiplyMM(mMVPMatrix, 0, mVMatrix, 0, mTempMatrix2, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

        GLES20.glUseProgram(mProgram);

        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);

        int colorHandle = GLES20.glGetAttribLocation(mProgram, "vColorA");
        GLES20.glEnableVertexAttribArray(colorHandle);
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 12, colorBuffer);

        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertices.length);
        GLES20.glDisableVertexAttribArray(maPositionHandle);
        GLES20.glDisableVertexAttribArray(colorHandle);
        System.out.println("\n");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        float centerX = (findXMin() + findXMax()) / 2.0f;
        float centerY = (findYMin() + findYMax()) / 2.0f;
        float centerZ = (findZMin() + findZMax()) / 2.0f;
        Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 1f, 1000f);
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");

        Matrix.setLookAtM(mVMatrix, 0,findXMin(),findYMin(),findZMin(), centerX, centerY, centerZ, 0f, 1f, 0f);
        Matrix.translateM(mVMatrix,0,mVMatrix,0,centerX,centerY,centerZ);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        System.out.println("zzzzzzzz");
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_NICEST);
        GLES20.glDepthFunc(GLES20.GL_LESS);
        GLES20.glDepthMask(true);
        vertices = convertFloatListToArray(vert);
        colors = convertFloatListToArray(col);
        initShapes();

        String vertexShaderCode = "uniform mat4 uMVPMatrix;" +
                "attribute vec4 vPosition;" +
                "attribute vec4 vColorA;" +
                "varying vec4 vColor;" +
                "void main() {" +
                "  gl_Position = uMVPMatrix * vPosition;" +
                "  vColor = vColorA;" +
                "}";
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        String fragmentShaderCode = "precision mediump float;" +
                "varying vec4 vColor;" +
                "void main() {" +
                "   gl_FragColor = vec4(vColor.rgb, 1.0);" +
                "}";
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // creates OpenGL program executables

        // get handle to the vertex shader's vPosition member
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

    }
    private void initShapes(){

        // initialize vertex Buffer for triangle
        {
            ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
            // (# of coordinate values * 4 bytes per float)
            vbb.order(ByteOrder.nativeOrder());// use the device hardware's native byte order
            vertexBuffer = vbb.asFloatBuffer();  // create a floating point buffer from the ByteBuffer
            vertexBuffer.put(vertices);    // add the coordinates to the FloatBuffer
            vertexBuffer.position(0);  // set the buffer to read the first coordinate
        }
        {
            ByteBuffer bb = ByteBuffer.allocateDirect(colors.length * 4);
            bb.order(ByteOrder.nativeOrder());
            colorBuffer = bb.asFloatBuffer();
            colorBuffer.put(colors);
            colorBuffer.position(0);
        }
    }

    public float[] convertFloatListToArray(List<Float> floatList) {
        float[] array = new float[floatList.size()];
        for (int i=0; i< floatList.size()/3; i++){
            array[i*3] = floatList.get(i*3);
            array[i*3+1] = floatList.get(i*3+1);
            array[i*3+2] = floatList.get(i*3+2);
        }
        return array;
    }

    private int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }



    public float findXMax() {
        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3]);
        }
        Optional<Float> max = floatList.parallelStream().max(Comparator.naturalOrder());

        return max.get();
    }
    public float findYMax() {
        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3+1]);
        }
        Optional<Float> max = floatList.parallelStream().max(Comparator.naturalOrder());
        return max.get();
    }

    public float findZMax() {

        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3+2]);
        }
        Optional<Float> max = floatList.parallelStream().max(Comparator.naturalOrder());
        return max.get();
    }
    public float findXMin() {
        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3]);
        }
        Optional<Float> min = floatList.parallelStream().min(Comparator.naturalOrder());

        return min.get();

    }
    public float findYMin() {
        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3+1]);
        }
        Optional<Float> min = floatList.parallelStream().min(Comparator.naturalOrder());

        return min.get();

    }
    public float findZMin() {
        List<Float> floatList = new ArrayList<>();
        for (int i = 0; i < vertices.length/3; i++) {
            floatList.add(vertices[i*3+2]);
        }
        Optional<Float> min = floatList.parallelStream().min(Comparator.naturalOrder());

        return min.get();

    }
}