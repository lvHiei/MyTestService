package com.lvhiei.mytestservice.renderer;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.lvhiei.mytestservice.camera.CameraHelper;
import com.lvhiei.mytestservice.glutil.GLRender;
import com.lvhiei.mytestservice.MyLog;
import com.lvhiei.mytestservice.glutil.OpenGLUtils;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by mj on 18-12-20.
 */

public class MyRenderer implements GLSurfaceView.Renderer {
    private MyLog log = new MyLog(this.getClass().getName());
    private int mCameraTextureId = OpenGLUtils.NO_TEXTURE;
    private GLSurfaceView mSurfaceView;
    private GLRender mGLRender = new GLRender();
    private int mImageWidth = 720;
    private int mImageHeight = 1280;
    private CameraHelper mCameraHelper;
    private SurfaceTexture mSurfaceTexture;
    private int mViewWidth;
    private int mViewHeight;

    public void setSurfaceView(GLSurfaceView surfaceView){
        mSurfaceView = surfaceView;
    }

    public void onResume(){
        mSurfaceView.requestRender();
    }


    public void onPause(){
        mSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraHelper.closeCamera();
                OpenGLUtils.deleteTexture(mCameraTextureId);
                mGLRender.destroy();
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mCameraHelper = new CameraHelper(mSurfaceView.getContext().getApplicationContext());
        mCameraHelper.openCamera();
        if(mCameraTextureId == OpenGLUtils.NO_TEXTURE){
            mCameraTextureId = OpenGLUtils.getExternalOESTextureID();
        }
        mSurfaceTexture = new SurfaceTexture(mCameraTextureId);
        try {
            mCameraHelper.startPreview(mSurfaceTexture, new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    mSurfaceView.requestRender();
                    camera.addCallbackBuffer(data);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        mImageWidth = mCameraHelper.getCameraHeight();
        mImageHeight = mCameraHelper.getCameraWidth();

        mGLRender.init(mImageWidth, mImageHeight);
        mGLRender.adjustTextureBuffer(mCameraHelper.getOrientation(), mCameraHelper.isFrontCamera());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        log.i("onSurfaceChanged %d %d", width, height);
        mViewWidth = width;
        mViewHeight = height;
        GLES20.glViewport(0, 0, width, height);
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        mGLRender.calculateVertexBuffer(width, height, mImageWidth, mImageHeight, true);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        if(mSurfaceTexture != null){
            try {
                mSurfaceTexture.updateTexImage();
            }catch (Exception e){
                return;
            }
        }else{
            return;
        }

        int textureId = mGLRender.preProcess(mCameraTextureId, null);
        if(textureId > 0){
            GLES20.glViewport(0, 0, mViewWidth, mViewHeight);
            mGLRender.onDrawFrame(textureId);
        }
    }
}
