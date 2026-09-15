package com.winlator.linux;

import android.content.Context;

import com.winlator.core.AppUtils;
import com.winlator.core.FileUtils;
import com.winlator.core.HttpUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RootfsDownloader {
    public static final String REPO_BASE_URL = "https://github.com/linux-x/rootfs-repo/raw/main";
    public static final String INDEX_URL = REPO_BASE_URL + "/index.json";
    
    public interface DownloadCallback {
        void onProgress(int progress);
        void onComplete(boolean success, String message);
    }

    public static void downloadRootfs(Context context, String rootfsId, File destinationDir, DownloadCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String indexJson = HttpUtils.get(INDEX_URL);
                JSONArray rootfsArray = new JSONArray(indexJson);
                
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
                HttpUtils.download(downloadUrl, outputFile, (bytesRead, totalBytes) -> {
                    int progress = (int) ((bytesRead * 100) / totalBytes);
                    callback.onProgress(progress);
                });
                
                callback.onComplete(true, "Download complete");
            } catch (Exception e) {
                callback.onComplete(false, "Download failed: " + e.getMessage());
            }
        });
    }

    public static void listAvailableRootfs(DownloadCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String indexJson = HttpUtils.get(INDEX_URL);
                callback.onComplete(true, indexJson);
            } catch (Exception e) {
                callback.onComplete(false, "Failed to fetch rootfs list: " + e.getMessage());
            }
        });
    }
}
