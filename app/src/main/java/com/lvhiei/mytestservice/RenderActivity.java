package com.lvhiei.mytestservice;

import android.app.Activity;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.lvhiei.mytestservice.renderer.BaseRender;
import com.lvhiei.mytestservice.renderer.CameraRenderer;

public class RenderActivity extends Activity {
    private MyLog log = new MyLog(this.getClass().getName());
    private GLSurfaceView mSurfaceView;
    private BaseRender mRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        findViewById(R.id.btn_backto_main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_backto_main:
                        gotoMainActivity();
                        break;
                }
            }
        });

        mSurfaceView = findViewById(R.id.ts_glsurfaceView);
        createRender();
        mSurfaceView.setEGLContextClientVersion(2);
        mSurfaceView.setRenderer(mRenderer);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        log.i("RenderActivity onCreate pid " + android.os.Process.myPid());
    }

    private void gotoMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void createRender(){
        mRenderer = new CameraRenderer();
        mRenderer.setSurfaceView(mSurfaceView);
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
