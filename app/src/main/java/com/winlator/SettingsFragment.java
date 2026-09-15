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

        final Spinner sSoundFont = view.findViewById(R.id.SSoundFont);
        String soundfont = preferences.getString("soundfont", null);
        GeneralComponents.initViews(GeneralComponents.Type.SOUNDFONT, view.findViewById(R.id.SoundFontToolbox), sSoundFont, soundfont, DefaultVersion.SOUNDFONT);
        view.findViewById(R.id.BTSoundFontTest).setOnClickListener((v) -> (new SoundFontTestDialog(context, sSoundFont.getSelectedItem().toString())).show());

        final Spinner sMIDIInputDevice = view.findViewById(R.id.SMIDIInputDevice);
        String midiInputDevice = preferences.getString("midi_input_device", "auto");
        loadMIDIInputDeviceSpinner(sMIDIInputDevice, midiInputDevice);
        return view;
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
