#!/bin/bash
# Linux X Phase 3: Critical Bug Fixes
# Downloads originals via curl, processes with Python, uploads with gh api
set -euo pipefail

echo "=== Phase 3: Critical Bug Fixes ==="

ORIG_BASE="https://raw.githubusercontent.com/brunodev85/winlator-app/main/app/src/main/java/com/winlator"
FORK="repos/maybeagoodguy60-droid/winlator-app/contents"
MAIN="app/src/main/java/com/winlator"


upload_file() {
    local local_path="$1"
    local remote_path="$2"
    local message="$3"
    local sha
    sha=$(gh api "$FORK/$remote_path" --jq '.sha' 2>/dev/null || echo "")
    if [ -z "$sha" ]; then
        echo "ERROR: Could not get sha for $remote_path"
        return 1
    fi
    local b64
    b64=$(base64 -w0 "$local_path")
    gh api "$FORK/$remote_path" -X PUT \
        -f message="$message" \
        -f content="$b64" \
        -f sha="$sha" \
        -f branch="main" >/dev/null 2>&1
    echo "Uploaded $remote_path"
}

# ============================================
# FIX 1: GuestProgramLauncherComponent - add missing import
# ============================================
echo "[1/5] Fixing GuestProgramLauncherComponent..."

curl -sL "$ORIG_BASE/xenvironment/components/GuestProgramLauncherComponent.java" -o /tmp/guest_orig.java
python3 -c "
import re
with open('/tmp/guest_orig.java') as f: c = f.read()
c = c.replace(
    'import com.winlator.core.FileUtils;',
    'import com.winlator.core.FileUtils;\nimport com.winlator.core.LocaleHelper;'
)
with open('/tmp/guest_fixed.java','w') as f: f.write(c)
print('OK:', len(c), 'chars')
"
upload_file /tmp/guest_fixed.java "$MAIN/xenvironment/components/GuestProgramLauncherComponent.java" "Add missing LocaleHelper import to GuestProgramLauncherComponent"

# ============================================
# FIX 2: XServerDisplayActivity - remove all Wine code
# ============================================
echo "[2/5] Rewriting XServerDisplayActivity..."

curl -sL "$ORIG_BASE/XServerDisplayActivity.java" -o /tmp/xsda_orig.java
python3 << 'XSEOF'
import re

with open('/tmp/xsda_orig.java') as f:
    c = f.read()
print(f"Original: {len(c)} chars")

# Remove Wine imports
for imp in [
    'import com.winlator.container.DXWrappers;',
    'import com.winlator.contentdialog.DXVKConfigDialog;',
    'import com.winlator.contentdialog.TurnipConfigDialog;',
    'import com.winlator.contentdialog.VKD3DConfigDialog;',
    'import com.winlator.contentdialog.WineD3DConfigDialog;',
    'import com.winlator.core.Win32AppWorkarounds;',
    'import com.winlator.core.WineInfo;',
    'import com.winlator.core.WineInstaller;',
    'import com.winlator.core.WineRegistryEditor;',
    'import com.winlator.core.WineStartMenuCreator;',
    'import com.winlator.core.WineThemeManager;',
    'import com.winlator.core.WineUtils;',
    'import com.winlator.core.WinHandler;',
    'import com.winlator.winhandler.TaskManagerDialog;',
]:
    c = c.replace(imp, '')

