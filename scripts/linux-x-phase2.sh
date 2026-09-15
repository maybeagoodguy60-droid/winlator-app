#!/bin/bash
# Linux X Phase 2: XML Layout Updates
set -e

echo "=== Phase 2: XML Layout Updates ==="

# ============================================
# UPDATE STRINGS.XML - Remove Wine strings, add Linux strings
# ============================================
echo "[1/4] Updating strings.xml..."

# Remove Wine-specific strings
sed -i '/<string name="wine_configuration">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="enable_csmt">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="offscreen_rendering_mode">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="enable_strict_shader_math">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="video_memory_size">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="install_wine">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="unable_to_install_wine">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="unable_to_remove_this_wine_version">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="wine_version">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="removing_wine">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="wine_debug_channel">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="enable_wine_debug">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="do_you_want_to_remove_this_wine_version">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="windows_version">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="builtin_wine">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="native_windows">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="drive_c">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="box64_version">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="box64_logs">/d' app/src/main/res/values/strings.xml
sed -i '/<string name="save_mem_on_run_from_steam">/d' app/src/main/res/values/strings.xml

# Update remaining strings
sed -i 's|processor_affinity_32_bit_apps|processor_affinity_secondary|' app/src/main/res/values/strings.xml
sed -i 's|Processor Affinity (32-bit apps)|Processor Affinity (Secondary)|' app/src/main/res/values/strings.xml
sed -i 's|<string name="open_android_browser_from_linux">|<string name="open_file_manager_on_launch">Open File Manager on Launch</string>\n    <string name="open_android_browser_from_linux">|' app/src/main/res/values/strings.xml

echo "Updated strings.xml"

# ============================================
# UPDATE CONTAINER DETAIL FRAGMENT LAYOUT
# ============================================
echo "[2/4] Updating container_detail_fragment.xml..."

