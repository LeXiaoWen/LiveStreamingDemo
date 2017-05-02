package com.leo.livestreamingdemo.api;

import android.app.Application;

import com.qiniu.pili.droid.streaming.StreamingEnv;

/**
 * Created by Leo on 2017/5/2.
 */

public class MyApp extends Application{
    @Override
    public void onCreate() {
        super.onCreate();
        StreamingEnv.init(getApplicationContext());
    }
}
