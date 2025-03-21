package com.example.sic_2;

import android.app.Application;
import android.content.Context;

public class MyApplication extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
        // Perform initialization tasks here
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}