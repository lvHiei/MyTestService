package com.lvhiei.mytestservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public class TService1 extends Service {
    private final String TAG = this.getClass().getName();
    private IBinder mTestBinderInterface = new IMyBinderInterface.Stub() {
        @Override
        public IBinder getBinder() throws RemoteException {
            return mAddBinder;
        }
    };

    private IBinder mAddBinder = new IAddBinder.Stub() {
        @Override
        public int add(int x, int y) throws RemoteException {
            Log.i(TAG, "add pid is " + Process.myPid());
            return x + y;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.i(TAG, "onBind pid " + Process.myPid());
        return mTestBinderInterface;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate pid " + Process.myPid());
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand pid " + Process.myPid());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind pid " + Process.myPid());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy pid " + Process.myPid());
        super.onDestroy();
    }

}
