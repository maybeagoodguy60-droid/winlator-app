#!/bin/bash
# Linux X Phase 3: Critical Bug Fixes
# Uses curl to download originals, then rewrites them
set -e

echo "=== Phase 3: Critical Bug Fixes ==="

ORIGINAL_REPO="https://raw.githubusercontent.com/brunodev85/winlator-app/main/app/src/main/java/com/winlator"

# ============================================
# FIX 1: GuestProgramLauncherComponent - add missing import
# ============================================
echo "[1/5] Fixing GuestProgramLauncherComponent..."

GUEST_FILE=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java -q '.content' 2>&1)
GUEST_FILE=$(echo "$GUEST_FILE" | tr -d '\n' | base64 -d 2>/dev/null || echo "")

if [ -z "$GUEST_FILE" ]; then
    echo "ERROR: Could not decode GuestProgramLauncherComponent"
    exit 1
fi

# Check if LocaleHelper import is missing
if ! echo "$GUEST_FILE" | grep -q "import com.winlator.core.LocaleHelper;"; then
    echo "Adding missing LocaleHelper import..."
    GUEST_FILE=$(echo "$GUEST_FILE" | sed 's|import com.winlator.core.FileUtils;|import com.winlator.core.FileUtils;\nimport com.winlator.core.LocaleHelper;|')
fi

GUEST_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java --jq '.sha' 2>&1)
echo "$GUEST_FILE" | base64 -w0 > /tmp/guest_b64.txt
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/xenvironment/components/GuestProgramLauncherComponent.java \
  -X PUT -f message="Fix GuestProgramLauncherComponent - add missing LocaleHelper import" \
  -f content="$(cat /tmp/guest_b64.txt)" -f sha="$GUEST_SHA" -f branch="main" 2>&1 | head -3
echo "Fixed GuestProgramLauncherComponent"

# ============================================
# FIX 2: Download + rewrite XServerDisplayActivity
# ============================================
echo "[2/5] Rewriting XServerDisplayActivity..."

curl -sL "$ORIGINAL_REPO/XServerDisplayActivity.java" -o /tmp/XServerDisplayActivity_original.java
if [ ! -s /tmp/XServerDisplayActivity_original.java ]; then
    echo "ERROR: Could not download XServerDisplayActivity"
    exit 1
fi
echo "Downloaded original: $(wc -c < /tmp/XServerDisplayActivity_original.java) bytes"

python3 << 'PYEOF'
import re

with open('/tmp/XServerDisplayActivity_original.java', 'r') as f:
    content = f.read()

# Remove Wine-specific imports
wine_imports = [
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
]
for imp in wine_imports:
    content = content.replace(imp, '')

