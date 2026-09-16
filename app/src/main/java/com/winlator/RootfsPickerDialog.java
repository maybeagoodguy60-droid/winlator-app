package com.winlator.contentdialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.winlator.R;

public class RootfsPickerDialog {
    public interface Callback {
        void onDownload();
        void onImportFromFile();
        void onUseExistingPath(String path);
    }

    public static void show(Context context, Callback callback) {
        View view = LayoutInflater.from(context).inflate(R.layout.rootfs_picker_dialog, null, false);
        final EditText etRootfsPath = view.findViewById(R.id.ETRootfsPath);

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(R.string.select_rootfs_source)
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .create();

        view.findViewById(R.id.BTDownloadFromRepo).setOnClickListener((v) -> {
            dialog.dismiss();
            callback.onDownload();
        });

        view.findViewById(R.id.BTImportFromFile).setOnClickListener((v) -> {
            dialog.dismiss();
            callback.onImportFromFile();
        });

        view.findViewById(R.id.BTUseExistingPath).setOnClickListener((v) -> {
            String path = etRootfsPath.getText().toString().trim();
            if (path.isEmpty()) {
                etRootfsPath.setError(context.getString(R.string.rootfs_select_hint));
                return;
            }
            dialog.dismiss();
            callback.onUseExistingPath(path);
        });

        dialog.show();
    }
}