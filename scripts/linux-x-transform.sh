#!/bin/bash
# Linux X Transformation Script
# Transforms Winlator into Linux X
set -e

echo "=== Linux X Transformation ==="

# ============================================
# PHASE 1: REBRAND
# ============================================
echo "[1/8] Rebranding..."

# Update strings.xml - app name and notifications
sed -i 's|<string name="app_name">Winlator</string>|<string name="app_name">Linux X</string>|' app/src/main/res/values/strings.xml
sed -i 's|Keeps Winlator running in the background|Keeps Linux X running in the background|' app/src/main/res/values/strings.xml
sed -i 's|Winlator is running in the background|Linux X is running in the background|' app/src/main/res/values/strings.xml
sed -i 's|open_android_browser_from_wine|open_android_browser_from_linux|' app/src/main/res/values/strings.xml
sed -i 's|use_android_clipboard_on_Wine|use_android_clipboard_on_linux|' app/src/main/res/values/strings.xml

# Add Linux-specific strings before closing </resources>
sed -i 's|</resources>|    <!-- Linux X Strings -->\
    <string name="linux_preset">Linux Preset</string>\
    <string name="rootfs">Root Filesystem</string>\
    <string name="rootfs_path">Rootfs Path</string>\
    <string name="rootfs_type">Rootfs Type</string>\
    <string name="desktop_environment">Desktop Environment</string>\
    <string name="launch_command">Launch Command</string>\
    <string name="cpu_governor">CPU Governor</string>\
    <string name="install_rootfs">Install Rootfs</string>\
    <string name="download_rootfs">Download Rootfs</string>\
    <string name="import_rootfs">Import Rootfs</string>\
    <string name="rootfs_installed">Rootfs installed successfully</string>\
    <string name="no_rootfs_found">No rootfs found</string>\
    <string name="no_desktop_environment">No Desktop Environment</string>\
    <string name="no_de_detected">No desktop environment detected.\\n\\nInstall LXQt + Terminal?</string>\
    <string name="install_lxqt">Install LXQt</string>\
    <string name="terminal_only">Terminal Only</string>\
    <string name="installing_desktop_environment">Installing Desktop Environment...</string>\
    <string name="installing_terminal">Installing Terminal...</string>\
    <string name="debian">Debian</string>\
    <string name="ubuntu">Ubuntu</string>\
    <string name="arch_linux">Arch Linux</string>\
    <string name="alpine_linux">Alpine Linux</string>\
    <string name="fedora">Fedora</string>\
    <string name="custom_rootfs">Custom Rootfs</string>\
    <string name="xfce_desktop">XFCE</string>\
    <string name="lxqt_desktop">LXQt</string>\
    <string name="kde_plasma">KDE Plasma</string>\
    <string name="gnome_desktop">GNOME</string>\
    <string name="mate_desktop">MATE</string>\
    <string name="terminal_desktop">Terminal Only</string>\
    <string name="auto_detect">Auto Detect</string>\
    <string name="select_rootfs_source">Select Rootfs Source</string>\
    <string name="download_from_repo">Download from Linux X Repo</string>\
    <string name="import_from_file">Import from File</string>\
    <string name="use_existing_path">Use Existing Path</string>\
    <string name="linux_configuration">Linux Configuration</string>\
    <string name="start_script">Start Script</string>\
    <string name="advanced_linux_settings">Advanced Linux Settings</string>\
</resources>|' app/src/main/res/values/strings.xml

# ============================================
# PHASE 2: REMOVE WINE/WIN32 FILES
# ============================================
echo "[2/8] Removing Wine/Win32 files..."

# Remove Wine utility files
rm -f app/src/main/java/com/winlator/core/WineInfo.java
rm -f app/src/main/java/com/winlator/core/WineUtils.java
rm -f app/src/main/java/com/winlator/core/WineInstaller.java
rm -f app/src/main/java/com/winlator/core/WineRegistryEditor.java
rm -f app/src/main/java/com/winlator/core/WineStartMenuCreator.java
rm -f app/src/main/java/com/winlator/core/WineThemeManager.java
rm -f app/src/main/java/com/winlator/core/Win32AppWorkarounds.java

# Remove Win32 parsing files
rm -f app/src/main/java/com/winlator/win32/PEParser.java
rm -f app/src/main/java/com/winlator/win32/MSBitmap.java
rm -f app/src/main/java/com/winlator/win32/MSIcon.java
rm -f app/src/main/java/com/winlator/win32/MSLink.java
rm -f app/src/main/java/com/winlator/win32/MSLogFont.java
rm -f app/src/main/java/com/winlator/win32/WinVersions.java