# Add Linux import
if 'import com.winlator.linux.LinuxSessionLauncher;' not in c:
    c = c.replace(
        'import com.winlator.core.GeneralComponents;',
        'import com.winlator.core.GeneralComponents;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine field declarations
for p in [
    r'    private WineInfo wineInfo;\n',
    r'    private final WinHandler winHandler = new WinHandler\(this\);\n',
    r'    private Win32AppWorkarounds win32AppWorkarounds;\n',
    r'    private String\[\] dxwrapper = new String\[\]\{Container\.DEFAULT_DXWRAPPER, null\};\n',
    r'    private KeyValueSet\[\] dxwrapperConfig;\n',
]:
    c = re.sub(p, '', c)

# Remove useAndroidClipboardOnWine block
c = re.sub(r'        boolean useAndroidClipboardOnWine.*?\n.*?\n.*?\n', '', c, flags=re.DOTALL)

# Remove wineprefixNeedsUpdate block
c = re.sub(r'            boolean wineprefixNeedsUpdate.*?\n\s*if \(wineprefixNeedsUpdate\) \{.*?\n.*?\n.*?\n.*?\n\s*\}\n', '', c, flags=re.DOTALL)

# Remove win32AppWorkarounds init
c = re.sub(r'            win32AppWorkarounds = new Win32AppWorkarounds\(this\);\n', '', c)

# Remove WineInfo setup
c = re.sub(
    r'            String wineVersion = container\.getWineVersion\(\);\n.*?wineInfo = WineInfo\.fromIdentifier\(this, wineVersion\);\n.*?if \(wineInfo != WineInfo\.MAIN_WINE_INFO\).*?\n',
    '', c, flags=re.DOTALL
)

# Remove shortcut/dxwrapper extraction block (big block with multiple conditions)
c = re.sub(
    r'            String shortcutPath = container\.getExtra\("shortcutPath"\).*?dxwrapperConfig = dxwrapperPicker\.getConfigs\(\);\n\s*\}\n\s*\}',
    '', c, flags=re.DOTALL
)

# Remove setWinHandler
c = c.replace('xServer.setWinHandler(winHandler);', '// WinHandler removed for Linux X')

# Replace guestExecutable with Linux session
c = re.sub(
    r'                String guestExecutable = "wine explorer /desktop=.*?";',
    '                String guestExecutable = LinuxSessionLauncher.buildLaunchCommand(rootFS.getRootDir(), container.getExtra("desktopEnv", "auto"), container.getExtra("launchCommand", ""));',
    c
)

# Remove box64Preset
c = re.sub(r'            guestProgramLauncherComponent\.setBox64Preset\(.*?\);\n', '', c)

# Remove wineInfo = getIntent() block
c = re.sub(r'        wineInfo = getIntent\(\)\.getParcelableExtra\("wine_info"\);\n.*?\n', '', c, flags=re.DOTALL)

# Remove setupWineSystemFiles call
c = c.replace('setupWineSystemFiles();', '// Wine system files removed for Linux X')

# Remove changeWineAudioDriver call
c = c.replace('changeWineAudioDriver();', '// Wine audio driver removed for Linux X')

# Remove extractDXWrapperFiles call
c = re.sub(r'        if \(extractDXWrapperFiles\(\)\) containerDataChanged = true;\n', '// DXWrapper files removed for Linux X\n', c)

# Remove extractWinComponentFiles call
c = c.replace('extractWinComponentFiles();', '// Win components removed for Linux X')

# Remove wineprefixWasUpdated
c = re.sub(r'        boolean wineprefixWasUpdated = WineUtils\.isWineprefixWasUpdated\(container\);\n', '', c)
c = c.replace('wineprefixWasUpdated', 'false')

# Remove WINEPREFIX env var
c = c.replace('envVars.put("WINEPREFIX", rootPath+RootFS.WINEPREFIX);', '// Wine prefix removed for Linux X')

# Remove WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER
c = c.replace('envVars.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");', '')

# Remove WINEDEBUG block
c = re.sub(r'            boolean enableWineDebug.*?\n.*?\n.*?\n', '', c, flags=re.DOTALL)

# Remove WineUtils.createDosdevicesSymlinks
c = re.sub(r'        WineUtils\.createDosdevicesSymlinks\(container, true\);\n', '', c)

# Remove WineUtils.changeServicesStatus
c = re.sub(r'            WineUtils\.changeServicesStatus\(container, container\.getStartupSelection\(\)\);\n', '', c)

# Remove openAndroidBrowserFromWine block
c = re.sub(r'        boolean openAndroidBrowserFromWine.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n', '', c, flags=re.DOTALL)

# Remove desktopName variable
c = re.sub(r'            String desktopName = container\.getExtra\("desktopName"\).*?\n', '', c)

# Remove Wine private methods
for method in [
    r'    private void setupWineSystemFiles\(\) \{.*?\n    \}',
    r'    private void changeWineAudioDriver\(\) \{.*?\n    \}',
    r'    private boolean extractDXWrapperFiles\(\) \{.*?\n    \}',
    r'    private void extractWinComponentFiles\(\) \{.*?\n    \}',
    r'    private void restoreBuiltinDllFiles.*?\n    \}',
    r'    private String getWineStartCommand\(\) \{.*?\n    \}',
    r'    private void saveWineRegistryKeys.*?\n    \}',
    r'    public boolean verifyUserRegistry\(\) \{.*?\n    \}',
    r'    private boolean isGenerateWineprefix\(\) \{.*?\n    \}',
    r'    private void applyGeneralPatches.*?\n    \}',
]:
    c = re.sub(method, '', c, flags=re.DOTALL)

# Remove win32AppWorkarounds.stop() reference
c = re.sub(r'        if \(win32AppWorkarounds != null\) win32AppWorkarounds\..*?\n', '', c)

# Clean blank lines
c = re.sub(r'\n{3,}', '\n\n', c)

with open('/tmp/xsda_fixed.java', 'w') as f:
    f.write(c)
print(f"Fixed: {len(c)} chars")
XSEOF

upload_file /tmp/xsda_fixed.java "$MAIN/XServerDisplayActivity.java" "Rewrite XServerDisplayActivity - remove all Wine references"

# ============================================
# FIX 3: ContainerDetailFragment - remove all Wine code
# ============================================
echo "[3/5] Rewriting ContainerDetailFragment..."

curl -sL "$ORIG_BASE/ContainerDetailFragment.java" -o /tmp/cdf_orig.java
python3 << 'CDFEOF'
import re

with open('/tmp/cdf_orig.java') as f:
    c = f.read()
print(f"Original: {len(c)} chars")

# Remove Wine imports
for imp in [
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
]:
    c = c.replace(imp, '')

# Add Linux import
if 'import com.winlator.linux.LinuxSessionLauncher;' not in c:
    c = c.replace(
        'import com.winlator.container.GraphicsDriverPicker;',
        'import com.winlator.container.GraphicsDriverPicker;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine version spinner block
c = re.sub(
    r'        final ArrayList<WineInfo> wineInfos = WineInstaller\.getInstalledWineInfos\(context\);\n.*?WinVersions\.loadSpinner\(container, sWinVersion\);\n',
    '', c, flags=re.DOTALL
)

# Remove DXWrapper picker block
c = re.sub(
    r'        String oldDXWrapperConfig = isEditMode\(\).*?BTHelpDXWrapper.*?\n',
    '', c, flags=re.DOTALL
)

# Remove Box64 preset spinner
c = re.sub(
    r'        final Spinner sBox64Preset = .*?\n.*?Box64PresetManager\.loadSpinner\(sBox64Preset.*?\n',
    '', c, flags=re.DOTALL
)

# Remove WoW64 CPU list
c = re.sub(r'        final CPUListView cpuListViewWoW64.*?\n.*?\n', '', c)

# Remove createWineConfigurationTab
c = re.sub(r'        createWineConfigurationTab\(view\);\n', '', c)

# Remove createWinComponentsTab
c = re.sub(r'        createWinComponentsTab\(view,.*?\);\n', '', c)

# Remove createDrivesTab
c = re.sub(r'        createDrivesTab\(view\);\n', '', c)

# Remove saveWineRegistryKeys call
c = re.sub(r'                    saveWineRegistryKeys\(view\);\n', '', c)

# Remove DXWrapper/Box64 data extraction in confirmButton
c = re.sub(r'                String dxwrapper = dxwrapperPicker.*?\n', '', c)
c = re.sub(r'                String dxwrapperConfig = dxwrapperPicker.*?\n', '', c)
c = re.sub(r'                String box64Preset = Box64PresetManager.*?\n', '', c)
c = re.sub(r'                String desktopTheme = getDesktopTheme\(view\);\n', '', c)

# Remove container.setXxx calls
for p in [
    r'                    container\.setDXWrapper\(dxwrapper\);\n',
    r'                    container\.setDXWrapperConfig\(dxwrapperConfig\);\n',
    r'                    container\.setBox64Preset\(box64Preset\);\n',
    r'                    container\.setDesktopTheme\(desktopTheme\);\n',
]:
    c = re.sub(p, '', c)

# Remove data.put calls for Wine fields
for p in [
    r'                    data\.put\("dxwrapper", dxwrapper\);\n',
    r'                    data\.put\("dxwrapperConfig", dxwrapperConfig\);\n',
    r'                    data\.put\("box64Preset", box64Preset\);\n',
    r'                    data\.put\("desktopTheme", desktopTheme\);\n',
    r'                    data\.put\("wineVersion".*?\n',
    r'                    data\.put\("wincomponents".*?\n',
    r'                    data\.put\("drives".*?\n',
]:
    c = re.sub(p, '', c)

# Remove requireRestart check
c = re.sub(r'        boolean requireRestart = .*?\n.*?\n.*?\n', '', c, flags=re.DOTALL)

# Remove wineInfos.size() check
c = re.sub(r'        if \(wineInfos\.size\(\) > 0\) \{\n.*?\}\n', '', c, flags=re.DOTALL)

# Remove Wine private methods
for method in [
    r'    private void saveWineRegistryKeys.*?\n    \}',
    r'    private void createWineConfigurationTab.*?\n    \}',
    r'    private void createWinComponentsTab.*?\n    \}',
    r'    private void createDrivesTab.*?\n    \}',
    r'    private String getWinComponents.*?\n    \}',
    r'    private String getDrives.*?\n    \}',
    r'    private String getDesktopTheme.*?\n    \}',
    r'    private void loadWineVersionSpinner.*?\n    \}',
]:
    c = re.sub(method, '', c, flags=re.DOTALL)

# Clean blank lines
c = re.sub(r'\n{3,}', '\n\n', c)

with open('/tmp/cdf_fixed.java', 'w') as f:
    f.write(c)
print(f"Fixed: {len(c)} chars")
CDFEOF

upload_file /tmp/cdf_fixed.java "$MAIN/ContainerDetailFragment.java" "Rewrite ContainerDetailFragment - remove all Wine references"

# ============================================
# FIX 4: SettingsFragment - remove Wine references
# ============================================
echo "[4/5] Fixing SettingsFragment..."

curl -sL "$ORIG_BASE/SettingsFragment.java" -o /tmp/sf_orig.java
python3 << 'SFEOF'
import re

with open('/tmp/sf_orig.java') as f:
    c = f.read()
print(f"Original: {len(c)} chars")

# Remove Wine imports
for imp in [
    'import com.winlator.box64.Box64EditPresetDialog;',
    'import com.winlator.box64.Box64Preset;',
    'import com.winlator.box64.Box64PresetManager;',
    'import com.winlator.core.WineInfo;',
    'import com.winlator.core.WineInstaller;',
    'import com.winlator.winhandler.GamepadHandler;',
]:
    c = c.replace(imp, '')

# Remove Box64 version spinner
c = re.sub(
    r'        final Spinner sBox64Version = .*?\n.*?sBox64Version\..*?\n.*?\n.*?\n',
    '', c, flags=re.DOTALL
)

# Remove Box64 preset spinner
c = re.sub(
    r'        final Spinner sBox64Preset = .*?\n.*?sBox64Preset\..*?\n.*?\n.*?\n',
    '', c, flags=re.DOTALL
)

# Remove openAndroidBrowserFromWine checkbox
c = re.sub(
    r'        final CheckBox cbOpenAndroidBrowserFromWine.*?\n.*?\n.*?\n',
    '', c, flags=re.DOTALL
)

# Remove useAndroidClipboardOnWine checkbox
c = re.sub(
    r'        final CheckBox cbUseAndroidClipboardOnWine.*?\n.*?\n.*?\n',
    '', c, flags=re.DOTALL
)

# Clean blank lines
c = re.sub(r'\n{3,}', '\n\n', c)

with open('/tmp/sf_fixed.java', 'w') as f:
    f.write(c)
print(f"Fixed: {len(c)} chars")
SFEOF

upload_file /tmp/sf_fixed.java "$MAIN/SettingsFragment.java" "Fix SettingsFragment - remove Wine references"

# ============================================
# FIX 5: RootFSInstaller - ensure correct
# ============================================
echo "[5/5] Fixing RootFSInstaller..."

cat > /tmp/rfsi.java << 'RFSIEOF'
package com.winlator.xenvironment;

import com.winlator.MainActivity;
import com.winlator.R;
import com.winlator.core.AppUtils;
import com.winlator.core.DownloadProgressDialog;
import com.winlator.core.FileUtils;
import com.winlator.core.TarCompressorUtils;

import java.io.File;
import java.util.concurrent.Executors;

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
RFSIEOF

upload_file /tmp/rfsi.java "$MAIN/xenvironment/RootFSInstaller.java" "Fix RootFSInstaller - use ZSTD decompression"

echo ""
echo "=== Phase 3 Complete: All critical bugs fixed ==="
