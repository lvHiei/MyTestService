package com.lvhiei.mytestservice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class RenderActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render);

        Log.i(TAG, "RenderActivity onCreate pid " + android.os.Process.myPid());
    }
}
