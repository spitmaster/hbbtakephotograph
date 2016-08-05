package com.example.zyj.hbbtakephotograph;

import android.app.Application;
import android.content.Context;

/**
 * Created by Administrator on 2016/8/2.
 */
public class MyApplication extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }
}
