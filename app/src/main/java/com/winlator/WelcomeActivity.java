package com.winlator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.winlator.core.FileLogger;
import com.winlator.core.RootAccessHelper;

public class WelcomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            FileLogger.start(getApplicationContext());
        }
        catch (Throwable ignored) {
        }
        setContentView(R.layout.activity_welcome);

        LinearLayout capabilitiesList = findViewById(R.id.capabilitiesList);
        Button btnContinue = findViewById(R.id.btnContinue);
        Button btnGrantRoot = findViewById(R.id.btnGrantRoot);
        TextView rootStatus = findViewById(R.id.rootStatus);

        addCapability(capabilitiesList, "ARM64 Linux Desktop", true, "Debian, Alpine, Arch, Fedora & more");
        addCapability(capabilitiesList, "GPU Acceleration (VirGL)", true, "Hardware-accelerated rendering");
        addCapability(capabilitiesList, "Audio (ALSA)", true, "Native Linux audio output");
        addCapability(capabilitiesList, "Input Controls", true, "Gamepad, keyboard, touch");
        addCapability(capabilitiesList, "Multiple Containers", true, "Run different distros simultaneously");
        addCapability(capabilitiesList, "File Manager", true, "Browse and manage container files");

        boolean rootAvail = RootAccessHelper.isRootAvailable();
        boolean rootGranted = RootAccessHelper.isRootGrantedCached(this);

        if (rootGranted) {
            rootStatus.setText("Root: Granted");
            rootStatus.setTextColor(0xFF4CAF50);
            btnGrantRoot.setVisibility(View.GONE);
            addCapability(capabilitiesList, "Root Access", true, "Full system access available");
            addCapability(capabilitiesList, "Chroot Containers", true, "Native chroot without proot");
        } else if (rootAvail) {
            rootStatus.setText("Root: Available (optional)");
            rootStatus.setTextColor(0xFFFFC107);
            addCapability(capabilitiesList, "Root Access", false, "Tap below to grant (optional)");
            addCapability(capabilitiesList, "Chroot Containers", false, "Requires root access");
        } else {
            rootStatus.setText("Root: Not detected (not required)");
            rootStatus.setTextColor(0xFF9E9E9E);
            btnGrantRoot.setVisibility(View.GONE);
            addCapability(capabilitiesList, "Proot Containers", true, "Full Linux without root");
            addCapability(capabilitiesList, "Chroot Containers", false, "Requires rooted device");
        }

        btnGrantRoot.setOnClickListener((v) -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "echo granted"});
                process.waitFor();
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                String output = reader.readLine();
                if (output != null && output.contains("granted")) {
                    RootAccessHelper.setRootGranted(WelcomeActivity.this, true);
                    rootStatus.setText("Root: Granted");
                    rootStatus.setTextColor(0xFF4CAF50);
                    btnGrantRoot.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                rootStatus.setText("Root: Grant failed — continuing without root");
            }
        });

        btnContinue.setOnClickListener((v) -> {
            RootAccessHelper.setWelcomeShown(this);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void addCapability(LinearLayout container, String name, boolean available, String detail) {
        View view = getLayoutInflater().inflate(R.layout.item_capability, container, false);
        TextView tvName = view.findViewById(R.id.capabilityName);
        TextView tvDetail = view.findViewById(R.id.capabilityDetail);
        View statusDot = view.findViewById(R.id.statusDot);

        tvName.setText(name);
        tvDetail.setText(detail);
        statusDot.setBackgroundColor(available ? 0xFF4CAF50 : 0xFF9E9E9E);
        tvName.setAlpha(available ? 1.0f : 0.5f);

        container.addView(view);
    }
}