# Remove DX wrapper picker
rm -f app/src/main/java/com/winlator/container/DXWrapperPicker.java
rm -f app/src/main/java/com/winlator/container/DXWrappers.java

echo "Removed $(find . -name '*.java' -path '*/win32/*' | wc -l) Win32 files"
echo "Removed Wine utility files"

# ============================================
# PHASE 3: REPURPOSE BOX64 → LINUX PRESETS
# ============================================
echo "[3/8] Repurposing Box64 → Linux presets..."

# Create linux package directory
mkdir -p app/src/main/java/com/winlator/linux

# Rename Box64Preset → LinuxPreset
cp app/src/main/java/com/winlator/box64/Box64Preset.java app/src/main/java/com/winlator/linux/LinuxPreset.java
sed -i 's/package com\.winlator\.box64;/package com.winlator.linux;/' app/src/main/java/com/winlator/linux/LinuxPreset.java
sed -i 's/class Box64Preset/class LinuxPreset/g' app/src/main/java/com/winlator/linux/LinuxPreset.java
sed -i 's/Box64Preset/LinuxPreset/g' app/src/main/java/com/winlator/linux/LinuxPreset.java

# Rename Box64PresetManager → LinuxPresetManager
cp app/src/main/java/com/winlator/box64/Box64PresetManager.java app/src/main/java/com/winlator/linux/LinuxPresetManager.java
sed -i 's/package com\.winlator\.box64;/package com.winlator.linux;/' app/src/main/java/com/winlator/linux/LinuxPresetManager.java
sed -i 's/class Box64PresetManager/class LinuxPresetManager/g' app/src/main/java/com/winlator/linux/LinuxPresetManager.java
sed -i 's/Box64PresetManager/LinuxPresetManager/g' app/src/main/java/com/winlator/linux/LinuxPresetManager.java
sed -i 's/Box64Preset/LinuxPreset/g' app/src/main/java/com/winlator/linux/LinuxPresetManager.java

# Create LinuxContainer.java
cat > app/src/main/java/com/winlator/linux/LinuxContainer.java << 'JAVAEOF'
package com.winlator.linux;

import com.winlator.container.Container;
import com.winlator.core.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LinuxContainer extends Container {
    public static final String DEFAULT_ROOTFS_TYPE = "debian";
    public static final String DEFAULT_DESKTOP_ENV = "auto";
    public static final String DEFAULT_CPU_GOVERNOR = "ondemand";
    
    private String rootfsPath = "";
    private String rootfsType = DEFAULT_ROOTFS_TYPE;
    private String desktopEnv = DEFAULT_DESKTOP_ENV;
    private String launchCommand = "";
    private String cpuGovernor = DEFAULT_CPU_GOVERNOR;

    public LinuxContainer(int id) {
        super(id);
    }

    public String getRootfsPath() {
        return rootfsPath;
    }

    public void setRootfsPath(String rootfsPath) {
        this.rootfsPath = rootfsPath != null ? rootfsPath : "";
    }

    public String getRootfsType() {
        return rootfsType;
    }

    public void setRootfsType(String rootfsType) {
        this.rootfsType = rootfsType != null ? rootfsType : DEFAULT_ROOTFS_TYPE;
    }

    public String getDesktopEnv() {
        return desktopEnv;
    }

    public void setDesktopEnv(String desktopEnv) {
        this.desktopEnv = desktopEnv != null ? desktopEnv : DEFAULT_DESKTOP_ENV;
    }

    public String getLaunchCommand() {
        return launchCommand;
    }

    public void setLaunchCommand(String launchCommand) {
        this.launchCommand = launchCommand != null ? launchCommand : "";
    }

    public String getCpuGovernor() {
        return cpuGovernor;
    }

    public void setCpuGovernor(String cpuGovernor) {
        this.cpuGovernor = cpuGovernor != null ? cpuGovernor : DEFAULT_CPU_GOVERNOR;
    }

    @Override
    public void saveData() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", getName());
            data.put("screenSize", getScreenSize());
            data.put("envVars", getEnvVars());
            data.put("graphicsDriver", getGraphicsDriver());
            data.put("audioDriver", getAudioDriver());
            data.put("audioDriverConfig", getAudioDriverConfig());
            data.put("hudMode", getHUDMode());
            data.put("startupSelection", getStartupSelection());
            data.put("cpuList", getCPUList());
            data.put("rootfsPath", rootfsPath);
            data.put("rootfsType", rootfsType);
            data.put("desktopEnv", desktopEnv);
            data.put("launchCommand", launchCommand);
            data.put("cpuGovernor", cpuGovernor);
            FileUtils.writeString(getConfigFile(), data.toString());
        } catch (JSONException e) {}
    }

    @Override
    public void loadData(JSONObject data) throws JSONException {
        for (java.util.Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "name": setName(data.getString(key)); break;
                case "screenSize": setScreenSize(data.getString(key)); break;
                case "envVars": setEnvVars(data.getString(key)); break;
                case "graphicsDriver": setGraphicsDriver(data.getString(key)); break;
                case "audioDriver": setAudioDriver(data.getString(key)); break;
                case "audioDriverConfig": setAudioDriverConfig(data.getString(key)); break;
                case "hudMode": setHUDMode((byte)data.getInt(key)); break;
                case "startupSelection": setStartupSelection((byte)data.getInt(key)); break;
                case "cpuList": setCPUList(data.getString(key)); break;
                case "rootfsPath": setRootfsPath(data.getString(key)); break;
                case "rootfsType": setRootfsType(data.getString(key)); break;
                case "desktopEnv": setDesktopEnv(data.getString(key)); break;
                case "launchCommand": setLaunchCommand(data.getString(key)); break;
                case "cpuGovernor": setCpuGovernor(data.getString(key)); break;
            }
        }
    }

    public File getRootfsDir() {
        return new File(rootfsPath);
    }

    public boolean hasRootfs() {
        return rootfsPath != null && !rootfsPath.isEmpty() && new File(rootfsPath).isDirectory();
    }
}
JAVAEOF

