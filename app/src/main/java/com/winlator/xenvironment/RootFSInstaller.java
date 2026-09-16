package com.winlator.xenvironment;

import android.content.Context;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.RootAccessHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;

public abstract class RootFSInstaller {
    public interface ProgressCallback {
        void onProgress(int percent, String currentPath);
    }

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
        return extractTarGz(tarGzFile, destDir, null);
    }

    public static boolean extractTarGz(File tarGzFile, File destDir, ProgressCallback callback) {
        return extractArchive(tarGzFile, destDir, callback);
    }

    public static boolean extractArchive(File archiveFile, File destDir, ProgressCallback callback) {
        try {
            long totalBytes = 0;
            try (ArchiveInputStream ais = openArchive(archiveFile)) {
                ArchiveEntry entry;
                while ((entry = ais.getNextEntry()) != null) {
                    totalBytes += entry.getSize();
                }
            }

            long currentBytes = 0;
            try (ArchiveInputStream ais = openArchive(archiveFile)) {
                ArchiveEntry entry;
                byte[] buffer = new byte[8192];
                int lastPercent = -1;

                while ((entry = ais.getNextEntry()) != null) {
                    File outFile = new File(destDir, entry.getName());

                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else {
                        outFile.getParentFile().mkdirs();
                        try (FileOutputStream fos = new FileOutputStream(outFile)) {
                            int len;
                            while ((len = ais.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                                currentBytes += len;
                                if (callback != null && totalBytes > 0) {
                                    int percent = (int)(currentBytes * 100 / totalBytes);
                                    if (percent != lastPercent) {
                                        lastPercent = percent;
                                        callback.onProgress(percent, entry.getName());
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static ArchiveInputStream openArchive(File archiveFile) throws IOException {
        String name = archiveFile.getName().toLowerCase();
        FileInputStream fis = new FileInputStream(archiveFile);

        if (name.endsWith(".zip") || isZip(fis)) {
            return new ZipArchiveInputStream(fis);
        }

        if (name.endsWith(".tar.gz") || name.endsWith(".tgz") || isGzip(fis)) {
            return new TarArchiveInputStream(new java.util.zip.GZIPInputStream(fis));
        }
        if (name.endsWith(".tar.xz") || name.endsWith(".txz") || name.endsWith(".xz") || isXz(fis)) {
            return new TarArchiveInputStream(new XZCompressorInputStream(fis));
        }
        if (name.endsWith(".tar.zst") || name.endsWith(".tzst")) {
            return new TarArchiveInputStream(new ZstdCompressorInputStream(fis));
        }
        return new TarArchiveInputStream(fis);
    }

    private static boolean isGzip(InputStream in) throws IOException {
        try {
            if (!in.markSupported()) return false;
            in.mark(2);
            boolean ok = in.read() == 0x1f && in.read() == 0x8b;
            in.reset();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isXz(InputStream in) throws IOException {
        try {
            if (!in.markSupported()) return false;
            in.mark(8);
            boolean ok = (in.read() == 0xfd) && (in.read() == 0x37) && (in.read() == 0x7a) && (in.read() == 0x58) &&
                         (in.read() == 0x5a) && (in.read() == 0x00);
            in.reset();
            return ok;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isZip(InputStream in) throws IOException {
        try {
            if (!in.markSupported()) return false;
            in.mark(4);
            boolean ok = in.read() == 0x50 && in.read() == 0x4b && (in.read() == 0x03 || in.read() == 0x05);
            in.reset();
            return ok;
        } catch (Exception e) {
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
        return importFromUri(context, uri, targetRootDir, null);
    }

    public static boolean importFromUri(final Context context, final android.net.Uri uri, final File targetRootDir, final ProgressCallback callback) {
        try {
            long fileSize = 0;
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex);
                }
            }

            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return false;

            String name = getUriName(context, uri);
            File tempFile = new File(context.getCacheDir(), name != null ? name : FILENAME);
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[8192];
            int len;
            long total = 0;
            if (callback != null) callback.onProgress(0, "Copying rootfs file...");
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
                total += len;
                if (callback != null) {
                    int percent = fileSize > 0 ? (int)Math.min(90, total * 90 / fileSize) : 0;
                    callback.onProgress(percent, "Copying rootfs file...");
                }
            }
            fos.close();
            is.close();

            clearRootDir(targetRootDir);
            boolean success = extractArchive(tempFile, targetRootDir, (p, path) -> {
                if (callback != null) callback.onProgress(90 + p / 10, path);
            });
            tempFile.delete();
            if (success) {
                setupHomeDirectory(targetRootDir);
                RootFS rootFS = RootFS.find(context);
                if (!rootFS.isValid()) rootFS.createRFSVersionFile(LATEST_VERSION);
            }
            return success;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getUriName(Context context, android.net.Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) return cursor.getString(nameIndex);
            }
        } catch (Exception ignored) {}
        String lastSegment = uri.getLastPathSegment();
        return lastSegment != null ? lastSegment : null;
    }

    public static boolean prepareSocketDirs(File rootDir) {
        String[] dirs = {
            "/tmp/.X11-unix", "/tmp/.virgl", "/tmp/.sound",
            "/tmp/.sysvshm", "/tmp/.vortek", "/tmp/shm"
        };
        StringBuilder suCmd = new StringBuilder();
        boolean directOk = true;
        for (String dir : dirs) {
            File d = new File(rootDir, dir);
            directOk &= (d.mkdirs() || d.isDirectory());
            directOk &= d.setReadable(true, false);
            directOk &= d.setWritable(true, false);
            directOk &= d.setExecutable(true, false);
            if (suCmd.length() > 0) suCmd.append(" && ");
            suCmd.append("mkdir -p ").append(d.getAbsolutePath());
            suCmd.append(" && chmod 777 ").append(d.getAbsolutePath());
        }
        if (directOk) return true;
        if (!RootAccessHelper.isRootGranted()) return false;
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", suCmd.toString()});
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
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
