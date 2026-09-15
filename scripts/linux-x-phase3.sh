#!/bin/bash
# Linux X Phase 3: Critical Bug Fixes
# Rewrites the heavily broken files completely
set -e

echo "=== Phase 3: Critical Bug Fixes ==="

# ============================================
# FIX 1: Complete rewrite of GuestProgramLauncherComponent
# Add missing LocaleHelper import
# ============================================
echo "[1/5] Fixing GuestProgramLauncherComponent..."

cat > app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java << 'JAVAEOF'
package com.winlator.xenvironment.components;

import android.content.Context;
import android.os.Process;

import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.LocaleHelper;
import com.winlator.core.ProcessHelper;
import com.winlator.linux.LinuxSessionLauncher;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.util.List;

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

        String fullCommand = "/bin/bash -c '" + command + "'";

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
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
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
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
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

echo "Fixed GuestProgramLauncherComponent"

# ============================================
# FIX 2: Complete rewrite of XServerDisplayActivity
# Remove ALL Wine references
# ============================================
echo "[2/5] Rewriting XServerDisplayActivity..."

# First, get the original file
gh api repos/brunodev85/winlator-app/contents/app/src/main/java/com/winlator/XServerDisplayActivity.java -q '.content' 2>&1 | base64 -d > /tmp/XServerDisplayActivity_original.java

# Now create the fixed version by removing Wine-specific code
python3 << 'PYEOF'
import re

with open('/tmp/XServerDisplayActivity_original.java', 'r') as f:
    content = f.read()

# Remove Wine-specific imports
wine_imports = [
    'import com.winlator.container.DXWrappers;',
    'import com.winlator.container.Shortcut;',
    'import com.winlator.contentdialog.DXVKConfigDialog;',
    'import com.winlator.contentdialog.TurnipConfigDialog;',
    'import com.winlator.contentdialog.VKD3DConfigDialog;',
    'import com.winlator.contentdialog.WineD3DConfigDialog;',
    'import com.winlator.core.WineInfo;',
    'import com.winlator.core.WineInstaller;',
    'import com.winlator.core.WineRegistryEditor;',
    'import com.winlator.core.WineStartMenuCreator;',
    'import com.winlator.core.WineThemeManager;',
    'import com.winlator.core.WineUtils;',
    'import com.winlator.core.Win32AppWorkarounds;',
    'import com.winlator.core.WinHandler;',
    'import com.winlator.winhandler.TaskManagerDialog;',
]

for imp in wine_imports:
    content = content.replace(imp, '')

