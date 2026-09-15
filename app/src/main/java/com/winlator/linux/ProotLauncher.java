package com.winlator.linux;

import android.content.Context;

import com.winlator.core.RootAccessHelper;
import com.winlator.xconnector.UnixSocketConfig;

import java.io.File;

public class ProotLauncher {
    public static final String PROOT_BINARY = "proot";
    public static final String PROOT_PATH = "/data/data/com.winlator/files/linuxx/proot";

    public static String buildCommand(Context context, File rootDir, String launchCommand, int launchMode) {
        String prootPath = resolveProotPath(context);
        boolean rootGranted = RootAccessHelper.isRootGranted();

        boolean useChroot;
        switch (launchMode) {
            case LinuxContainer.LAUNCH_MODE_CHROOT:
                useChroot = rootGranted;
                break;
            case LinuxContainer.LAUNCH_MODE_PROOT:
                useChroot = false;
                break;
            default:
                useChroot = rootGranted && new File(rootDir, "/bin/bash").exists();
        }

        if (useChroot) return buildChrootCommand(rootDir, launchCommand);
        return buildProotCommand(prootPath, rootDir, launchCommand);
    }

    public static String buildProotCommand(String prootPath, File rootDir, String launchCommand) {
        StringBuilder cmd = new StringBuilder();
        cmd.append(prootPath);
        cmd.append(" -0");                              // fake root
        cmd.append(" -r ").append(rootDir.getAbsolutePath()); // rootfs path
        cmd.append(" -b /proc");                         // bind /proc
        cmd.append(" -b /sys");                          // bind /sys
        cmd.append(" -b /dev");                          // bind /dev
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
        String r = rootDir.getAbsolutePath();
        StringBuilder cmd = new StringBuilder();
        cmd.append("su -c '");
        cmd.append("export DISPLAY=:0 HOME=/home/xuser USER=xuser TMPDIR=/tmp ");
        cmd.append("PATH=/usr/local/bin:/usr/bin:/bin LD_LIBRARY_PATH=/usr/lib ");
        cmd.append("ANDROID_SYSVSHM_SERVER=").append(UnixSocketConfig.SYSVSHM_SERVER_PATH).append(' ');
        cmd.append("ANDROID_ALSA_SERVER=").append(UnixSocketConfig.ALSA_SERVER_PATH).append(' ');
        cmd.append("PULSE_SERVER=").append(UnixSocketConfig.PULSE_SERVER_PATH).append(' ');
        cmd.append("VIRGL_SERVER_PATH=").append(UnixSocketConfig.VIRGL_SERVER_PATH).append("; ");
        cmd.append("mkdir -p ").append(r).append("/proc ").append(r).append("/sys ").append(r).append("/dev ");
        cmd.append("mkdir -p ").append(r).append("/tmp/.X11-unix ").append(r).append("/tmp/.virgl ").append(r).append("/tmp/.sound ").append(r).append("/tmp/.sysvshm ").append(r).append("/tmp/.vortek ").append(r).append("/tmp/shm; ");
        cmd.append("chmod 777 ").append(r).append("/tmp/.X11-unix ").append(r).append("/tmp/.virgl ").append(r).append("/tmp/.sound ").append(r).append("/tmp/.sysvshm ").append(r).append("/tmp/.vortek ").append(r).append("/tmp/shm; ");
        cmd.append("umount ").append(r).append("/proc 2>/dev/null; umount ").append(r).append("/sys 2>/dev/null; umount ").append(r).append("/dev 2>/dev/null; ");
        cmd.append("mount --bind /proc ").append(r).append("/proc; ");
        cmd.append("mount --bind /sys ").append(r).append("/sys; ");
        cmd.append("mount --bind /dev ").append(r).append("/dev; ");
        cmd.append("chroot ").append(r);

        if (launchCommand != null && !launchCommand.isEmpty()) {
            cmd.append(" /bin/bash -c \"").append(launchCommand).append("\"");
        } else {
            cmd.append(" /bin/bash --login");
        }

        cmd.append(" ; umount ").append(r).append("/proc");
        cmd.append(" ; umount ").append(r).append("/sys");
        cmd.append(" ; umount ").append(r).append("/dev");
        cmd.append("'");

        return cmd.toString();
    }

    public static String resolveProotPath(Context context) {
        // Check nativeLibraryDir (where jniLibs/arm64-v8a/ files are extracted)
        File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
        File prootInNativeLib = new File(nativeLibDir, "proot");
        if (prootInNativeLib.exists()) return prootInNativeLib.getAbsolutePath();

        // Check our custom path
        File prootInFiles = new File(PROOT_PATH);
        if (prootInFiles.exists()) return prootInFiles.getAbsolutePath();

        // Fallback
        return PROOT_PATH;
    }
}
