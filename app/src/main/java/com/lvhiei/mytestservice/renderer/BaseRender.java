package com.lvhiei.mytestservice.renderer;

import android.opengl.GLSurfaceView;

import com.lvhiei.mytestservice.glutil.OpenGLUtils;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by mj on 18-12-20.
 */

public abstract class BaseRender implements GLSurfaceView.Renderer {

    public abstract void setSurfaceView(GLSurfaceView surfaceView);

    public abstract void onResume();

    public abstract void onPause();

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {

    }

    @Override
    public void onDrawFrame(GL10 gl) {

    }
}
