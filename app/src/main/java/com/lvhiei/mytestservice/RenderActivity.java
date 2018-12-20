package com.lvhiei.mytestservice;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.WindowManager;

import com.lvhiei.mytestservice.renderer.MyRenderer;

public class RenderActivity extends Activity {
    private MyLog log = new MyLog(this.getClass().getName());
    private GLSurfaceView mSurfaceView;
    private MyRenderer mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mSurfaceView = findViewById(R.id.ts_glsurfaceView);
        mRenderer = new MyRenderer();
        mRenderer.setSurfaceView(mSurfaceView);
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(mRenderer);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        log.i("RenderActivity onCreate pid " + android.os.Process.myPid());
    }

    @Override
    protected void onResume() {
        log.i("onResume");
        super.onResume();
        mRenderer.onResume();
    }

    @Override
    protected void onPause() {
        log.i("onPause");
        super.onPause();
        mRenderer.onPause();
    }

    @Override
    protected void onDestroy() {
        log.i("onDestroy");
        super.onDestroy();
    }
}
