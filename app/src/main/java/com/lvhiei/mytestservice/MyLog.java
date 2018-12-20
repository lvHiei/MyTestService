package com.lvhiei.mytestservice;

import android.util.Log;

/**
 * Created by mj on 18-12-20.
 */

public class MyLog {
    private String mTag;
    public MyLog(String tag){
        mTag = tag;
    }


    public void d(String msg, Object... args){
        Log.d(mTag, String.format(msg, args));
    }

    public void i(String msg, Object... args){
        Log.i(mTag, String.format(msg, args));
    }

    public void w(String msg, Object... args){
        Log.w(mTag, String.format(msg, args));
    }

    public void e(String msg, Object... args){
        Log.e(mTag, String.format(msg, args));
    }
}
