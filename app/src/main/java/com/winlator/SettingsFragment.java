package com.winlator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.app.NotificationManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;

import com.winlator.container.Container;
import com.winlator.container.ContainerManager;
import com.winlator.contentdialog.ContentDialog;
import com.winlator.contentdialog.GamepadPlayerConfigDialog;
import com.winlator.contentdialog.SoundFontTestDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.DefaultVersion;
import com.winlator.core.FileUtils;
import com.winlator.core.GeneralComponents;
import com.winlator.core.LocaleHelper;
import com.winlator.core.PreloaderDialog;

import com.winlator.inputcontrols.ExternalController;
import com.winlator.services.NotificationUtils;
import com.winlator.widget.ColorPickerView;
import com.winlator.widget.LogView;
import com.winlator.widget.SeekBar;

import java.util.ArrayList;

public class SettingsFragment extends Fragment {
    public static final byte APP_THEME_LIGHT = 0;
    public static final byte APP_THEME_DARK = 1;
    private PreloaderDialog preloaderDialog;
    private SharedPreferences preferences;
    private boolean midiDeviceCallbackRegistered = false;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        preloaderDialog = new PreloaderDialog(getActivity());
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((AppCompatActivity)getActivity()).getSupportActionBar().setTitle(R.string.settings);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        final Context context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        loadCursorControls(view);
        loadGamepadControls(view);
        loadGamepadPlayerConfigs(view);
        loadLanguageAndThemeControls(view);
        loadSystemCheckBoxes(view);

        final Spinner sSoundFont = view.findViewById(R.id.SSoundFont);
        String soundfont = preferences.getString("soundfont", null);
        GeneralComponents.initViews(GeneralComponents.Type.SOUNDFONT, view.findViewById(R.id.SoundFontToolbox), sSoundFont, soundfont, DefaultVersion.SOUNDFONT);
        view.findViewById(R.id.BTSoundFontTest).setOnClickListener((v) -> (new SoundFontTestDialog(context, sSoundFont.getSelectedItem().toString())).show());

        final Spinner sMIDIInputDevice = view.findViewById(R.id.SMIDIInputDevice);
        String midiInputDevice = preferences.getString("midi_input_device", "auto");
        loadMIDIInputDeviceSpinner(sMIDIInputDevice, midiInputDevice);