cat > /tmp/container_detail_updated.xml << 'XMLEOF'
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/name" />

            <EditText
                style="@style/EditText"
                android:id="@+id/ETName"
                android:inputType="textCapSentences" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal">

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="@string/screen_size" />

                    <Spinner
                        style="@style/ComboBox"
                        android:layout_width="match_parent"
                        android:id="@+id/SScreenSize"
                        android:entries="@array/screen_size_entries" />
                </LinearLayout>

                <LinearLayout
                    android:id="@+id/LLCustomScreenSize"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:visibility="gone">

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/width" />

                        <EditText
                            style="@style/EditText"
                            android:layout_width="78dp"
                            android:id="@+id/ETScreenWidth"
                            android:inputType="number" />
                    </LinearLayout>

                    <TextView
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:textStyle="bold"
                        android:textSize="18dp"
                        android:layout_gravity="bottom"
                        android:layout_marginLeft="2dp"
                        android:layout_marginRight="2dp"
                        android:layout_marginBottom="4dp"
                        android:text="x" />

                    <LinearLayout
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/height" />

                        <EditText
                            style="@style/EditText"
                            android:layout_width="78dp"
                            android:id="@+id/ETScreenHeight"
                            android:inputType="number" />
                    </LinearLayout>
                </LinearLayout>
            </LinearLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/graphics_driver" />

            <LinearLayout
                android:id="@+id/LLGraphicsDriver"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/audio_driver" />

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="horizontal"
                android:gravity="center_vertical">

                <Spinner
                    style="@style/ComboBox"
                    android:layout_width="0dp"
                    android:layout_weight="1"
                    android:id="@+id/SAudioDriver"
                    android:entries="@array/audio_driver_entries" />

                <ImageButton
                    style="@style/ListMenuButton"
                    android:id="@+id/BTAudioDriverConfig"
                    android:src="@drawable/icon_audio_settings" />
            </LinearLayout>

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/hud_mode" />

            <Spinner
                style="@style/ComboBox"
                android:id="@+id/SHUDMode"
                android:layout_width="match_parent"
                android:entries="@array/hud_mode_entries" />

            <com.google.android.material.tabs.TabLayout
                android:id="@+id/TabLayout"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="4dp"
                android:background="@drawable/tab_layout_background"
                app:tabTextColor="?attr/tabTextColor"
                app:tabGravity="center"
                app:tabMode="auto">

                <com.google.android.material.tabs.TabItem
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/linux_configuration" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/environment_variables" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="@string/advanced" />
            </com.google.android.material.tabs.TabLayout>

            <LinearLayout
                android:id="@+id/LLTabLinuxConfig"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone">

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp">

                    <LinearLayout style="@style/FieldSet">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/rootfs" />

                        <Spinner
                            style="@style/ComboBox"
                            android:id="@+id/SRootfsType"
                            android:layout_width="match_parent"
                            android:entries="@array/rootfs_type_entries" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/rootfs_path" />

                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                            android:orientation="horizontal">

                            <EditText
                                style="@style/EditText"
                                android:layout_width="0dp"
                                android:layout_weight="1"
                                android:id="@+id/ETRootfsPath"
                                android:hint="@string/browse"
                                android:inputType="textUri"
                                android:enabled="false" />

                            <ImageButton
                                style="@style/ListMenuButton"
                                android:id="@+id/BTBrowseRootfs"
                                android:src="@drawable/icon_folder_search" />
                        </LinearLayout>
                    </LinearLayout>

                    <TextView
                        style="@style/FieldSetLabel"
                        android:text="@string/rootfs" />
                </FrameLayout>

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp">

                    <LinearLayout style="@style/FieldSet">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/desktop_environment" />

                        <Spinner
                            style="@style/ComboBox"
                            android:id="@+id/SDesktopEnv"
                            android:layout_width="match_parent"
                            android:entries="@array/desktop_env_entries" />

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/launch_command" />

                        <EditText
                            style="@style/EditText"
                            android:id="@+id/ETLaunchCommand"
                            android:hint="auto-detect"
                            android:inputType="text" />
                    </LinearLayout>

                    <TextView
                        style="@style/FieldSetLabel"
                        android:text="@string/desktop" />
                </FrameLayout>
            </LinearLayout>

            <LinearLayout
                android:id="@+id/LLTabEnvVars"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone">

                <com.winlator.widget.EnvVarsView
                    android:id="@+id/EnvVarsView"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:paddingLeft="16dp"
                    android:paddingRight="16dp"/>

                <View style="@style/HorizontalLine" />

                <Button
                    style="@style/ButtonNeutral"
                    android:id="@+id/BTAddEnvVar"
                    android:layout_width="160dp"
                    android:layout_height="wrap_content"
                    android:layout_gravity="center_horizontal"
                    android:text="@string/add" />
            </LinearLayout>

            <LinearLayout
                android:id="@+id/LLTabAdvanced"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone">

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp">

                    <LinearLayout style="@style/FieldSet">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/linux_preset" />

                        <Spinner
                            style="@style/ComboBox"
                            android:id="@+id/SLinuxPreset"
                            android:layout_width="match_parent" />
                    </LinearLayout>

                    <TextView
                        style="@style/FieldSetLabel"
                        android:text="@string/preset" />
                </FrameLayout>

                <FrameLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp">

                    <LinearLayout style="@style/FieldSet">
                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="@string/cpu_governor" />

                        <Spinner
                            style="@style/ComboBox"
                            android:id="@+id/SCpuGovernor"
                            android:layout_width="match_parent"
                            android:entries="@array/cpu_governor_entries" />
                    </LinearLayout>

                    <TextView
                        style="@style/FieldSetLabel"
                        android:text="@string/system" />
                </FrameLayout>

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/processor_affinity"
                    android:layout_marginTop="8dp" />

                <com.winlator.widget.CPUListView
                    android:id="@+id/CPUListView"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:gravity="center_horizontal"
                    android:layout_marginTop="4dp" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <com.google.android.material.floatingactionbutton.FloatingActionButton
        android:id="@+id/BTConfirm"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="right|bottom"
        android:tint="#ffffff"
        android:src="@drawable/icon_confirm"
        android:layout_margin="16dp" />
