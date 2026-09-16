package com.winlator.xenvironment;

import android.content.Context;

import androidx.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.winlator.core.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class RootFS {
    public static final String USER = "xuser";
    public static final String HOME_PATH = "/home/" + USER;
    public static final String USER_CACHE_PATH = "/home/" + USER + "/.cache";
    public static final String USER_CONFIG_PATH = "/home/" + USER + "/.config";
    private final File rootDir;

    private RootFS(File rootDir) {
        this.rootDir = rootDir;
    }

    public static RootFS find(Context context) {
        // Custom rootfs/chroot directory set in Settings (preferred, no extraction needed)
        String customPath = PreferenceManager.getDefaultSharedPreferences(context).getString("rootfs_path", "");
        if (customPath != null && !customPath.isEmpty()) {
            File customDir = new File(customPath);
            if (customDir.isDirectory() && isValidRootDir(customDir)) return new RootFS(customDir);
        }

        File legacyDir = new File(context.getFilesDir(), "imagefs");
        File rootDir = new File(context.getFilesDir(), "rootfs");
        if (legacyDir.isDirectory()) legacyDir.renameTo(rootDir);
        return new RootFS(rootDir);
    }


    public static RootFS of(File rootDir) {
        return new RootFS(rootDir);
    }
    public File getRootDir() {
        return rootDir;
    }

    public boolean isValid() {
        if (rootDir.isDirectory() &&
            (new File(rootDir, "bin/bash").exists() || new File(rootDir, "etc/os-release").exists())) {
            return true;
        }
        return rootDir.isDirectory() && getRFSVersionFile().exists();
    }

    public static boolean isValidRootDir(File rootDir) {
        return rootDir != null && rootDir.isDirectory() &&
            (new File(rootDir, "bin/bash").exists() ||
             new File(rootDir, "etc/os-release").exists() ||
             getRFSVersionFile(rootDir).exists());
    }

    public int getVersion() {
        File rfsVersionFile = getRFSVersionFile();
        if (!rfsVersionFile.isFile()) return 0;
        try {
            java.util.ArrayList<String> lines = FileUtils.readLines(rfsVersionFile, true);
            if (lines.isEmpty()) return 0;
            return Integer.parseInt(lines.get(0).trim());
        } catch (NumberFormatException e) {
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float) getVersion());
    }

    public void createRFSVersionFile(int version) {
        createVersionFile(rootDir, version);
    }

    public static void createVersionFile(File rootDir, int version) {
        getImageInfoDir(rootDir).mkdirs();
        File file = getRFSVersionFile(rootDir);
        try {
            file.createNewFile();
            FileUtils.writeString(file, String.valueOf(version));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getTmpDir() {
        return new File(rootDir, "/tmp");
    }

    public File getLibDir() {
        return new File(rootDir, "/usr/lib");
    }

    public File getBinDir() {
        return new File(rootDir, "/usr/bin");
    }

    public File getEtcDir() {
        return new File(rootDir, "/etc");
    }

    public File getLinuxHomeDir() {
        return new File(rootDir, HOME_PATH);
    }

    @NonNull
    @Override
    public String toString() {
        return rootDir.getPath();
    }

    private File getImageInfoDir() {
        return getImageInfoDir(rootDir);
    }

    public File getRFSVersionFile() {
        return getRFSVersionFile(rootDir);
    }

    private static File getImageInfoDir(File rootDir) {
        return new File(rootDir, ".linuxx");
    }

    public static File getRFSVersionFile(File rootDir) {
        return new File(getImageInfoDir(rootDir), ".rfs_version");
    }
}
