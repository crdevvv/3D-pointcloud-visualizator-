package com.example.prova3d;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;


public class OpenGLES20SurfaceView extends GLSurfaceView {

    private final OpenGLES20Renderer mRenderer;
    private final ScaleGestureDetector mScaleDetector;
    private float mLastTouchX;
    private float mLastTouchY;
    private boolean previousTwoTouch=false;
    private boolean firstTouch=false;
    private boolean secondTouch=false;

    public long[] deltaTime = new long[2];
    public float[] touches = new float[4];

    public OpenGLES20SurfaceView(Context context) {
        super(context);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        // Set the Renderer for drawing on the GLSurfaceView
        mScaleDetector = new ScaleGestureDetector(context,new PinchZoomListener());
        mRenderer = new OpenGLES20Renderer();
        setRenderer(mRenderer);
        //Render the view only when there is a change
        this.requestFocus();
        this.setFocusableInTouchMode(true);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

    }

    public void setRendererVertices(float[] vertices1){
        for (int i=0; i<vertices1.length/3; i++) {
            mRenderer.vert.add(vertices1[i * 3]);
            mRenderer.vert.add(vertices1[i * 3 + 1]);
            mRenderer.vert.add(vertices1[i * 3 + 2]);
        }
    }
    public void setRendererColors(float[] colors1){
        for (int i=0; i<colors1.length/3; i++){
            mRenderer.col.add(colors1[i * 3]);
            mRenderer.col.add(colors1[i * 3+1]);
            mRenderer.col.add(colors1[i * 3+2]);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent e) {

        System.out.println("inizio evento");
        float x=e.getX();
        float y=e.getY();
        long startTouchTime;
        long endTouchTime;

        mScaleDetector.onTouchEvent(e);
        if( mScaleDetector.isInProgress() ) {
            previousTwoTouch = true;
            return true;
        }
        if (e.getPointerCount()==1 && previousTwoTouch){
            previousTwoTouch=false;
            mLastTouchX=x;
            mLastTouchY=y;
        } else if (e.getPointerCount()==2) {
            previousTwoTouch=true;

        }

        if (!mScaleDetector.isInProgress()) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN: {

                    startTouchTime = System.currentTimeMillis();
                    deltaTime[0] = startTouchTime;
                    if (!firstTouch) {
                        touches[0] = x;
                        touches[1] = y;
                        firstTouch=true;

                    }
                    if(secondTouch){
                        touches[2]=x;
                        touches[3]=y;
                        secondTouch=false;
                    }
                    //System.out.println(e.getEventTime() + "event time"); t inizio dal boot
                    //System.out.println(startTouchTime + "init time");
                    break;
                }

                case MotionEvent.ACTION_MOVE: {

                    final float dx = x - mLastTouchX;
                    final float dy = y - mLastTouchY;
                    if ( y < getHeight()/8f && x > getWidth()/2f){
                        mRenderer.mTranslX += dx * 0.2;
                        //mRenderer.mTranslY += dy * 0.3;

                    }
                    // Calculate the distance moved
                    mRenderer.mAngleZ += dx * 0.4;
                    mRenderer.mAngleX += dy * 0.4;
                    break;
                }
                case MotionEvent.ACTION_UP:

                    endTouchTime = System.currentTimeMillis();
                    deltaTime[1] = endTouchTime;
                    long delta = deltaTime[1] - deltaTime[0];
                    if (delta < 600){
                        synchronized (mRenderer.lock){
                            System.out.println("delta time < 1000: " + delta);
                            System.out.println("first touch coordinates(x,y): " + touches[0] + "  " + touches[1]);
                            if (firstTouch){
                                System.out.println("second touch coordinates(x,y): " + touches[2] + "  " + touches[3]);
                                secondTouch=true;
                            }
                            if (touches[3]!=0){
                                touches[0]=0;touches[1]=0;
                                touches[2]=0;touches[3]=0;
                                firstTouch=false;
                                secondTouch=false;
                            }
                        }
                    }
                    break;
            }
            mLastTouchX = x;
            mLastTouchY = y;

            requestRender();
        }
        return true;
    }

    class PinchZoomListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            System.out.println("scaling attivato");
            float mScaleFactor = detector.getScaleFactor();
            System.out.println(mScaleFactor);
            if( mScaleFactor == 1.0 )
                return false;
            double dz = mScaleFactor -1.0f;
            mRenderer.mScaleFactor += dz;
            requestRender();
            return true;
        }
    }
}
