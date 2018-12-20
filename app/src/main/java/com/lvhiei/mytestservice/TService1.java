package com.lvhiei.mytestservice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

public class TService1 extends Service {
    private MyLog log = new MyLog(this.getClass().getName());
    private IBinder mTestBinderInterface = new IMyBinderInterface.Stub() {
        @Override
        public IBinder getBinder() throws RemoteException {
            return mAddBinder;
        }
    };

    private IBinder mAddBinder = new IAddBinder.Stub() {
        @Override
        public int add(int x, int y) throws RemoteException {
            log.i( "add pid is " + Process.myPid());
            return x + y;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        log.i( "onBind pid " + Process.myPid());
        return mTestBinderInterface;
    }

    @Override
    public void onCreate() {
        log.i( "onCreate pid " + Process.myPid());
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.i( "onStartCommand pid " + Process.myPid());
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log.i( "onUnbind pid " + Process.myPid());
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        log.i( "onDestroy pid " + Process.myPid());
        super.onDestroy();
    }

}
