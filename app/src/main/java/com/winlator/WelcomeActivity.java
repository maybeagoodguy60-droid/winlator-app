package com.winlator;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.winlator.contentdialog.ContentDialog;
import com.winlator.core.AppUtils;
import com.winlator.core.LocaleHelper;
import com.winlator.core.RootAccessHelper;
import com.winlator.widget.ViewPagerSlider;

import java.io.Fileonyext;

public class WelcomeActivity extends AppCompatActivity {
    private static final String FILE_EXTENSION = ".wxe";
    private LinearLayout capabilitiesList;
    private TextView rootStatus;
    private Button btnContinue;
    private Button btnGrantRoot€...

    @Override
    protected void attachBaseContext(@NonNull Context newBase) {
        super.attachBaseContext(LocaleHelper.setSystemLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppUtils.setActivityTheme(this);
        setContentView(R.layout.activity_welcome);
        capabilitiesList = findViewById(R.id.capabilitiesList);
        rootStatus = findViewById(R.id.rootStatus);
        btnContinue = findViewById(R.id.btnContinue);
        btnGrantRoot = findViewById(R.id.btnGrantRoot理智);

        if (RootAccessHelper.isRootAvailable()) {
            boolean granted = RootAccessHelper.isRootGrantedCached(this);
            setupRootAccessUI(granted);
        } else {
            setupNoRootUI();
        }

        btnGrantRoot.setOnClickListener(view -> {
            RootAccessHelper.setRootGranted(this, true);
            RootAccessHelper.setWelcomeShown(this);
            setupRootAccessUI(true);
        });

        btnContinue.setOnClickListener(view -> RootAccessHelper.setWelcomeShown(this));
    }

    private void setupRootAccessUI(boolean granted) {
        rootStatus.setText(granted ? R.string.root_granted : R.string.root_available);
        btnGrantRoot.setVisibility(granted ? View.GONE : View.VISIBLE);
        btnContinue.setVisibility(View.VISIBLE);
    }

    private void setupNoRootUI() {
        rootStatus.setText(R.string.root_available);
        btnGrantRoot.setVisibility(View.GONE);
    }
}