# Add Linux import
if 'import com.winlator.linux.LinuxSessionLauncher;' not in content:
    content = content.replace(
        'import com.winlator.core.GeneralComponents;',
        'import com.winlator.core.GeneralComponents;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine field declarations
for pattern in [
    r'    private WineInfo wineInfo;\n',
    r'    private final WinHandler winHandler = new WinHandler\(this\);\n',
    r'    private Win32AppWorkarounds win32AppWorkarounds;\n',
    r'    private String\[\] dxwrapper = new String\[\]\{Container.DEFAULT_DXWRAPPER, null\};\n',
    r'    private KeyValueSet\[\] dxwrapperConfig;\n',
]:
    content = re.sub(pattern, '', content)

# Fix setupXEnvironment - remove Wine env vars
content = content.replace('envVars.put("WINEPREFIX", rootPath+RootFS.WINEPREFIX);', '// Wine prefix removed for Linux X')
content = content.replace('envVars.put("WINE_DO_NOT_CREATE_DXGI_DEVICE_MANAGER", "1");', '// Wine DXGI removed for Linux X')

# Remove WINEDEBUG
for p in [
    r'            boolean enableWineDebug.*?\n.*?\n.*?\n',
    r'        boolean wineprefixNeedsUpdate.*?\n\s*if \(wineprefixNeedsUpdate\) \{.*?\n.*?\n.*?\n.*?\n\s*\}\n',
]:
    content = re.sub(p, '', content, flags=re.DOTALL)

# Remove Win32AppWorkarounds init
content = re.sub(r'            win32AppWorkarounds = new Win32AppWorkarounds\(this\);\n', '', content)

# Remove WineInfo setup block
content = re.sub(
    r'            String wineVersion = container\.getWineVersion\(\);\n.*?wineInfo = WineInfo\.fromIdentifier\(this, wineVersion\);\n.*?if \(wineInfo != WineInfo\.MAIN_WINE_INFO\).*?\n',
    '', content, flags=re.DOTALL
)

# Remove shortcut/dxwrapper extraction block
content = re.sub(
    r'            String shortcutPath = container\.getExtra\("shortcutPath"\);\n.*?dxwrapperConfig = dxwrapperPicker\.getConfigs\(\);\n.*?\}\n.*?\}',
    '', content, flags=re.DOTALL
)

# Remove setWinHandler
content = content.replace('xServer.setWinHandler(winHandler);', '// WinHandler removed for Linux X')

# Remove useAndroidClipboardOnWine
content = re.sub(r'        boolean useAndroidClipboardOnWine.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove win32AppWorkarounds references
content = re.sub(r'            if \(win32AppWorkarounds != null\) win32AppWorkarounds\.start\(\);\n', '', content)
content = re.sub(r'        if \(win32AppWorkarounds != null\) win32AppWorkarounds\..*?\n', '', content)

# Replace guestExecutable with Linux session
content = re.sub(
    r'                String guestExecutable = "wine explorer /desktop=.*?";',
    '                String guestExecutable = LinuxSessionLauncher.buildLaunchCommand(rootFS.getRootDir(), container.getExtra("desktopEnv", "auto"), container.getExtra("launchCommand", ""));',
    content
)

# Remove box64Preset line
content = re.sub(
    r'            guestProgramLauncherComponent\.setBox64Preset\(.*?\);\n',
    '', content
)

# Remove wineInfo = getIntent() block
content = re.sub(r'        wineInfo = getIntent\(\)\.getParcelableExtra\("wine_info"\);\n.*?\n', '', content, flags=re.DOTALL)

# Remove setupWineSystemFiles call
content = content.replace('setupWineSystemFiles();', '// setupWineSystemFiles removed')

# Remove changeWineAudioDriver call
content = content.replace('changeWineAudioDriver();', '// changeWineAudioDriver removed')

# Remove extractDXWrapperFiles call
content = re.sub(r'        if \(extractDXWrapperFiles\(\)\) containerDataChanged = true;\n', '// extractDXWrapperFiles removed\n', content)

# Remove extractWinComponentFiles call
content = content.replace('extractWinComponentFiles();', '// extractWinComponentFiles removed')

# Remove wineprefixWasUpdated
content = re.sub(r'        boolean wineprefixWasUpdated = WineUtils\.isWineprefixWasUpdated\(container\);\n', '', content)
content = re.sub(r'wineprefixWasUpdated', 'false', content)

# Remove WINE-version checks in containerDataChanged
content = re.sub(
    r'        if \(!container\.getExtra\("appVersion"\).*?\n.*?\n.*?\n.*?\n.*?\n',
    '        if (!container.getExtra("appVersion").equals(appVersion) || !container.getExtra("rfsVersion").equals(rfsVersion)) {\n',
    content, flags=re.DOTALL
)

# Remove WineUtils.createDosdevicesSymlinks
content = re.sub(r'        WineUtils\.createDosdevicesSymlinks\(container, true\);\n', '', content)

# Remove WineUtils.changeServicesStatus
content = re.sub(r'            WineUtils\.changeServicesStatus\(container, container\.getStartupSelection\(\)\);\n', '', content)

# Remove openAndroidBrowserFromWine
content = re.sub(r'        boolean openAndroidBrowserFromWine.*?\n.*?\n.*?\n.*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove private Wine methods
for method_pattern in [
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
    content = re.sub(method_pattern, '', content, flags=re.DOTALL)

# Remove desktopName variable
content = re.sub(r'            String desktopName = container\.getExtra\("desktopName"\).*?\n', '', content)

# Remove blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/XServerDisplayActivity_fixed.java', 'w') as f:
    f.write(content)

print(f"Fixed XServerDisplayActivity: {len(content)} chars")
PYEOF

# Upload the fixed file
FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/XServerDisplayActivity.java --jq '.sha' 2>&1)
cat /tmp/XServerDisplayActivity_fixed.java | base64 -w0 > /tmp/xsda_b64.txt
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/XServerDisplayActivity.java \
  -X PUT -f message="Rewrite XServerDisplayActivity - remove all Wine references" \
  -f content="$(cat /tmp/xsda_b64.txt)" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3
echo "Fixed XServerDisplayActivity"

# ============================================
# FIX 3: Download + rewrite ContainerDetailFragment
# ============================================
echo "[3/5] Rewriting ContainerDetailFragment..."

curl -sL "$ORIGINAL_REPO/ContainerDetailFragment.java" -o /tmp/ContainerDetailFragment_original.java
echo "Downloaded original: $(wc -c < /tmp/ContainerDetailFragment_original.java) bytes"

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

# Add Linux imports
if 'import com.winlator.linux.LinuxSessionLauncher;' not in content:
    content = content.replace(
        'import com.winlator.container.GraphicsDriverPicker;',
        'import com.winlator.container.GraphicsDriverPicker;\nimport com.winlator.linux.LinuxSessionLauncher;'
    )

# Remove Wine version spinner
content = re.sub(
    r'        final ArrayList<WineInfo> wineInfos = WineInstaller\.getInstalledWineInfos\(context\);\n.*?WinVersions\.loadSpinner\(container, sWinVersion\);\n',
    '', content, flags=re.DOTALL
)

# Remove DXWrapper picker
content = re.sub(
    r'        String oldDXWrapperConfig = isEditMode\(\).*?\n.*?DXWrapperPicker dxwrapperPicker.*?\n.*?BTHelpDXWrapper.*?\n',
    '', content, flags=re.DOTALL
)

# Remove Box64 preset spinner
content = re.sub(
    r'        final Spinner sBox64Preset = .*?\n.*?Box64PresetManager\.loadSpinner\(sBox64Preset.*?\n',
    '', content, flags=re.DOTALL
)

# Remove WoW64 CPU list
content = re.sub(r'        final CPUListView cpuListViewWoW64.*?\n.*?\n', '', content)

# Remove createWineConfigurationTab
content = re.sub(r'        createWineConfigurationTab\(view\);\n', '', content)

# Remove createWinComponentsTab
content = re.sub(r'        createWinComponentsTab\(view,.*?\);\n', '', content)

# Remove createDrivesTab
content = re.sub(r'        createDrivesTab\(view\);\n', '', content)

# Remove saveWineRegistryKeys call
content = re.sub(r'                    saveWineRegistryKeys\(view\);\n', '', content)

# Remove DXWrapper/Box64 data extraction in confirmButton
content = re.sub(r'                String dxwrapper = dxwrapperPicker.*?\n', '', content)
content = re.sub(r'                String dxwrapperConfig = dxwrapperPicker.*?\n', '', content)
content = re.sub(r'                String box64Preset = Box64PresetManager.*?\n', '', content)
content = re.sub(r'                String desktopTheme = getDesktopTheme\(view\);\n', '', content)

# Remove container.setXxx calls
for p in [
    r'                    container\.setDXWrapper\(dxwrapper\);\n',
    r'                    container\.setDXWrapperConfig\(dxwrapperConfig\);\n',
    r'                    container\.setBox64Preset\(box64Preset\);\n',
    r'                    container\.setDesktopTheme\(desktopTheme\);\n',
]:
    content = re.sub(p, '', content)

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
    content = re.sub(p, '', content)

# Remove requireRestart check
content = re.sub(r'        boolean requireRestart = .*?\n.*?\n.*?\n', '', content, flags=re.DOTALL)

# Remove wineInfos.size() check
content = re.sub(
    r'        if \(wineInfos\.size\(\) > 0\) \{\n.*?\}\n',
    '', content, flags=re.DOTALL
)

# Remove Wine private methods
for method_pattern in [
    r'    private void saveWineRegistryKeys.*?\n    \}',
    r'    private void createWineConfigurationTab.*?\n    \}',
    r'    private void createWinComponentsTab.*?\n    \}',
    r'    private void createDrivesTab.*?\n    \}',
    r'    private String getWinComponents.*?\n    \}',
    r'    private String getDrives.*?\n    \}',
    r'    private String getDesktopTheme.*?\n    \}',
    r'    private void loadWineVersionSpinner.*?\n    \}',
]:
    content = re.sub(method_pattern, '', content, flags=re.DOTALL)

# Remove duplicate blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/ContainerDetailFragment_fixed.java', 'w') as f:
    f.write(content)

print(f"Fixed ContainerDetailFragment: {len(content)} chars")
PYEOF2

FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/ContainerDetailFragment.java --jq '.sha' 2>&1)
cat /tmp/ContainerDetailFragment_fixed.java | base64 -w0 > /tmp/cdf_b64.txt
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/ContainerDetailFragment.java \
  -X PUT -f message="Rewrite ContainerDetailFragment - remove all Wine references" \
  -f content="$(cat /tmp/cdf_b64.txt)" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3
echo "Fixed ContainerDetailFragment"

# ============================================
# FIX 4: Fix RootFSInstaller - use ZSTD decompression
# ============================================
echo "[4/5] Fixing RootFSInstaller..."

ROOTFS_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/xenvironment/RootFSInstaller.java --jq '.sha' 2>&1)

# Write the correct RootFSInstaller
cat > /tmp/RootFSInstaller.java << 'EOF4'
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
EOF4

cat /tmp/RootFSInstaller.java | base64 -w0 > /tmp/rfsi_b64.txt
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/xenvironment/RootFSInstaller.java \
  -X PUT -f message="Fix RootFSInstaller - use ZSTD decompression" \
  -f content="$(cat /tmp/rfsi_b64.txt)" -f sha="$ROOTFS_SHA" -f branch="main" 2>&1 | head -3
echo "Fixed RootFSInstaller"

# ============================================
# FIX 5: Fix SettingsFragment - remove Wine references
# ============================================
echo "[5/5] Fixing SettingsFragment..."

curl -sL "$ORIGINAL_REPO/SettingsFragment.java" -o /tmp/SettingsFragment_original.java
echo "Downloaded original: $(wc -c < /tmp/SettingsFragment_original.java) bytes"

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

# Remove Box64 version spinner
content = re.sub(
    r'        final Spinner sBox64Version = .*?\n.*?sBox64Version\..*?\n.*?\n.*?\n',
    '', content, flags=re.DOTALL
)

# Remove Box64 preset spinner
content = re.sub(
    r'        final Spinner sBox64Preset = .*?\n.*?sBox64Preset\..*?\n.*?\n.*?\n',
    '', content, flags=re.DOTALL
)

# Remove openAndroidBrowserFromWine checkbox
content = re.sub(
    r'        final CheckBox cbOpenAndroidBrowserFromWine.*?\n.*?\n.*?\n',
    '', content, flags=re.DOTALL
)

# Remove useAndroidClipboardOnWine checkbox
content = re.sub(
    r'        final CheckBox cbUseAndroidClipboardOnWine.*?\n.*?\n.*?\n',
    '', content, flags=re.DOTALL
)

# Remove duplicate blank lines
content = re.sub(r'\n{3,}', '\n\n', content)

with open('/tmp/SettingsFragment_fixed.java', 'w') as f:
    f.write(content)

print(f"Fixed SettingsFragment: {len(content)} chars")
PYEOF3

FIXED_SHA=$(gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/SettingsFragment.java --jq '.sha' 2>&1)
cat /tmp/SettingsFragment_fixed.java | base64 -w0 > /tmp/sf_b64.txt
gh api repos/maybeagoodguy60-droid/winlator-app/contents/app/src/main/java/com/winlator/SettingsFragment.java \
  -X PUT -f message="Fix SettingsFragment - remove Wine references" \
  -f content="$(cat /tmp/sf_b64.txt)" -f sha="$FIXED_SHA" -f branch="main" 2>&1 | head -3
echo "Fixed SettingsFragment"

echo ""
echo "=== Phase 3 Complete ==="
