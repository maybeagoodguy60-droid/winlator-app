package com.winlator.linux;

import android.content.Context;

import com.winlator.core.RootAccessHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LaunchValidator {
    public static boolean hasRootfs(File rootDir) {
        if (rootDir == null || !rootDir.isDirectory()) return false;
        return new File(rootDir, "bin/bash").exists()
            || new File(rootDir, "etc/os-release").exists()
            || new File(rootDir, "usr").isDirectory();
    }

    public static List<String> validate(final Context context, final File rootDir, final int launchMode) {
        final List<String> problems = new ArrayList<>();

        if (rootDir == null || !rootDir.isDirectory()) {
            String path = rootDir != null ? rootDir.getAbsolutePath() : "null";
            problems.add(context.getString(com.winlator.R.string.launch_error_rootfs_missing, path));
            return problems;
        }

        if (!new File(rootDir, "bin/bash").exists() && new File(rootDir, "etc/os-release").exists()) {
            problems.add(context.getString(com.winlator.R.string.launch_error_rootfs_incomplete));
        }

        boolean rootGranted = RootAccessHelper.isRootGranted();
        boolean willChroot;
        switch (launchMode) {
            case LinuxContainer.LAUNCH_MODE_CHROOT:
                willChroot = rootGranted;
                break;
            case LinuxContainer.LAUNCH_MODE_PROOT:
                willChroot = false;
                break;
            default:
                willChroot = rootGranted && new File(rootDir, "/bin/bash").exists();
        }

        if (!willChroot) {
            File proot = new File(ProotLauncher.resolveProotPath(context));
            if (!proot.exists()) {
                problems.add(context.getString(com.winlator.R.string.launch_error_proot_missing, proot.getAbsolutePath()));
            }
            else if (!proot.canExecute()) {
                problems.add(context.getString(com.winlator.R.string.launch_error_proot_not_exec, proot.getAbsolutePath()));
            }
        }

        if (problems.isEmpty()) {
            File tmpDir = new File(rootDir, "/tmp");
            if (!tmpDir.isDirectory() && !tmpDir.mkdirs()) {
                problems.add(context.getString(com.winlator.R.string.launch_error_tmp_not_writable, rootDir.getAbsolutePath()));
            }
        }

        return problems;
    }

    public static String join(List<String> problems) {
        if (problems == null || problems.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String problem : problems) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("• ").append(problem);
        }
        return sb.toString();
    }
}