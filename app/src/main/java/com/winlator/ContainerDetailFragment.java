package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.container.GraphicsDriverPicker;
import com.winlator.contentdialog.AddEnvVarDialog;
import com.winlator.contentdialog.AudioDriverConfigDialog;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.core.KeyValueSet;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.StringUtils;
import com.winlator.linux.LinuxContainer;
import com.winlator.linux.LinuxPreset;
import com.winlator.linux.LinuxPresetManager;
import com.winlator.linux.LinuxSessionLauncher;
import com.winlator.widget.CPUListView;
import com.winlator.widget.EnvVarsView;

import org.json.JSONException;
import org.json.JSONObject;

public class ContainerDetailFragment extends Fragment {
    private ContainerManager manager;
    private final int containerId;
    private LinuxContainer container;
    private PreloaderDialog preloaderDialog;
    private EnvVarsView envVarsView;
    private Spinner sRootfsSource;
    private EditText etRootfsPath;
    private TextView tvRootfsStatus;
    private Button BTBrowseRootfs;
    private GraphicsDriverPicker graphicsDriverPicker;
    private LinearLayout llTabEnvVars;
    private LinearLayout llTabAdvanced;
    private EditText etName;
    private Spinner sScreenSize;
    private Spinner sAudioDriver;
    private Spinner sHUDMode;
    private Spinner sDesktopEnv;
    private Spinner sCPUGovernor;
    private Spinner sStartupSelection;
    private Spinner sControlsProfile;
    private EditText etLaunchCommand;
    private CPUListView cpuListView;

    public ContainerDetailFragment() {
        this(0);
    }

    public ContainerDetailFragment(int containerId) {
        this.containerId = containerId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(isEditMode() ? R.string.edit_container : R.string.new_container);
    }

