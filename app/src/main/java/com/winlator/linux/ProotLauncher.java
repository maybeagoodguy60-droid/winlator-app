package com.winlator.linux;

import android.content.Context;

import com.winlator.core.RootAccessHelper;

import java.io.File;

public class ProotLauncher {
    public static final String PROOT_BINARY = "proot";
    public static final String PROOT_PATH = "/data/data/com.winlator/files/linuxx/proot";

    public static String buildCommand(File rootDir, String launchCommand, boolean useChroot) {
        if (useChroot && RootAccessHelper.isRootGranted()) {
            return buildChrootCommand(rootDir, launchCommand);
        }
        return buildProotCommand(rootDir, launchCommand);
    }

    public static String buildProotCommand(File rootDir, String launchCommand) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(PROOT_PATH);
        cmd.append(" -0");                              // fake root
        cmd.append(" -r ").append(rootDir.getAbsolutePath()); // rootfs path
        cmd.append(" -b /proc");                         // bind /proc
        cmd.append(" -b /sys");                          // bind /sys
        cmd.append(" -b /dev");                          // bind /dev
        cmd.append(" -b /tmp");                          // bind /tmp
        cmd.append(" -b ").append(rootDir.getAbsolutePath()).append("/tmp/shm:/dev/shm"); // shared memory
        cmd.append(" -w /home/xuser");                   // working directory
        cmd.append(" -H");                               // fake hostname
        cmd.append(" --link2symlink");                   // hardlink -> symlink
        cmd.append(" --kill-on-exit");

        if (launchCommand != null && !launchCommand.isEmpty()) {
            cmd.append(" /bin/bash -c '").append(launchCommand).append("'");
        } else {
            cmd.append(" /bin/bash --login");
        }

        return cmd.toString();
    }

    public static String buildChrootCommand(File rootDir, String launchCommand) {
        StringBuilder cmd = new StringBuilder();
        cmd.append("su -c '");
        cmd.append("mount --bind /proc ").append(rootDir).append("/proc && ");
        cmd.append("mount --bind /sys ").append(rootDir).append("/sys && ");
        cmd.append("mount --bind /dev ").append(rootDir).append("/dev && ");
        cmd.append("chroot ").append(rootDir);

        if (launchCommand != null && !launchCommand.isEmpty()) {
            cmd.append(" /bin/bash -c \"").append(launchCommand).append("\"");
        } else {
            cmd.append(" /bin/bash --login");
        }

        cmd.append(" && umount ").append(rootDir).append("/proc");
        cmd.append(" && umount ").append(rootDir).append("/sys");
        cmd.append(" && umount ").append(rootDir).append("/dev");
        cmd.append("'");

        return cmd.toString();
    }

    public static boolean isProotAvailable(Context context) {
        File prootFile = new File(PROOT_PATH);
        return prootFile.exists() && prootFile.canExecute();
    }

    public static boolean isChrootAvailable() {
        return RootAccessHelper.isRootGranted();
    }

    public static String getContainerType(boolean useChroot) {
        return useChroot ? "chroot" : "proot";
    }
}
