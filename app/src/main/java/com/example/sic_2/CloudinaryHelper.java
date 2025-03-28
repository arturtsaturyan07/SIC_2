// CloudinaryHelper.java
package com.example.sic_2;

import android.content.Context;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {
    private static boolean initialized = false;

    public static synchronized void initialize(Context context) {
        if (!initialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "disiijbpp");
            config.put("api_key", "265226997838638");
            // Note: Never include api_secret in client-side code

            MediaManager.init(context, config);
            initialized = true;
        }
    }
}