    public boolean isEditMode() {
        return container != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup root, @Nullable Bundle savedInstanceState) {
        final Context context = getContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final View view = inflater.inflate(R.layout.container_detail_fragment, root, false);
        manager = new ContainerManager(context);
        container = containerId > 0 ? (LinuxContainer)manager.getContainerById(containerId) : null;

        etName = view.findViewById(R.id.ETName);
        sScreenSize = view.findViewById(R.id.SScreenSize);
        sAudioDriver = view.findViewById(R.id.SAudioDriver);
        sHUDMode = view.findViewById(R.id.SHUDMode);
        sDesktopEnv = view.findViewById(R.id.SDesktopEnv);
        sCPUGovernor = view.findViewById(R.id.SCPUGovernor);
        sStartupSelection = view.findViewById(R.id.SStartupSelection);
        sControlsProfile = view.findViewById(R.id.SControlsProfile);
        etLaunchCommand = view.findViewById(R.id.ETLaunchCommand);
        cpuListView = view.findViewById(R.id.CPUListView);

        LinearLayout llGraphicsDriver = view.findViewById(R.id.LLGraphicsDriver);
        graphicsDriverPicker = new GraphicsDriverPicker(
            llGraphicsDriver,
            isEditMode() ? container.getGraphicsDriver() : Container.DEFAULT_AUDIO_DRIVER,
            isEditMode() ? container.getGraphicsDriverConfig() : ""
        );

        if (isEditMode()) {
            etName.setText(container.getName());
            AppUtils.setSpinnerSelectionFromIdentifier(sAudioDriver, container.getAudioDriver());
            sHUDMode.setSelection(container.getHUDMode());
            sDesktopEnv.setSelection(getDesktopEnvPosition(container.getDesktopEnv()));
            etLaunchCommand.setText(container.getLaunchCommand());
            sCPUGovernor.setSelection(getCPUGovernorPosition(container.getCpuGovernor()));
            sStartupSelection.setSelection(container.getStartupSelection());
        }
        else {
            etName.setText(getString(R.string.container)+"-"+manager.getNextContainerId());
        }

        loadScreenSizeSpinner(view, isEditMode() ? container.getScreenSize() : Container.DEFAULT_SCREEN_SIZE);
        envVarsView = createEnvVarsTab(view);

        cpuListView.setCheckedCPUList(isEditMode() ? container.getCPUList(true) : Container.getFallbackCPUList());

        sRootfsSource = view.findViewById(R.id.SRootfsSource);
        etRootfsPath = view.findViewById(R.id.ETRootfsPath);
        tvRootfsStatus = view.findViewById(R.id.TVRootfsStatus);
        BTBrowseRootfs = view.findViewById(R.id.BTBrowseRootfs);

        setupRootfsSourceSpinner();
        setupTabs(view);

        view.findViewById(R.id.BTAudioDriverConfig).setOnClickListener((v) -> {
            new AudioDriverConfigDialog(view.findViewById(R.id.SAudioDriver)).show();
        });

        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> {
            try {
                String name = etName.getText().toString();
                String screenSize = getScreenSize(view);
                String envVars = envVarsView.getEnvVars();
                String graphicsDriver = graphicsDriverPicker.getGraphicsDriver();
                String graphicsDriverConfig = graphicsDriverPicker.getGraphicsDriverConfig();
                String audioDriverConfig = "";
                String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
                byte hudMode = (byte)sHUDMode.getSelectedItemPosition();
                String cpuList = cpuListView.getCheckedCPUListAsString();
                byte startupSelection = (byte)sStartupSelection.getSelectedItemPosition();
                String desktopEnv = getDesktopEnvId(sDesktopEnv.getSelectedItemPosition());
                String launchCommand = etLaunchCommand.getText().toString().trim();
                String cpuGovernor = sCPUGovernor.getSelectedItem().toString();

                if (isEditMode()) {
                    container.setName(name);
                    container.setScreenSize(screenSize);
                    container.setEnvVars(envVars);
                    container.setCPUList(cpuList);
                    container.setGraphicsDriver(graphicsDriver);
                    container.setGraphicsDriverConfig(graphicsDriverConfig);
                    container.setAudioDriver(audioDriver);
                    container.setAudioDriverConfig(audioDriverConfig);
                    container.setHUDMode(hudMode);
                    container.setStartupSelection(startupSelection);
                    container.setDesktopEnv(desktopEnv);
                    container.setLaunchCommand(launchCommand);
                    container.setCpuGovernor(cpuGovernor);
                    container.saveData();
                }
                else {
                    container = new LinuxContainer(0);
                    container.setName(name);
                    container.setScreenSize(screenSize);
                    container.setEnvVars(envVars);
                    container.setCPUList(cpuList);
                    container.setGraphicsDriver(graphicsDriver);
                    container.setGraphicsDriverConfig(graphicsDriverConfig);
                    container.setAudioDriver(audioDriver);
                    container.setAudioDriverConfig(audioDriverConfig);
                    container.setHUDMode(hudMode);
                    container.setStartupSelection(startupSelection);
                    container.setDesktopEnv(desktopEnv);
                    container.setLaunchCommand(launchCommand);
                    container.setCpuGovernor(cpuGovernor);
                    container.setRootfsType(LinuxContainer.DEFAULT_ROOTFS_TYPE);

                    JSONObject data = new JSONObject();
                    data.put("name", name);
                    data.put("screenSize", screenSize);
                    data.put("envVars", envVars);
                    data.put("cpuList", cpuList);
                    data.put("graphicsDriver", graphicsDriver);
                    data.put("graphicsDriverConfig", graphicsDriverConfig);
                    data.put("audioDriver", audioDriver);
                    data.put("audioDriverConfig", audioDriverConfig);
                    data.put("hudMode", hudMode);
                    data.put("startupSelection", startupSelection);
                    data.put("desktopEnv", desktopEnv);
                    data.put("launchCommand", launchCommand);
                    data.put("cpuGovernor", cpuGovernor);
                    data.put("rootfsType", LinuxContainer.DEFAULT_ROOTFS_TYPE);

                    preloaderDialog.show(R.string.creating_container);
                    manager.createContainerAsync(data, (createdContainer) -> {
                        if (createdContainer != null) {
                            this.container = (LinuxContainer)createdContainer;
                        }
                        preloaderDialog.close();
                    });
                }

                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
            catch (JSONException e) {}
        });

        return view;
    }

