package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;

import com.winlator.container.GraphicsDrivers;
import com.winlator.contentdialog.AddEnvVarDialog;
import com.winlator.contentdialog.AudioDriverConfigDialog;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.VortekConfigDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.Callback;

import com.winlator.core.EnvVars;
import com.winlator.core.FileUtils;
import com.winlator.container.GraphicsDriverPicker;
import com.winlator.linux.LinuxSessionLauncher;
import com.winlator.core.KeyValueSet;
import com.winlator.core.PreloaderDialog;
import com.winlator.core.StringUtils;

import com.winlator.widget.CPUListView;
import com.winlator.widget.ColorPickerView;
import com.winlator.widget.EnvVarsView;
import com.winlator.widget.FrameRating;
import com.winlator.widget.ImagePickerView;
import com.winlator.widget.SeekBar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ContainerDetailFragment extends Fragment {
    private ContainerManager manager;
    private final int containerId;
    private Container container;
    private PreloaderDialog preloaderDialog;
    private Callback<String> openDirectoryCallback;

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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MainActivity.OPEN_DIRECTORY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String path = FileUtils.getFilePathFromUri(data.getData());
                if (path != null && openDirectoryCallback != null) openDirectoryCallback.call(path);
            }
            openDirectoryCallback = null;
        }
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
        container = containerId > 0 ? manager.getContainerById(containerId) : null;

        final EditText etName = view.findViewById(R.id.ETName);

        if (isEditMode()) {
            etName.setText(container.getName());
        }
        else etName.setText(getString(R.string.container)+"-"+manager.getNextContainerId());

        }, R.id.LLTabWineConfiguration, R.id.LLTabWinComponents, R.id.LLTabEnvVars, R.id.LLTabDrives, R.id.LLTabAdvanced);

        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> {
            try {
                String name = etName.getText().toString();
                String screenSize = getScreenSize(view);
                String envVars = envVarsView.getEnvVars();
                String graphicsDriver = graphicsDriverPicker.getGraphicsDriver();
                String graphicsDriverConfig = graphicsDriverPicker.getGraphicsDriverConfig();
                String audioDriverConfig = vAudioDriverConfig.getTag().toString();
                String audioDriver = StringUtils.parseIdentifier(sAudioDriver.getSelectedItem());
                String wincomponents = getWinComponents(view);
                String drives = getDrives(view);
                byte hudMode = (byte)sHUDMode.getSelectedItemPosition();
                String cpuList = cpuListView.getCheckedCPUListAsString();
                String cpuListWoW64 = cpuListViewWoW64.getCheckedCPUListAsString();
                byte startupSelection = (byte)sStartupSelection.getSelectedItemPosition();

                if (isEditMode()) {
                    container.setName(name);
                    container.setScreenSize(screenSize);
                    container.setEnvVars(envVars);
                    container.setCPUList(cpuList);
                    container.setCPUListWoW64(cpuListWoW64);
                    container.setGraphicsDriver(graphicsDriver);
                    container.setGraphicsDriverConfig(graphicsDriverConfig);
                    container.setAudioDriver(audioDriver);
                    container.setAudioDriverConfig(audioDriverConfig);
                    container.setWinComponents(wincomponents);
                    container.setDrives(drives);
                    container.setHUDMode(hudMode);
                    container.setStartupSelection(startupSelection);
                    container.saveData();

                                getActivity().onBackPressed();
                }
                else {
                    JSONObject data = new JSONObject();
                    data.put("name", name);
                    data.put("screenSize", screenSize);
                    data.put("envVars", envVars);
                    data.put("cpuList", cpuList);
                    data.put("cpuListWoW64", cpuListWoW64);
                    data.put("graphicsDriver", graphicsDriver);
                    data.put("graphicsDriverConfig", graphicsDriverConfig);
                    data.put("audioDriver", audioDriver);
                    data.put("audioDriverConfig", audioDriverConfig);
                    data.put("hudMode", hudMode);
                    data.put("startupSelection", startupSelection);

                    if (wineInfos.size() > 1) {
                        }

                    preloaderDialog.show(R.string.creating_container);
                    manager.createContainerAsync(data, (container) -> {
                        if (container != null) {
                            this.container = container;
                                }
                        preloaderDialog.close();
                        getActivity().onBackPressed();
                    });
                }
            }
            catch (JSONException e) {}
        });
        return view;
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

    public static String getWinComponents(View view) {
        ViewGroup parent = view.findViewById(R.id.LLTabWinComponents);
        ArrayList<View> views = new ArrayList<>();
        AppUtils.findViewsWithClass(parent, Spinner.class, views);
        String[] wincomponents = new String[views.size()];

        for (int i = 0; i < views.size(); i++) {
            Spinner spinner = (Spinner)views.get(i);
            wincomponents[i] = spinner.getTag()+"="+spinner.getSelectedItemPosition();
        }
        return String.join(",", wincomponents);
    }

    public static void createWinComponentsTab(View view, String wincomponents) {
        Context context = view.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        ViewGroup tabView = view.findViewById(R.id.LLTabWinComponents);
        ViewGroup directxSectionView = tabView.findViewById(R.id.LLWinComponentsDirectX);
        ViewGroup generalSectionView = tabView.findViewById(R.id.LLWinComponentsGeneral);

        for (String[] wincomponent : new KeyValueSet(wincomponents)) {
            final String name = wincomponent[0];
            ViewGroup parent = name.startsWith("direct") || name.startsWith("x") ? directxSectionView : generalSectionView;
            View itemView = inflater.inflate(R.layout.wincomponent_list_item, parent, false);
            ((TextView)itemView.findViewById(R.id.TextView)).setText(StringUtils.getString(context, name));
            Spinner spinner = itemView.findViewById(R.id.Spinner);
            spinner.setSelection(Integer.parseInt(wincomponent[1]), false);
            spinner.setTag(name);
            parent.addView(itemView);
        }
    }

    private EnvVarsView createEnvVarsTab(final View view) {
        final Context context = view.getContext();
        final EnvVarsView envVarsView = view.findViewById(R.id.EnvVarsView);
        envVarsView.setEnvVars(new EnvVars(isEditMode() ? container.getEnvVars() : Container.DEFAULT_ENV_VARS));
        view.findViewById(R.id.BTAddEnvVar).setOnClickListener((v) -> (new AddEnvVarDialog(context, envVarsView)).show());
        return envVarsView;
    }

    private void showDriveSearchPopupMenu(View anchorView, final Drive drive, final EditText editText) {
        final FragmentActivity activity = getActivity();
        final Fragment $this = ContainerDetailFragment.this;

        PopupMenu popupMenu = new PopupMenu(activity, anchorView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) popupMenu.setForceShowIcon(true);
        popupMenu.inflate(R.menu.drive_search_popup_menu);
        Menu menu = popupMenu.getMenu();
        SubMenu subMenu = menu.findItem(R.id.menu_item_locations).getSubMenu();
        ArrayList<Container> containers = manager.getContainers();
        for (int i = 0; i < containers.size(); i++) {
            Container container = containers.get(i);
            subMenu.add(0, 0, container.id, container.getName()+" (Drive C:)");
        }

        popupMenu.setOnMenuItemClickListener((menuItem) -> {
            int itemId = menuItem.getItemId();
            switch (itemId) {
                case R.id.menu_item_open_directory:
                    openDirectoryCallback = (path) -> {
                        drive.path = path;
                        editText.setText(path);
                    };

                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.fromFile(Environment.getExternalStorageDirectory()));
                    activity.startActivityFromFragment($this, intent, MainActivity.OPEN_DIRECTORY_REQUEST_CODE);
                    break;
                case R.id.menu_item_downloads:
                    drive.path = AppUtils.DIRECTORY_DOWNLOADS;
                    editText.setText(AppUtils.DIRECTORY_DOWNLOADS);
                    break;
                case R.id.menu_item_internal_storage:
                    drive.path = AppUtils.INTERNAL_STORAGE;
                    editText.setText(AppUtils.INTERNAL_STORAGE);
                    break;
                default:
                    Container container = manager.getContainerById(menuItem.getOrder());
                    if (container != null) {
                        String path = container.getRootDir()+"/.wine/drive_c";
                        drive.path = path;
                        editText.setText(path);
                    }
                    break;
            }
            return true;
        });

        popupMenu.show();
    }

}
