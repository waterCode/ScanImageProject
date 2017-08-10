package com.meitu.scanimageproject;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by zmc on 2017/8/10.
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}
