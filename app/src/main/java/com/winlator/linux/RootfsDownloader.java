package com.winlator.linux;

import android.content.Context;

import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class RootfsDownloader {
    private static final String INDEX_URL = "https://raw.githubusercontent.com/maybeagoodguy60-droid/linux-x-rootfs/main/index.json";

    public interface Callback {
        void onProgress(int progress);
        void onComplete(boolean success, String message);
    }

    public static void downloadRootfs(Context context, String rootfsId, String destinationDir, Callback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                URL url = new URL(INDEX_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                InputStream is = conn.getInputStream();
                byte[] data = new byte[conn.getContentLength() > 0 ? conn.getContentLength() : 8192];
                StringBuilder sb = new StringBuilder();
                int read;
                while ((read = is.read(data)) != -1) sb.append(new String(data, 0, read));
                is.close();

                JSONArray rootfsArray = new JSONArray(sb.toString());

                JSONObject selectedRootfs = null;
                for (int i = 0; i < rootfsArray.length(); i++) {
                    JSONObject rootfs = rootfsArray.getJSONObject(i);
                    if (rootfs.getString("id").equals(rootfsId)) {
                        selectedRootfs = rootfs;
                        break;
                    }
                }

                if (selectedRootfs == null) {
                    callback.onComplete(false, "Rootfs not found in repository");
                    return;
                }

                String downloadUrl = selectedRootfs.getString("url");
                String filename = selectedRootfs.getString("filename");

                File outputFile = new File(destinationDir, filename);
                URL dlUrl = new URL(downloadUrl);
                HttpURLConnection dlConn = (HttpURLConnection) dlUrl.openConnection();
                dlConn.setConnectTimeout(15000);
                dlConn.setReadTimeout(60000);
                InputStream dlIs = dlConn.getInputStream();
                int totalBytes = dlConn.getContentLength();
                int bytesRead = 0;

                java.io.FileOutputStream fos = new java.io.FileOutputStream(outputFile);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = dlIs.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                    bytesRead += len;
                    if (totalBytes > 0) {
                        int progress = (int) ((bytesRead * 100) / totalBytes);
                        callback.onProgress(progress);
                    }
                }
                fos.close();
                dlIs.close();

                callback.onComplete(true, outputFile.getAbsolutePath());
            } catch (Exception e) {
                callback.onComplete(false, e.getMessage());
            }
        });
        executor.shutdown();
    }
}
