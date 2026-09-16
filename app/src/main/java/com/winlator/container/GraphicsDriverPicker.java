package com.winlator.container;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.winlator.contentdialog.TurnipConfigDialog;
import com.winlator.contentdialog.VirGLConfigDialog;
import com.winlator.contentdialog.VortekConfigDialog;
import com.winlator.core.KeyValueSet;
import com.winlator.core.StringUtils;
import com.winlator.widget.TaggedSelectionBox;

public class GraphicsDriverPicker {
    private final LinearLayout container;

    public GraphicsDriverPicker(LinearLayout container, String selectedGraphicsDriver, String graphicsDriverConfig) {
        this(container, false, selectedGraphicsDriver, graphicsDriverConfig);
    }

    public GraphicsDriverPicker(LinearLayout container, boolean linuxMode, String selectedGraphicsDriver, String graphicsDriverConfig) {
        this.container = container;
        final Context context = container.getContext();
        container.removeAllViews();
        String[] identifiers = GraphicsDrivers.parseIdentifiers(selectedGraphicsDriver);
        KeyValueSet[] configs = GraphicsDrivers.parseConfigs(selectedGraphicsDriver, graphicsDriverConfig);

        final String[] apiNames = {"Vulkan", "OpenGL"};
        for (int i = 0; i < apiNames.length; i++) {
            final TaggedSelectionBox taggedSelectionBox = new TaggedSelectionBox(context);
            taggedSelectionBox.setLabel(apiNames[i]);
            String[] items = GraphicsDrivers.getItems(apiNames[i], linuxMode);
            taggedSelectionBox.setItems(items);
            String selected = GraphicsDrivers.getName(identifiers[i]);
            if (linuxMode && !GraphicsDrivers.isLinuxCompatible(identifiers[i])) selected = items.length > 0 ? items[0] : "";
            taggedSelectionBox.setSelectedItem(selected);
            taggedSelectionBox.setTag(configs[i].toString());
            taggedSelectionBox.setOnButtonClickListener(() -> {
                final String graphicsDriver = StringUtils.parseIdentifier(taggedSelectionBox.getSelectedItem());
                if (linuxMode && !GraphicsDrivers.isLinuxCompatible(graphicsDriver)) return;
                showGraphicsDriverConfigDialog(graphicsDriver, taggedSelectionBox);
            });
            container.addView(taggedSelectionBox);
        }
    }

    public String getGraphicsDriver() {
        StringBuilder graphicsDriver = new StringBuilder();
        for (int i = 0; i < container.getChildCount(); i++) {
            TaggedSelectionBox taggedSelectionBox = (TaggedSelectionBox)container.getChildAt(i);
            if (graphicsDriver.length() > 0) graphicsDriver.append(',');
            graphicsDriver.append(StringUtils.parseIdentifier(taggedSelectionBox.getSelectedItem()));
        }
        return graphicsDriver.toString();
    }

    public String getGraphicsDriverConfig() {
        StringBuilder graphicsDriverConfig = new StringBuilder();
        for (int i = 0; i < container.getChildCount(); i++) {
            TaggedSelectionBox taggedSelectionBox = (TaggedSelectionBox)container.getChildAt(i);
            if (graphicsDriverConfig.length() > 0) graphicsDriverConfig.append('|');
            graphicsDriverConfig.append(taggedSelectionBox.getTag().toString());
        }
        return graphicsDriverConfig.toString();
    }

    private static void showGraphicsDriverConfigDialog(String graphicsDriver, View anchor) {
        switch (graphicsDriver) {
            case GraphicsDrivers.TURNIP:
                (new TurnipConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.VORTEK:
                (new VortekConfigDialog(anchor)).show();
                break;
            case GraphicsDrivers.VIRGL:
                (new VirGLConfigDialog(anchor)).show();
                break;
        }
    }
}
