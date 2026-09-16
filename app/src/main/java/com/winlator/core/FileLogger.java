package com.winlator.core;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FileLogger {
    private static final String TAG = "FileLogger";
    private static final String FILE_NAME = "winlator_debug.log";
    private static volatile Thread dumper = null;

    public static synchronized void start(Context context) {
        try {
            if (dumper != null && dumper.isAlive()) return;
            File target = pickTarget(context);
            if (target == null) {
                Log.w(TAG, "no writable storage dir available");
                return;
            }
            dumper = new Thread(() -> dump(target), "file-logger");
            dumper.setDaemon(true);
            dumper.start();
            Log.i(TAG, "logging started -> " + target.getAbsolutePath());
        }
        catch (Throwable t) {
            Log.w(TAG, "start failed", t);
        }
    }

    private static File pickTarget(Context context) {
        File[] candidates = {
                new File(Environment.getExternalStorageDirectory(), FILE_NAME),
                new File(context.getExternalFilesDir(null), FILE_NAME),
                new File(context.getFilesDir(), FILE_NAME)
        };
        for (File file : candidates) {
            if (file == null) continue;
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();
                FileOutputStream fos = new FileOutputStream(file, true);
                fos.close();
                return file;
            }
            catch (IOException ignore) {
            }
        }
        return null;
    }

    private static void dump(File target) {
        try {
            Process process = new ProcessBuilder("logcat", "--pid=" + android.os.Process.myPid(), "-v", "time")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 Writer writer = new FileWriter(target, true)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write("[" + timestamp() + "] " + line + "\n");
                    writer.flush();
                }
            }
            process.destroy();
        }
        catch (Throwable t) {
            Log.w(TAG, "dumper stopped", t);
        }
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
    }
}