    private void setupTabs(final View view) {
        llTabEnvVars = view.findViewById(R.id.LLTabEnvVars);
        llTabAdvanced = view.findViewById(R.id.LLTabAdvanced);

        final LinearLayout[] tabs = {llTabEnvVars, llTabAdvanced};
        TabLayout tabLayout = view.findViewById(R.id.TabLayout);

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    for (int j = 0; j < tabs.length; j++) {
                        tabs[j].setVisibility(j == index ? View.VISIBLE : View.GONE);
                    }
                }
                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}
                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    public static String getScreenSize(View view) {
        Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        String value = sScreenSize.getSelectedItem().toString();
        if (sScreenSize.getSelectedItemPosition() == 0) {
            value = Container.DEFAULT_SCREEN_SIZE;
            String strWidth = ((EditText)view.findViewById(R.id.ETScreenWidth)).getText().toString().trim();
            String strHeight = ((EditText)view.findViewById(R.id.ETScreenHeight)).getText().toString().trim();
            if (strWidth.matches("[0-9]+") && strHeight.matches("[0-9]+")) {
                int width = Integer.parseInt(strWidth);
                int height = Integer.parseInt(strHeight);
                if ((width % 2) == 0 && (height % 2) == 0) return width+"x"+height;
            }
        }
        return StringUtils.parseIdentifier(value);
    }

    public static void loadScreenSizeSpinner(View view, String selectedValue) {
        final Spinner sScreenSize = view.findViewById(R.id.SScreenSize);
        final LinearLayout llCustomScreenSize = view.findViewById(R.id.LLCustomScreenSize);
        sScreenSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                llCustomScreenSize.setVisibility(sScreenSize.getSelectedItemPosition() == 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        boolean found = AppUtils.setSpinnerSelectionFromIdentifier(sScreenSize, selectedValue);
        if (!found) {
            sScreenSize.setSelection(0);
            String[] screenSize = selectedValue.split("x");
            ((EditText)view.findViewById(R.id.ETScreenWidth)).setText(screenSize[0]);
            ((EditText)view.findViewById(R.id.ETScreenHeight)).setText(screenSize[1]);
        }
    }

    private EnvVarsView createEnvVarsTab(final View view) {
        final Context context = view.getContext();
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);
        envVarsView.setEnvVars(new EnvVars(isEditMode() ? container.getEnvVars() : Container.DEFAULT_ENV_VARS));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) -> (new AddEnvVarDialog(context, envVarsView)).show());
        return envVarsView;
    }

    private int getDesktopEnvPosition(String desktopEnv) {
        if (desktopEnv == null) return 0;
        switch (desktopEnv) {
            case "auto": return 0;
            case "xfce": return 1;
            case "lxqt": return 2;
            case "kde": return 3;
            case "gnome": return 4;
            case "mate": return 5;
            case "terminal": return 6;
            default: return 0;
        }
    }

    private String getDesktopEnvId(int position) {
        switch (position) {
            case 0: return "auto";
            case 1: return "xfce";
            case 2: return "lxqt";
            case 3: return "kde";
            case 4: return "gnome";
            case 5: return "mate";
            case 6: return "terminal";
            default: return "auto";
        }
    }

    private int getCPUGovernorPosition(String cpuGovernor) {
        if (cpuGovernor == null) return 0;
        switch (cpuGovernor) {
            case "ondemand": return 0;
            case "performance": return 1;
            case "powersave": return 2;
            case "conservative": return 3;
            case "schedutil": return 4;
            default: return 0;
        }
    }

    private void setupRootfsSourceSpinner() {
        String[] sources = getResources().getStringArray(R.array.rootfs_source_entries);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, sources);
        sRootfsSource.setAdapter(adapter);

        // Set current value
        if (isEditMode()) {
            String rootfsPath = container.getRootfsPath();
            if (rootfsPath != null && !rootfsPath.isEmpty()) {
                etRootfsPath.setText(rootfsPath);
                tvRootfsStatus.setText(getString(R.string.rootfs_validate_ok));
                tvRootfsStatus.setTextColor(0xFF4CAF50);
            }
        }

        sRootfsSource.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String source = sources[position];
                if (position == 0) { // Bundled
                    etRootfsPath.setEnabled(false);
                    etRootfsPath.setText("/data/data/com.winlator/files/rootfs");
                    BTBrowseRootfs.setEnabled(false);
                    validateRootfsPath("/data/data/com.winlator/files/rootfs");
                } else if (position == 1) { // Import from file
                    etRootfsPath.setEnabled(false);
                    etRootfsPath.setText("Tap Browse to select");
                    BTBrowseRootfs.setEnabled(true);
                    tvRootfsStatus.setText("Select a .tar.gz rootfs file");
                    tvRootfsStatus.setTextColor(0xFFFFC107);
                } else { // Existing path
                    etRootfsPath.setEnabled(true);
                    etRootfsPath.setText("");
                    BTBrowseRootfs.setEnabled(true);
                    tvRootfsStatus.setText(getString(R.string.rootfs_select_hint));
                    tvRootfsStatus.setTextColor(0xFF888888);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        BTBrowseRootfs.setOnClickListener((v) -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select Rootfs"), 1001);
        });
    }

    private void validateRootfsPath(String path) {
        File rootDir = new File(path);
        if (rootDir.isDirectory() && new File(rootDir, "/bin/bash").exists()) {
            tvRootfsStatus.setText(getString(R.string.rootfs_validate_ok));
            tvRootfsStatus.setTextColor(0xFF4CAF50);
        } else if (rootDir.isDirectory() && new File(rootDir, "/etc/os-release").exists()) {
            tvRootfsStatus.setText(getString(R.string.rootfs_validate_ok));
            tvRootfsStatus.setTextColor(0xFF4CAF50);
        } else {
            tvRootfsStatus.setText(getString(R.string.rootfs_validate_fail));
            tvRootfsStatus.setTextColor(0xFFFF5252);
        }
    }
}