</FrameLayout>
XMLEOF

echo "Created updated container_detail_fragment.xml"

# ============================================
# UPDATE ARRAYS.XML - Add new array entries
# ============================================
echo "[3/4] Updating arrays.xml..."

# Check if arrays.xml exists and add new entries
ARRAYS_FILE="app/src/main/res/values/arrays.xml"
if [ -f "$ARRAYS_FILE" ]; then
    # Add new array entries before closing </resources>
    sed -i 's|</resources>|    <string-array name="rootfs_type_entries">\
        <item>Debian</item>\
        <item>Ubuntu</item>\
        <item>Arch Linux</item>\
        <item>Alpine Linux</item>\
        <item>Fedora</item>\
        <item>Custom</item>\
    </string-array>\
\
    <string-array name="desktop_env_entries">\
        <item>Auto Detect</item>\
        <item>XFCE</item>\
        <item>LXQt</item>\
        <item>KDE Plasma</item>\
        <item>GNOME</item>\
        <item>MATE</item>\
        <item>Terminal Only</item>\
    </string-array>\
\
    <string-array name="cpu_governor_entries">\
        <item>ondemand</item>\
        <item>performance</item>\
        <item>powersave</item>\
        <item>conservative</item>\
        <item>schedutil</item>\
    </string-array>\
\
    <string-array name="linux_preset_entries">\
        <item>Balanced</item>\
        <item>Performance</item>\
        <item>Power Saving</item>\
        <item>Stability</item>\
        <item>Custom</item>\
    </string-array>\
</resources>|' "$ARRAYS_FILE"
    echo "Updated arrays.xml"
else
    echo "arrays.xml not found, skipping"
fi

# ============================================
# REMOVE WINE-SPECIFIC LAYOUTS
# ============================================
echo "[4/4] Removing Wine-specific layouts..."

# Remove Wine-specific dialog layouts
rm -f app/src/main/res/layout/dxvk_config_dialog.xml
rm -f app/src/main/res/layout/vkd3d_config_dialog.xml
rm -f app/src/main/res/layout/wined3d_config_dialog.xml
rm -f app/src/main/res/layout/wine_debug_channel_list_item.xml
rm -f app/src/main/res/layout/wine_install_dialog.xml
rm -f app/src/main/res/layout/box64_edit_preset_dialog.xml
rm -f app/src/main/res/layout/box64_env_var_list_item.xml
rm -f app/src/main/res/layout/wincomponent_list_item.xml
rm -f app/src/main/res/layout/drive_list_item.xml
rm -f app/src/main/res/layout/installed_wine_list_item.xml

echo "Removed Wine-specific layouts"

# ============================================
# CREATE NEW LINUX-SPECIFIC LAYOUTS
# ============================================
echo "Creating Linux-specific layouts..."

# Create rootfs_picker_dialog.xml
cat > app/src/main/res/layout/rootfs_picker_dialog.xml << 'XMLEOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/select_rootfs_source"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="16dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/BTDownloadFromRepo"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/download_from_repo"
        android:layout_marginBottom="8dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/BTImportFromFile"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/import_from_file"
        android:layout_marginBottom="8dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/BTUseExistingPath"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/use_existing_path" />
</LinearLayout>
XMLEOF

# Create de_install_prompt_dialog.xml
cat > app/src/main/res/layout/de_install_prompt_dialog.xml << 'XMLEOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/no_desktop_environment"
        android:textSize="18sp"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/no_de_detected"
        android:layout_marginBottom="16dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/BTInstallLxqt"
        style="@style/Widget.MaterialComponents.Button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/install_lxqt"
        android:layout_marginBottom="8dp" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/BTTerminalOnly"
        style="@style/Widget.MaterialComponents.Button.OutlinedButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/terminal_only" />
</LinearLayout>
XMLEOF

echo "Created Linux-specific layouts"

echo ""
echo "=== Phase 2 Complete ==="