# Create LinuxSessionLauncher.java
cat > app/src/main/java/com/winlator/linux/LinuxSessionLauncher.java << 'JAVAEOF'
package com.winlator.linux;

import java.io.File;

public class LinuxSessionLauncher {
    
    private static final String[][] DE_CHECKS = {
        {"/usr/bin/startxfce4", "xfce4-session"},
        {"/usr/bin/startlxqt", "lxqt-session"},
        {"/usr/bin/startplasma-x11", "plasma-session"},
        {"/usr/bin/gnome-session", "gnome-session"},
        {"/usr/bin/startmate", "mate-session"},
        {"/usr/bin/budgie-desktop", "budgie-desktop"}
    };
    
    private static final String[][] TERM_CHECKS = {
        {"/usr/bin/xterm", "xterm"},
        {"/usr/bin/qterminal", "qterminal"},
        {"/usr/bin/xfce4-terminal", "xfce4-terminal"},
        {"/usr/bin/gnome-terminal", "gnome-terminal"},
        {"/usr/bin/mate-terminal", "mate-terminal"},
        {"/usr/bin/lxterminal", "lxterminal"},
        {"/usr/bin/konsole", "konsole"}
    };

    public static String detectDesktopEnvironment(File rootDir) {
        for (String[] de : DE_CHECKS) {
            if (new File(rootDir, de[0]).exists()) {
                return de[1];
            }
        }
        return null;
    }

    public static String detectTerminal(File rootDir) {
        for (String[] term : TERM_CHECKS) {
            if (new File(rootDir, term[0]).exists()) {
                return term[1];
            }
        }
        return null;
    }

    public static String buildLaunchCommand(File rootDir, String desktopEnv, String customLaunchCommand) {
        if (customLaunchCommand != null && !customLaunchCommand.isEmpty()) {
            return customLaunchCommand;
        }
        
        if (desktopEnv != null && !desktopEnv.equals("auto")) {
            switch (desktopEnv) {
                case "xfce": return "startxfce4";
                case "lxqt": return "lxqt-session";
                case "kde": return "plasma-session";
                case "gnome": return "gnome-session";
                case "mate": return "mate-session";
                case "terminal":
                    String term = detectTerminal(rootDir);
                    return term != null ? term : "xterm";
            }
        }
        
        String detected = detectDesktopEnvironment(rootDir);
        if (detected != null) return detected;
        
        String term = detectTerminal(rootDir);
        return term != null ? term : "xterm";
    }

    public static boolean hasDesktopEnvironment(File rootDir) {
        return detectDesktopEnvironment(rootDir) != null;
    }

    public static boolean hasTerminal(File rootDir) {
        return detectTerminal(rootDir) != null;
    }

    public static String getPackageManager(File rootDir) {
        if (new File(rootDir, "/usr/bin/apt").exists()) return "apt";
        if (new File(rootDir, "/usr/bin/apk").exists()) return "apk";
        if (new File(rootDir, "/usr/bin/pacman").exists()) return "pacman";
        if (new File(rootDir, "/usr/bin/dnf").exists()) return "dnf";
        return null;
    }

