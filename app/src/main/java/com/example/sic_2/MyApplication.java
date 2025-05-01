package com.example.sic_2;

import android.app.Application;
import android.util.Log;

import com.cloudinary.android.MediaManager;

import java.util.HashMap;
import java.util.Map;

// Create a custom Application class
public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initializeCloudinary();
    }

    private void initializeCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            config.put("api_secret", "RsPtut3zPunRm-8Hwh8zRqQ8uG8");
            MediaManager.init(this, config);
            Log.d("Cloudinary", "Initialized successfully");
        } catch (Exception e) {
            Log.e("Cloudinary", "Initialization failed", e);
        }
    }
}