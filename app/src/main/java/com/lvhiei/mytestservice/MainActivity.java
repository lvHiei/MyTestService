package com.lvhiei.mytestservice;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private MyLog log = new MyLog(this.getClass().getName());
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private IAddBinder mBinder = null;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log.i( "onServiceConnected pid " + android.os.Process.myPid());
            IMyBinderInterface binder = IMyBinderInterface.Stub.asInterface(service);
            try {
                mBinder = IAddBinder.Stub.asInterface(binder.getBinder());
                int r = mBinder.add(5, 15);
                log.e( "onServiceConnected: r is " + r);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log.i( "onServiceDisconnected pid " + android.os.Process.myPid());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());

        findViewById(R.id.btn_to_render).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()){
                    case R.id.btn_to_render:
                        gotoRenderActivity();
                        break;
                }
            }
        });

        startservice();
    }

    private void gotoRenderActivity() {
        Intent intent = new Intent(this, RenderActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopservice();
    }

    private void startservice(){
        Intent intent = new Intent(this, TService1.class);
        this.bindService(intent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void stopservice(){
        this.unbindService(mServiceConnection);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