    public static String getInstallCommand(String pkgManager, String pkg) {
        switch (pkgManager) {
            case "apt": return "apt-get update && apt-get install -y " + pkg;
            case "apk": return "apk add " + pkg;
            case "pacman": return "pacman -S --noconfirm " + pkg;
            case "dnf": return "dnf install -y " + pkg;
            default: return null;
        }
    }
}
JAVAEOF

# Create RootfsDownloader.java
cat > app/src/main/java/com/winlator/linux/RootfsDownloader.java << 'JAVAEOF'
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
JAVAEOF

echo "Created Linux package files"

# ============================================
# PHASE 4: MODIFY GUEST PROGRAM LAUNCHER
# ============================================
echo "[4/8] Modifying GuestProgramLauncherComponent..."

cat > app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java << 'JAVAEOF'
package com.winlator.xenvironment.components;

import android.content.Context;
import android.os.Process;

import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.ProcessHelper;
import com.winlator.linux.LinuxSessionLauncher;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xenvironment.RootFS;

import java.io.File;

public class GuestProgramLauncherComponent extends EnvironmentComponent {
    private String guestExecutable;
    private static int pid = -1;
    private EnvVars envVars;
    private Callback<Integer> terminationCallback;
    private static final Object lock = new Object();

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            pid = execGuestProgram();
        }
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
                pid = -1;
            }
        }
    }

    public Callback<Integer> getTerminationCallback() {
        return terminationCallback;
    }

    public void setTerminationCallback(Callback<Integer> terminationCallback) {
        this.terminationCallback = terminationCallback;
    }

    public String getGuestExecutable() {
        return guestExecutable;
    }

    public void setGuestExecutable(String guestExecutable) {
        this.guestExecutable = guestExecutable;
    }

    public EnvVars getEnvVars() {
        return envVars;
    }

    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }

    private int execGuestProgram() {
        RootFS rootFS = environment.getRootFS();
        File rootDir = rootFS.getRootDir();

        EnvVars envVars = new EnvVars();
        LocaleHelper.setEnvVars(envVars);

        envVars.put("HOME", rootDir + RootFS.HOME_PATH);
        envVars.put("USER", RootFS.USER);
        envVars.put("TMPDIR", rootDir + "/tmp");
        envVars.put("DISPLAY", ":0");
        envVars.put("PATH", rootDir + "/usr/local/bin:" + rootDir + "/usr/bin:" + rootDir + "/bin");
        envVars.put("LD_LIBRARY_PATH", rootFS.getLibDir().getPath());
        envVars.put("ANDROID_SYSVSHM_SERVER", rootDir + UnixSocketConfig.SYSVSHM_SERVER_PATH);

        if (this.envVars != null) envVars.putAll(this.envVars);

        File shmDir = new File(rootDir, "/tmp/shm");
        if (!shmDir.isDirectory()) shmDir.mkdirs();

        String command;
        if (guestExecutable != null && !guestExecutable.isEmpty()) {
            command = guestExecutable;
        } else {
            command = LinuxSessionLauncher.buildLaunchCommand(rootDir, null, null);
        }

        String fullCommand = "/bin/bash -c 'export DISPLAY=:0 && " + command + "'";

        return ProcessHelper.exec(fullCommand, envVars, rootDir, (status) -> {
            synchronized (lock) {
                pid = -1;
            }
            if (terminationCallback != null) terminationCallback.call(status);
        });
    }

    @Override
    public void onPause() {
        synchronized (lock) {
            if (pid != -1) {
                java.util.List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = processes.size() - 1; i >= 0; i--) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state != ProcessHelper.PState.STOPPED) {
                        ProcessHelper.suspendProcess(process.pid);
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        synchronized (lock) {
            if (pid != -1) {
                java.util.List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = 0; i < processes.size(); i++) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state == ProcessHelper.PState.STOPPED) {
                        ProcessHelper.resumeProcess(process.pid);
                    }
                }
            }
        }
    }
}
JAVAEOF

echo "Modified GuestProgramLauncherComponent"

# ============================================
# PHASE 5: MODIFY ROOTFS
# ============================================
echo "[5/8] Modifying RootFS..."

cat > app/src/main/java/com/winlator/xenvironment/RootFS.java << 'JAVAEOF'
package com.winlator.xenvironment;

