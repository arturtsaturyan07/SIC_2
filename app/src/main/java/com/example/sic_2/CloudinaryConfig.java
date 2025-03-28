package com.example.sic_2;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryConfig {
    private static final String CLOUD_NAME = "disiijbpp";
    private static final String API_KEY = "265226997838638";
    private static final String API_SECRET = "RsPtut3zPunRm-8Hwh8zRqQ8uG8"; // For development only!
    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (!initialized) {
            try {
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", CLOUD_NAME);
                config.put("api_key", API_KEY);
                config.put("api_secret", API_SECRET); // Remove for production!
                MediaManager.init(context, config);
                initialized = true;
            } catch (IllegalStateException e) {
                // Already initialized
                initialized = true;
            }
        }
    }
}