        view.findViewById(R.id.BTConfirm).setOnClickListener((v) -> onConfirmClicked(view));
        return view;
    }
    private void loadCursorControls(View view) {
        SeekBar sbCursorSpeed = view.findViewById(R.id.SBCursorSpeed);
        sbCursorSpeed.setValue(preferences.getFloat("cursor_speed", 1.0f) * 10f);

        SeekBar sbCursorSize = view.findViewById(R.id.SBCursorSize);
        sbCursorSize.setValue(preferences.getFloat("cursor_scale", 1.0f) * 100f);

        ColorPickerView cpvCursorColor = view.findViewById(R.id.CPVCursorColor);
        cpvCursorColor.setColor(preferences.getInt("cursor_color", 0xffffff));

        ((android.widget.CheckBox)view.findViewById(R.id.CBMoveCursorToTouchpoint)).setChecked(preferences.getBoolean("move_cursor_to_touchpoint", false));
        ((android.widget.CheckBox)view.findViewById(R.id.CBCapturePointerOnExternalMouse)).setChecked(preferences.getBoolean("capture_pointer_on_external_mouse", true));
    }
    private void loadGamepadControls(final View view) {
        final Spinner sGamepadModel = view.findViewById(R.id.SGamepadModel);
        ArrayList<String> items = new ArrayList<>();
        items.add(getContext().getString(R.string.auto));
        for (ExternalController controller : ExternalController.getControllers()) items.add(controller.getName());

        String selectedName = preferences.getString("gamepad_model", "");
        if (!selectedName.isEmpty() && !items.contains(selectedName)) items.add(selectedName);
        sGamepadModel.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, items));
        AppUtils.setSpinnerSelectionFromValue(sGamepadModel, selectedName);
    }
    private void loadLanguageAndThemeControls(View view) {
        final Spinner sLanguage = view.findViewById(R.id.SLanguage);
        sLanguage.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.language_entries)));
        sLanguage.setSelection(LocaleHelper.getLocaleIndex(getContext()), false);

        RadioGroup rgAppTheme = view.findViewById(R.id.RGAppTheme);
        rgAppTheme.check(preferences.getInt("app_theme", APP_THEME_DARK) == APP_THEME_LIGHT ? R.id.RBLight : R.id.RBDark);
    }
    private void loadSystemCheckBoxes(View view) {
        ((android.widget.CheckBox)view.findViewById(R.id.CBUseAndroidClipboardOnWine)).setChecked(preferences.getBoolean("use_winlator_clipboard", false));
        ((android.widget.CheckBox)view.findViewById(R.id.CBEnableBackgroundProtection)).setChecked(preferences.getBoolean("enable_background_protection", false));
        ((android.widget.CheckBox)view.findViewById(R.id.CBEnableBackgroundWakelock)).setChecked(preferences.getBoolean("enable_background_wakelock", false));
        ((android.widget.CheckBox)view.findViewById(R.id.CBSaveMemOnRunFromSteam)).setChecked(preferences.getBoolean("save_mem_on_run_from_steam", false));
    }
    private void onConfirmClicked(View view) {
        int oldTheme = preferences.getInt("app_theme", APP_THEME_DARK);
        int oldLanguageIndex = preferences.getInt("lc_index", -1);
        SharedPreferences.Editor editor = preferences.edit();

        SeekBar sbCursorSpeed = view.findViewById(R.id.SBCursorSpeed);
        editor.putFloat("cursor_speed", sbCursorSpeed.getValue() / 10.0f);

        SeekBar sbCursorSize = view.findViewById(R.id.SBCursorSize);
        editor.putFloat("cursor_scale", sbCursorSize.getValue() / 100.0f);

        ColorPickerView cpvCursorColor = view.findViewById(R.id.CPVCursorColor);
        editor.putInt("cursor_color", cpvCursorColor.getColor());

        editor.putBoolean("move_cursor_to_touchpoint", ((android.widget.CheckBox)view.findViewById(R.id.CBMoveCursorToTouchpoint)).isChecked());
        editor.putBoolean("capture_pointer_on_external_mouse", ((android.widget.CheckBox)view.findViewById(R.id.CBCapturePointerOnExternalMouse)).isChecked());

        Spinner sGamepadModel = view.findViewById(R.id.SGamepadModel);
        String gamepadModel = (sGamepadModel.getSelectedItemPosition() > 0 ? sGamepadModel.getSelectedItem().toString() : "");
        if (!gamepadModel.isEmpty()) editor.putString("gamepad_model", gamepadModel);
        else editor.remove("gamepad_model");

        putGamepadPlayerConfigs(view, editor);

        Spinner sLanguage = view.findViewById(R.id.SLanguage);
        editor.putInt("lc_index", sLanguage.getSelectedItemPosition());

        RadioGroup rgAppTheme = view.findViewById(R.id.RGAppTheme);
        editor.putInt("app_theme", rgAppTheme.getCheckedRadioButtonId() == R.id.RBLight ? APP_THEME_LIGHT : APP_THEME_DARK);

        editor.putBoolean("use_winlator_clipboard", ((android.widget.CheckBox)view.findViewById(R.id.CBUseAndroidClipboardOnWine)).isChecked());
        editor.putBoolean("enable_background_protection", ((android.widget.CheckBox)view.findViewById(R.id.CBEnableBackgroundProtection)).isChecked());
        editor.putBoolean("enable_background_wakelock", ((android.widget.CheckBox)view.findViewById(R.id.CBEnableBackgroundWakelock)).isChecked());
        editor.putBoolean("save_mem_on_run_from_steam", ((android.widget.CheckBox)view.findViewById(R.id.CBSaveMemOnRunFromSteam)).isChecked());

        editor.apply();
        Toast toast = Toast.makeText(getContext(), R.string.settings_saved, Toast.LENGTH_SHORT);
        toast.show();

        int newLanguageIndex = preferences.getInt("lc_index", oldLanguageIndex);
        int newTheme = preferences.getInt("app_theme", oldTheme);
        if (newLanguageIndex != oldLanguageIndex || newTheme != oldTheme) {
            AppUtils.restartApplication(getContext());
        }
    }
    private void loadMIDIInputDeviceSpinner(final Spinner sMIDIInputDevice, final String selectedValue) {
        Context context = getContext();
        MidiManager mm = (MidiManager)context.getSystemService(Context.MIDI_SERVICE);
        MidiDeviceInfo[] infos = mm.getDevices();

        if (!midiDeviceCallbackRegistered) {
            midiDeviceCallbackRegistered = true;
            mm.registerDeviceCallback(new MidiManager.DeviceCallback() {
                @Override
                public void onDeviceAdded(MidiDeviceInfo device) {
                    loadMIDIInputDeviceSpinner(sMIDIInputDevice, selectedValue);
                }

                @Override
                public void onDeviceRemoved(MidiDeviceInfo device) {
                    loadMIDIInputDeviceSpinner(sMIDIInputDevice, selectedValue);
                }
            }, new Handler(Looper.getMainLooper()));
        }

        ArrayList<String> items = new ArrayList<>();
        items.add(context.getString(R.string.none));
        items.add(context.getString(R.string.auto));

        for (MidiDeviceInfo info : infos) {
            if (info.getOutputPortCount() > 0) {
                Bundle properties = info.getProperties();
                items.add(properties.getString(MidiDeviceInfo.PROPERTY_NAME));
            }
        }

        sMIDIInputDevice.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, items));

        if (selectedValue.equals("none")) {
            sMIDIInputDevice.setSelection(0, false);
        }
        else if (selectedValue.equals("auto") || !AppUtils.setSpinnerSelectionFromValue(sMIDIInputDevice, selectedValue)) {
            sMIDIInputDevice.setSelection(1, false);
        }
    }
    private void loadGamepadPlayerConfigs(View view) {
        LinearLayout container = view.findViewById(R.id.LLGamepadPlayer);
        view.findViewById(R.id.BTResetGamepadPlayerConfigs).setOnClickListener((v) -> {
            ContentDialog.confirm(v.getContext(), R.string.do_you_want_to_reset_configurations, () -> {
                for (int i = 0; i < container.getChildCount(); i++) container.getChildAt(i).setTag("");
            });
        });

        for (int i = 0; i < container.getChildCount(); i++) {
            final View child = container.getChildAt(i);
            child.setTag(preferences.getString("gamepad_player"+i, ""));
            final byte slot = (byte)i;
            child.setOnClickListener((v) -> (new GamepadPlayerConfigDialog(child, slot)).show());
        }
    }
    private void putGamepadPlayerConfigs(View view, SharedPreferences.Editor editor) {
        LinearLayout container = view.findViewById(R.id.LLGamepadPlayer);
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            String config = child.getTag().toString();
            String key = "gamepad_player"+i;
            if (!config.isEmpty()) {
                editor.putString(key, child.getTag().toString());
            }
            else editor.remove(key);
        }
    }
}