import android.content.Context;

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
        File legacyDir = new File(context.getFilesDir(), "imagefs");
        File rootDir = new File(context.getFilesDir(), "rootfs");
        if (legacyDir.isDirectory()) legacyDir.renameTo(rootDir);
        return new RootFS(rootDir);
    }

    public File getRootDir() {
        return rootDir;
    }

    public boolean isValid() {
        return rootDir.isDirectory() && getRFSVersionFile().exists();
    }

    public int getVersion() {
        File rfsVersionFile = getRFSVersionFile();
        return rfsVersionFile.exists() ? Integer.parseInt(FileUtils.readLines(rfsVersionFile).get(0)) : 0;
    }

    public String getFormattedVersion() {
        return String.format(Locale.ENGLISH, "%.1f", (float) getVersion());
    }

    public void createRFSVersionFile(int version) {
        getImageInfoDir().mkdirs();
        File file = getRFSVersionFile();
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
        return new File(rootDir, ".linuxx");
    }

    public File getRFSVersionFile() {
        return new File(getImageInfoDir(), ".rfs_version");
    }
}
JAVAEOF

echo "Modified RootFS"

# ============================================
# PHASE 6: MODIFY ROOTFS INSTALLER
# ============================================
echo "[6/8] Modifying RootFSInstaller..."

cat > app/src/main/java/com/winlator/xenvironment/RootFSInstaller.java << 'JAVAEOF'
package com.winlator.xenvironment;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.TarCompressorUtils;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public abstract class RootFSInstaller {
    public static final byte LATEST_VERSION = 1;
    public static final String FILENAME = "debian-xfce-arm64.tar.gz";

    public static void install(final MainActivity activity) {
        AppUtils.keepScreenOn(activity);
        RootFS rootFS = RootFS.find(activity);
        final File rootDir = rootFS.getRootDir();

        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(() -> {
            clearRootDir(rootDir);

            boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, FILENAME, rootDir, (file, size) -> {
                return file;
            });

            if (success) {
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
JAVAEOF

echo "Modified RootFSInstaller"

# ============================================
# PHASE 7: MODIFY XSERVER DISPLAY ACTIVITY
# ============================================
echo "[7/8] Modifying XServerDisplayActivity..."

# Remove Wine-specific imports and replace with Linux equivalents
sed -i 's|import com\.winlator\.core\.WineInfo;|import com.winlator.linux.LinuxSessionLauncher;|' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.WineInstaller;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.WineRegistryEditor;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.WineStartMenuCreator;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.WineThemeManager;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.WineUtils;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.core\.Win32AppWorkarounds;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.winhandler\.TaskManagerDialog;||' app/src/main/java/com/winlator/XServerDisplayActivity.java
sed -i 's|import com\.winlator\.winhandler\.WinHandler;||' app/src/main/java/com/winlator/XServerDisplayActivity.java

echo "Modified XServerDisplayActivity imports"

# ============================================
# PHASE 8: UPDATE CONTAINER DETAIL FRAGMENT
# ============================================
echo "[8/8] Updating ContainerDetailFragment..."

# Remove Wine-specific imports
sed -i '/import com\.winlator\.box64/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.core\.WineInfo/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.core\.WineInstaller/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.core\.WineRegistryEditor/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.core\.WineThemeManager/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.core\.WineUtils/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.win32/d' app/src/main/java/com/winlator/ContainerDetailFragment.java
sed -i '/import com\.winlator\.container\.DXWrapperPicker/d' app/src/main/java/com/winlator/ContainerDetailFragment.java

# Add Linux imports
sed -i '/import com\.winlator\.container\.GraphicsDriverPicker;/a import com.winlator.linux.LinuxSessionLauncher;' app/src/main/java/com/winlator/ContainerDetailFragment.java

echo "Updated ContainerDetailFragment"

# ============================================
# COMMIT CHANGES
# ============================================
echo ""
echo "=== Transformation Complete ==="
echo "Files modified:"
find . -name "*.java" -newer .git/HEAD 2>/dev/null | wc -l
echo ""
echo "New files created:"
ls -la app/src/main/java/com/winlator/linux/
echo ""
echo "Deleted files:"
echo "- WineInfo.java, WineUtils.java, WineInstaller.java"
echo "- WineRegistryEditor.java, WineStartMenuCreator.java, WineThemeManager.java"
echo "- Win32AppWorkarounds.java"
echo "- PEParser.java, MSBitmap.java, MSIcon.java, MSLink.java, MSLogFont.java, WinVersions.java"
echo "- DXWrapperPicker.java, DXWrappers.java"
