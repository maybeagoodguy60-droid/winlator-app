package com.winlator.core;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;

public class RootAccessHelper {
    private static final String PREFS_NAME = "linux_x_prefs";
    private static final String KEY_ROOT_GRANTED = "root_granted";
    private static final String KEY_WELCOME_SHOWN = "welcome_shown";
    private static Boolean rootAvailable = null;

    public static boolean isRootAvailable() {
        if (rootAvailable != null) return rootAvailable;
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/data/local/xbin/su", "/data/local/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) {
                rootAvailable = true;
                return true;
            }
        }
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", "su"});
            int exitCode = process.waitFor();
            rootAvailable = (exitCode == 0);
        } catch (Exception e) {
            rootAvailable = false;
        }
        return rootAvailable;
    }

    public static boolean isRootGranted() {
        if (!isRootAvailable()) return false;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();
            return output != null && output.contains("uid=0");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isRootGrantedCached(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ROOT_GRANTED, false);
    }

    public static void setRootGranted(Context context, boolean granted) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ROOT_GRANTED, granted).apply();
    }

    public static boolean isWelcomeShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_WELCOME_SHOWN, false);
    }

    public static void setWelcomeShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply();
    }
}