# Add Linux import if not present
if 'import com.winlator.linux.LinuxSessionLauncher;' not in content:
    content = content.replace(
        'import com.winlator.core.GeneralComponents;',
        'import com.winlator.core.GeneralComponents;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine-specific field declarations
content = re.sub(r'private WineInfo wineInfo;\n', '', content)
content = re.sub(r'private final WinHandler winHandler = new WinHandler\(this\);\n', '', content)
content = re.sub(r'private Win32AppWorkarounds win32AppWorkarounds;\n', '', content)

# Remove Wine-specific string fields
content = re.sub(r'private String\[\] dxwrapper =.*?\n', '', content)
content = re.sub(r'private KeyValueSet\[\] dxwrapperConfig.*?\n', '', content)
content = re.sub(r'private String wincomponents.*?\n', '', content)

# Remove Wine initialization in onCreate
content = re.sub(r'boolean useAndroidClipboardOnWine.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove wineprefixNeedsUpdate check
content = re.sub(r'boolean wineprefixNeedsUpdate.*?return;\n\s*\}', '', content, flags=re.DOTALL)

# Remove WineInfo/wineVersion setup
content = re.sub(r'win32AppWorkarounds = new Win32AppWorkarounds\(this\);\n', '', content)
content = re.sub(r'String wineVersion.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove shortcut/dxwrapper setup
content = re.sub(r'String shortcutPath.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove xServer.setWinHandler
content = content.replace('xServer.setWinHandler(winHandler);', '')

# Remove winHandler references
content = re.sub(r'winHandler\..*?\n', '', content)

# Remove TaskManagerDialog
content = content.replace('(new TaskManagerDialog(this)).show();', '// Task manager removed for Linux X')

# Remove setupWineSystemFiles call
content = content.replace('setupWineSystemFiles();', '// Wine system files removed for Linux X')

# Remove extractGraphicsDriverFiles call (keep it - it's useful)
# content = content.replace('extractGraphicsDriverFiles();', '')

# Remove changeWineAudioDriver call
content = content.replace('changeWineAudioDriver();', '// Wine audio driver removed for Linux X')

# Remove extractDXWrapperFiles call
content = content.replace('if (extractDXWrapperFiles()) containerDataChanged = true;', '// DXWrapper files removed for Linux X')

# Remove extractWinComponentFiles call
content = content.replace('extractWinComponentFiles();', '// Win components removed for Linux X')

# Remove setupWineSystemFiles method
content = re.sub(r'private void setupWineSystemFiles\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove changeWineAudioDriver method
content = re.sub(r'private void changeWineAudioDriver\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove extractDXWrapperFiles method
content = re.sub(r'private boolean extractDXWrapperFiles\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove extractWinComponentFiles method
content = re.sub(r'private void extractWinComponentFiles\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove restoreBuiltinDllFiles method
content = re.sub(r'private void restoreBuiltinDllFiles.*?\n    \}', '', content, flags=re.DOTALL)

# Remove getWineStartCommand method
content = re.sub(r'private String getWineStartCommand\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove saveWineRegistryKeys method
content = re.sub(r'private void saveWineRegistryKeys.*?\n    \}', '', content, flags=re.DOTALL)

# Remove verifyUserRegistry method
content = re.sub(r'public boolean verifyUserRegistry\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove isGenerateWineprefix method
content = re.sub(r'private boolean isGenerateWineprefix\(\) \{.*?\n    \}', '', content, flags=re.DOTALL)

# Remove applyGeneralPatches method
content = re.sub(r'private void applyGeneralPatches.*?\n    \}', '', content, flags=re.DOTALL)

# Fix setupXEnvironment - remove Wine-specific env vars
content = content.replace('envVars.put("WINEPREFIX", rootPath+RootFS.WINEPREFIX);', '// Wine prefix removed for Linux X')
content = content.replace('envVars.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");', '// Wine DXGI removed for Linux X')

# Remove WINEDEBUG
content = re.sub(r'boolean enableWineDebug.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Fix guestExecutable - use Linux session launcher
content = content.replace(
    'String guestExecutable = "wine explorer /desktop="+desktopName+","+xServer.screenInfo+" "+getWineStartCommand();',
    'String guestExecutable = LinuxSessionLauncher.buildLaunchCommand(rootFS.getRootDir(), container.getExtra("desktopEnv", "auto"), container.getExtra("launchCommand", ""));'
)

# Remove box64Preset
content = content.replace(
    'guestProgramLauncherComponent.setBox64Preset(shortcut != null ? shortcut.getExtra("box64Preset", container.getBox64Preset()) : container.getBox64Preset());',
    '// Box64 preset removed for Linux X'
)

# Remove wineInfo references
content = re.sub(r'wineInfo = getIntent\(\).*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove desktopName variable
content = re.sub(r'String desktopName =.*?\n', '', content)

# Fix the win32AppWorkarounds references
content = re.sub(r'if \(win32AppWorkarounds != null\) win32AppWorkarounds.*?\n', '', content)

# Remove DXWrapper field references
content = content.replace('private String dxwrapper = Container.DEFAULT_DXWRAPPER;', '')
content = content.replace('private KeyValueSet[] dxwrapperConfig;', '')

# Remove duplicate blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/XServerDisplayActivity_fixed.java', 'w') as f:
    f.write(content)

print("Fixed XServerDisplayActivity")
PYEOF

# Upload the fixed file
FIXED_CONTENT=$(cat /tmp/XServerDisplayActivity_fixed.java | base64 -w 0)
FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/XServerDisplayActivity.java --jq '.sha' 2>&1)
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/XServerDisplayActivity.java -X PUT -f message="Fix XServerDisplayActivity - remove all Wine references" -f content="$FIXED_CONTENT" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3

echo "Fixed XServerDisplayActivity"

# ============================================
# FIX 3: Complete rewrite of ContainerDetailFragment
# ============================================
echo "[3/5] Rewriting ContainerDetailFragment..."

# Get the original
gh api repos/brunodev85/winlator-app/contents/app/src/main/java/com/winlator/ContainerDetailFragment.java -q '.content' 2>&1 | base64 -d > /tmp/ContainerDetailFragment_original.java

python3 << 'PYEOF2'
import re

with open('/tmp/ContainerDetailFragment_original.java', 'r') as f:
    content = f.read()

# Remove Wine-specific imports
wine_imports = [
    'import com.winlator.box64.Box64Preset;',
    'import com.winlator.box64.Box64PresetManager;',
    'import com.winlator.container.DXWrapperPicker;',
    'import com.winlator.container.Drive;',
    'import com.winlator.core.WineInfo;',
    'import com.winlator.core.WineInstaller;',
    'import com.winlator.core.WineRegistryEditor;',
    'import com.winlator.core.WineThemeManager;',
    'import com.winlator.core.WineUtils;',
    'import com.winlator.win32.MSLogFont;',
    'import com.winlator.win32.WinVersions;',
]

for imp in wine_imports:
    content = content.replace(imp, '')

# Add Linux import
if 'import com.winlator.linux.LinuxSessionLauncher;' not in content:
    content = content.replace(
        'import com.winlator.container.GraphicsDriverPicker;',
        'import com.winlator.container.GraphicsDriverPicker;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine version spinner
content = re.sub(r'final ArrayList<WineInfo> wineInfos.*?\n.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove DXWrapper picker
content = re.sub(r'String oldDXWrapperConfig.*?\n.*?\n.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove DXWrapper help button
content = re.sub(r'view\.findViewById\(R\.id\.BTHelpDXWrapper\).*?\n', '', content)

# Remove Box64 preset spinner
content = re.sub(r'final Spinner sBox64Preset.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove WoW64 CPU list
content = re.sub(r'final CPUListView cpuListViewWoW64.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove createWineConfigurationTab call
content = re.sub(r'createWineConfigurationTab\(view\);', '// Wine configuration removed for Linux X')

# Remove createWinComponentsTab call
content = re.sub(r'createWinComponentsTab\(view,.*?\);', '// Win components removed for Linux X')

# Remove createDrivesTab call
content = re.sub(r'createDrivesTab\(view\);', '// Drives removed for Linux X')

# Remove WinVersions spinner setup
content = re.sub(r'if \(tabResId == R\.id\.LLTabAdvanced\).*?\n', '', content)

# Remove saveWineRegistryKeys call
content = content.replace('saveWineRegistryKeys(view);', '// Wine registry removed for Linux X')

# Remove DXWrapper/Box64 from confirm button
content = re.sub(r'String dxwrapper = dxwrapperPicker.*?\n', '', content)
content = re.sub(r'String dxwrapperConfig = dxwrapperPicker.*?\n', '', content)
content = re.sub(r'String box64Preset = Box64PresetManager.*?\n', '', content)

# Remove Wine theme
content = re.sub(r'String desktopTheme = getDesktopTheme.*?\n', '', content)

# Remove container.setDXWrapper/setBox64Preset/setDesktopTheme
content = re.sub(r'container\.setDXWrapper.*?\n', '', content)
content = re.sub(r'container\.setDXWrapperConfig.*?\n', '', content)
content = re.sub(r'container\.setBox64Preset.*?\n', '', content)
content = re.sub(r'container\.setDesktopTheme.*?\n', '', content)

# Remove data.put for Wine fields
content = re.sub(r'data\.put\("dxwrapper.*?\n', '', content)
content = re.sub(r'data\.put\("dxwrapperConfig.*?\n', '', content)
content = re.sub(r'data\.put\("box64Preset.*?\n', '', content)
content = re.sub(r'data\.put\("desktopTheme.*?\n', '', content)
content = re.sub(r'data\.put\("wineVersion.*?\n', '', content)
content = re.sub(r'data\.put\("wincomponents.*?\n', '', content)
content = re.sub(r'data\.put\("drives.*?\n', '', content)

# Remove wineInfos.size() check
content = re.sub(r'if \(wineInfos\.size\(\).*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove VortekConfigDialog restart check
content = re.sub(r'boolean requireRestart =.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove saveWineRegistryKeys method
content = re.sub(r'private void saveWineRegistryKeys.*?\n    \}', '', content, flags=re.DOTALL)

# Remove createWineConfigurationTab method
content = re.sub(r'private void createWineConfigurationTab.*?\n    \}', '', content, flags=re.DOTALL)

# Remove createWinComponentsTab method
content = re.sub(r'private void createWinComponentsTab.*?\n    \}', '', content, flags=re.DOTALL)

# Remove createDrivesTab method
content = re.sub(r'private void createDrivesTab.*?\n    \}', '', content, flags=re.DOTALL)

# Remove getWinComponents method
content = re.sub(r'private String getWinComponents.*?\n    \}', '', content, flags=re.DOTALL)

# Remove getDrives method
content = re.sub(r'private String getDrives.*?\n    \}', '', content, flags=re.DOTALL)

# Remove getDesktopTheme method
content = re.sub(r'private String getDesktopTheme.*?\n    \}', '', content, flags=re.DOTALL)

# Remove loadWineVersionSpinner method
content = re.sub(r'private void loadWineVersionSpinner.*?\n    \}', '', content, flags=re.DOTALL)

# Remove duplicate blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/ContainerDetailFragment_fixed.java', 'w') as f:
    f.write(content)

print("Fixed ContainerDetailFragment")
PYEOF2

# Upload the fixed file
FIXED_CONTENT=$(cat /tmp/ContainerDetailFragment_fixed.java | base64 -w 0)
FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/ContainerDetailFragment.java --jq '.sha' 2>&1)
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/ContainerDetailFragment.java -X PUT -f message="Fix ContainerDetailFragment - remove all Wine references" -f content="$FIXED_CONTENT" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3

echo "Fixed ContainerDetailFragment"

# ============================================
# FIX 4: Fix RootFSInstaller - use correct compression type
# ============================================
echo "[4/5] Fixing RootFSInstaller..."

cat > app/src/main/java/com/winlator/xenvironment/RootFSInstaller.java << 'JAVAEOF2'
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
    public static final String FILENAME = "rootfs.tzst";

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
JAVAEOF2

echo "Fixed RootFSInstaller"

# ============================================
# FIX 5: Fix SettingsFragment - remove Wine references
# ============================================
echo "[5/5] Fixing SettingsFragment..."

# Get the original
gh api repos/brunodev85/winlator-app/contents/app/src/main/java/com/winlator/SettingsFragment.java -q '.content' 2>&1 | base64 -d > /tmp/SettingsFragment_original.java

python3 << 'PYEOF3'
import re

with open('/tmp/SettingsFragment_original.java', 'r') as f:
    content = f.read()

# Remove Wine-specific imports
wine_imports = [
    'import com.winlator.box64.Box64EditPresetDialog;',
    'import com.winlator.box64.Box64Preset;',
    'import com.winlator.box64.Box64PresetManager;',
    'import com.winlator.core.WineInfo;',
    'import com.winlator.core.WineInstaller;',
    'import com.winlator.winhandler.GamepadHandler;',
]

for imp in wine_imports:
    content = content.replace(imp, '')

# Remove Box64 version spinner setup
content = re.sub(r'final Spinner sBox64Version.*?\n.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove Box64 preset spinner setup
content = re.sub(r'final Spinner sBox64Preset.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove Wine debug checkbox
content = re.sub(r'final CheckBox cbOpenAndroidBrowserFromWine.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove Android clipboard on Wine checkbox
content = re.sub(r'final CheckBox cbUseAndroidClipboardOnWine.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove duplicate blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/SettingsFragment_fixed.java', 'w') as f:
    f.write(content)

print("Fixed SettingsFragment")
PYEOF3

# Upload the fixed file
FIXED_CONTENT=$(cat /tmp/SettingsFragment_fixed.java | base64 -w 0)
FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/SettingsFragment.java --jq '.sha' 2>&1)
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/SettingsFragment.java -X PUT -f message="Fix SettingsFragment - remove Wine references" -f content="$FIXED_CONTENT" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3

echo "Fixed SettingsFragment"

echo ""
echo "=== Phase 3 Complete: All critical bugs fixed ==="
