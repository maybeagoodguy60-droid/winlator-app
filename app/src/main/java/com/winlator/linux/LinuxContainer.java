package com.winlator.linux;

import com.winlator.container.Container;
import com.winlator.container.GraphicsDrivers;
import com.winlator.core.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class LinuxContainer extends Container {
    public static final String DEFAULT_ROOTFS_TYPE = "debian";
    public static final String DEFAULT_DESKTOP_ENV = "auto";
    public static final String DEFAULT_CPU_GOVERNOR = "ondemand";
    public static final String DEFAULT_GRAPHICS_DRIVER = GraphicsDrivers.VORTEK + "," + GraphicsDrivers.VIRGL;
    public static final int LAUNCH_MODE_AUTO = 0;
    public static final int LAUNCH_MODE_PROOT = 1;
    public static final int LAUNCH_MODE_CHROOT = 2;
    
    private String rootfsPath = "";
    private String rootfsType = DEFAULT_ROOTFS_TYPE;
    private String desktopEnv = DEFAULT_DESKTOP_ENV;
    private String launchCommand = "";
    private String cpuGovernor = DEFAULT_CPU_GOVERNOR;
    private String cpuAffinity = "";
    private int launchMode = LAUNCH_MODE_AUTO;

    public LinuxContainer(int id) {
        super(id);
        setGraphicsDriver(DEFAULT_GRAPHICS_DRIVER);
    }

    public String getRootfsPath() {
        return rootfsPath;
    }

    public void setRootfsPath(String rootfsPath) {
        this.rootfsPath = rootfsPath != null ? rootfsPath : "";
    }

    public String getRootfsType() {
        return rootfsType;
    }

    public void setRootfsType(String rootfsType) {
        this.rootfsType = rootfsType != null ? rootfsType : DEFAULT_ROOTFS_TYPE;
    }

    public String getDesktopEnv() {
        return desktopEnv;
    }

    public void setDesktopEnv(String desktopEnv) {
        this.desktopEnv = desktopEnv != null ? desktopEnv : DEFAULT_DESKTOP_ENV;
    }

    public String getLaunchCommand() {
        return launchCommand;
    }

    public void setLaunchCommand(String launchCommand) {
        this.launchCommand = launchCommand != null ? launchCommand : "";
    }

    public int getLaunchMode() {
        return launchMode;
    }

    public void setLaunchMode(int launchMode) {
        this.launchMode = launchMode;
    }

    public String getCpuGovernor() {
        return cpuGovernor;
    }

    public void setCpuGovernor(String cpuGovernor) {
        this.cpuGovernor = cpuGovernor != null ? cpuGovernor : DEFAULT_CPU_GOVERNOR;
    }

    public String getCpuAffinity() {
        return cpuAffinity;
    }

    public void setCpuAffinity(String cpuAffinity) {
        this.cpuAffinity = cpuAffinity != null ? cpuAffinity : "";
    }

    @Override
    public void saveData() {
        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("name", getName());
            data.put("screenSize", getScreenSize());
            data.put("envVars", getEnvVars());
            data.put("graphicsDriver", getGraphicsDriver());
            data.put("audioDriver", getAudioDriver());
            data.put("audioDriverConfig", getAudioDriverConfig());
            data.put("hudMode", getHUDMode());
            data.put("startupSelection", getStartupSelection());
            data.put("cpuList", getCPUList());
            data.put("rootfsPath", rootfsPath);
            data.put("rootfsType", rootfsType);
            data.put("desktopEnv", desktopEnv);
            data.put("launchCommand", launchCommand);
            data.put("cpuGovernor", cpuGovernor);
            data.put("cpuAffinity", cpuAffinity);
            data.put("launchMode", launchMode);
            data.put("extraData", getExtraData() != null ? getExtraData() : new JSONObject());
            FileUtils.writeString(getConfigFile(), data.toString());
        } catch (JSONException e) {}
    }

    @Override
    public void loadData(JSONObject data) throws JSONException {
        for (java.util.Iterator<String> it = data.keys(); it.hasNext(); ) {
            String key = it.next();
            switch (key) {
                case "name": setName(data.getString(key)); break;
                case "screenSize": setScreenSize(data.getString(key)); break;
                case "envVars": setEnvVars(data.getString(key)); break;
                case "graphicsDriver": setGraphicsDriver(data.getString(key)); break;
                case "audioDriver": setAudioDriver(data.getString(key)); break;
                case "audioDriverConfig": setAudioDriverConfig(data.getString(key)); break;
                case "hudMode": setHUDMode((byte)data.getInt(key)); break;
                case "startupSelection": setStartupSelection((byte)data.getInt(key)); break;
                case "cpuList": setCPUList(data.getString(key)); break;
                case "rootfsPath": setRootfsPath(data.getString(key)); break;
                case "rootfsType": setRootfsType(data.getString(key)); break;
                case "desktopEnv": setDesktopEnv(data.getString(key)); break;
                case "launchCommand": setLaunchCommand(data.getString(key)); break;
                case "cpuGovernor": setCpuGovernor(data.getString(key)); break;
                case "cpuAffinity": setCpuAffinity(data.getString(key)); break;
                case "launchMode": setLaunchMode(data.getInt(key)); break;
                case "extraData": setExtraData(data.getJSONObject(key)); break;
            }
        }
    }

    public File getRootfsDir() {
        return new File(rootfsPath);
    }

    public boolean hasRootfs() {
        return rootfsPath != null && !rootfsPath.isEmpty() && new File(rootfsPath).isDirectory();
    }
}
