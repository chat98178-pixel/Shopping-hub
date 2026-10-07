package com.example.shopping;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.multidex.MultiDexApplication;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoppingApp extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        // Apply saved theme
        boolean isDark = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("isDarkMode", false);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(isDark ? 
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : 
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);

        // Ensure unique Device ID for security
        String deviceID = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("deviceID", "");
        if (deviceID.isEmpty()) {
            deviceID = UUID.randomUUID().toString();
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().putString("deviceID", deviceID).apply();
        }
        
        // Global Crash Handler to see why it closes
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            new Thread(() -> {
                Looper.prepare();
                Toast.makeText(getApplicationContext(), "Fatal Error: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                Looper.loop();
            }).start();
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        });

        try {
            // Initialize Cloudinary
            Map config = new HashMap();
            config.put("cloud_name", "fhbabwba"); // Updated with your Cloud Name
            MediaManager.init(this, config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}