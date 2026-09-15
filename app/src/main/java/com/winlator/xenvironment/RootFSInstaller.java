package com.winlator.xenvironment;

import android.content.Context;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

public abstract class RootFSInstaller {
    public static final byte LATEST_VERSION = 1;
    public static final String FILENAME = "rootfs.tar.gz";

    public static void install(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        RootFS rootFS = RootFS.find(activity);
        final File rootDir = rootFS.getRootDir();

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(rootDir);

            File rootfsFile = new File(activity.getFilesDir(), FILENAME);
            boolean success = false;

            if (rootfsFile.exists()) {
                success = extractTarGz(rootfsFile, rootDir);
            } else {
                // Try assets
                success = extractFromAssets(activity, rootDir);
            }

            if (success) {
                setupHomeDirectory(rootDir);
                rootFS.createRFSVersionFile(LATEST_VERSION);
            } else {
                AppUtils.showToast(activity, R.string.unable_to_install_system_files);
            }

            dialog.closeOnUiThread();
        });
    }

    public static void installIfNeeded(final MainActivity activity) {
        RootFS rootFS = RootFS.find(activity);
        if (!rootFS.isValid() || rootFS.getVersion() < LATEST_VERSION) install(activity);
    }

    public static boolean extractTarGz(File tarGzFile, File destDir) {
        try {
            FileInputStream fis = new FileInputStream(tarGzFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            TarArchiveInputStream tis = new TarArchiveInputStream(gis);

            TarArchiveEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = tis.getNextTarEntry()) != null) {
                File outFile = new File(destDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    FileOutputStream fos = new FileOutputStream(outFile);
                    int len;
                    while ((len = tis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
            }

            tis.close();
            gis.close();
            fis.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean extractFromAssets(Context context, File destDir) {
        try {
            InputStream is = context.getAssets().open(FILENAME);
            File tempFile = new File(context.getCacheDir(), FILENAME);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();

            boolean success = extractTarGz(tempFile, destDir);
            tempFile.delete();
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void setupHomeDirectory(File rootDir) {
        File homeDir = new File(rootDir, "/home/xuser");
        if (!homeDir.exists()) {
            homeDir.mkdirs();
        }

        // Create .bashrc if it doesn't exist
        File bashrc = new File(homeDir, ".bashrc");
        if (!bashrc.exists()) {
            try {
                FileOutputStream fos = new FileOutputStream(bashrc);
                fos.write("export DISPLAY=:0\n".getBytes());
                fos.write("export PULSE_SERVER=/tmp/.sound/PS0\n".getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create tmp directory and socket dirs needed by the Android services
        File tmpDir = new File(rootDir, "/tmp");
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        new File(rootDir, "/tmp/.X11-unix").mkdirs();
        new File(rootDir, "/tmp/.virgl").mkdirs();
        new File(rootDir, "/tmp/.sound").mkdirs();
        new File(rootDir, "/tmp/.sysvshm").mkdirs();
    }


    public static void installToDir(final Context context, final File targetRootDir) {
        final File rootfsFile = new File(context.getFilesDir(), FILENAME);
        if (!rootfsFile.exists()) return;
        
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(targetRootDir);
            boolean success = extractTarGz(rootfsFile, targetRootDir);
            if (success) {
                setupHomeDirectory(targetRootDir);
            }
        });
    }

    public static boolean importFromUri(final Context context, final android.net.Uri uri, final File targetRootDir) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return false;

            File tempFile = new File(context.getCacheDir(), FILENAME);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();

            clearRootDir(targetRootDir);
            boolean success = extractTarGz(tempFile, targetRootDir);
            tempFile.delete();
            if (success) {
                setupHomeDirectory(targetRootDir);
            }
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    private static void clearRootDir(File rootDir) {
        if (rootDir.isDirectory()) {
            File[] files = rootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    if (name.equals("home") || name.equals("opt")) continue;
                    FileUtils.delete(file);
                }
            }
        } else {
            rootDir.mkdirs();
        }
    }
}
