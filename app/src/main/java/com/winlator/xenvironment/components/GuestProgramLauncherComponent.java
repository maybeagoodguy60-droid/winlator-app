package com.winlator.xenvironment.components;

import android.os.Process;

import com.winlator.core.Callback;
import com.winlator.core.EnvVars;
import com.winlator.core.LocaleHelper;
import com.winlator.core.ProcessHelper;
import com.winlator.linux.LinuxContainer;
import com.winlator.linux.ProotLauncher;
import com.winlator.xconnector.UnixSocketConfig;
import com.winlator.xenvironment.EnvironmentComponent;
import com.winlator.xenvironment.RootFS;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class GuestProgramLauncherComponent extends EnvironmentComponent {
    private String guestExecutable;
    private static int pid = -1;
    private EnvVars envVars;
    private Callback<Integer> terminationCallback;
    private Callback<String> startupFailureCallback;
    private static final Object lock = new Object();
    private final HashMap<File, String> originalGovernors = new HashMap<>();

    private String cpuGovernor;
    private int cpuAffinityMask = 0;
    private int launchMode = LinuxContainer.LAUNCH_MODE_AUTO;

    public void setCpuGovernor(String cpuGovernor) {
        this.cpuGovernor = cpuGovernor;
    }

    public void setCpuAffinityMask(int cpuAffinityMask) {
        this.cpuAffinityMask = cpuAffinityMask;
    }

    public void setLaunchMode(int launchMode) {
        this.launchMode = launchMode;
    }

    public void setStartupFailureCallback(Callback<String> startupFailureCallback) {
        this.startupFailureCallback = startupFailureCallback;
    }

    @Override
    public void start() {
        synchronized (lock) {
            stop();
            if (cpuGovernor != null && !cpuGovernor.isEmpty()) {
                applyCpuGovernor(cpuGovernor);
            }
            pid = execGuestProgram();
            if (pid == -1 && startupFailureCallback != null) {
                startupFailureCallback.call("Could not start the guest process. Check the launch command and that the proot binary is present.");
            }
            else if (pid != -1) {
                applyCpuAffinity(pid);
            }
        }
    }

    private void applyCpuAffinity(int targetPid) {
        if (cpuAffinityMask == 0 || targetPid <= 0) return;
        try {
            ProcessBuilder pb = new ProcessBuilder("su", "-c", "taskset", "-p", Integer.toHexString(cpuAffinityMask), String.valueOf(targetPid));
            pb.redirectErrorStream(true);
            pb.start();
        } catch (Exception e) {}
    }

    private static String readFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            return scanner.hasNextLine() ? scanner.nextLine().trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void applyCpuGovernor(String governor) {
        try {
            File cpuDir = new File("/sys/devices/system/cpu");
            File[] policyDirs = cpuDir.listFiles(d -> d.getName().startsWith("cpufreq"));
            if (policyDirs != null) {
                for (File policyDir : policyDirs) {
                    File governorFile = new File(policyDir, "scaling_governor");
                    if (governorFile.exists()) {
                        String original = readFile(governorFile);
                        if (!governor.equals(original)) {
                            originalGovernors.put(governorFile, original);
                            java.io.FileWriter fw = new java.io.FileWriter(governorFile);
                            fw.write(governor);
                            fw.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Governor write may fail without root - silently ignore
        }
    }

    private void restoreCpuGovernors() {
        for (Map.Entry<File, String> entry : originalGovernors.entrySet()) {
            try {
                FileWriter fw = new FileWriter(entry.getKey());
                fw.write(entry.getValue());
                fw.close();
            } catch (Exception e) {}
        }
        originalGovernors.clear();
    }

    @Override
    public void stop() {
        synchronized (lock) {
            if (pid != -1) {
                Process.killProcess(pid);
                pid = -1;
            }
            restoreCpuGovernors();
        }
    }

    public Callback<Integer> getTerminationCallback() {
        return terminationCallback;
    }

    public void setTerminationCallback(Callback<Integer> terminationCallback) {
        this.terminationCallback = terminationCallback;
    }

    public String getGuestExecutable() {
        return guestExecutable;
    }

    public void setGuestExecutable(String guestExecutable) {
        this.guestExecutable = guestExecutable;
    }

    public EnvVars getEnvVars() {
        return envVars;
    }

    public void setEnvVars(EnvVars envVars) {
        this.envVars = envVars;
    }



    private int execGuestProgram() {
        RootFS rootFS = environment.getRootFS();
        File rootDir = rootFS.getRootDir();

        EnvVars envVars = new EnvVars();
        LocaleHelper.setEnvVars(envVars);

        envVars.put("HOME", RootFS.HOME_PATH);
        envVars.put("USER", RootFS.USER);
        envVars.put("TMPDIR", "/tmp");
        envVars.put("DISPLAY", ":0");
        envVars.put("PATH", "/usr/local/bin:/usr/bin:/bin");
        envVars.put("LD_LIBRARY_PATH", "/usr/lib");
        envVars.put("TZ", java.util.TimeZone.getDefault().getID());
        
        envVars.put("ANDROID_SYSVSHM_SERVER", UnixSocketConfig.SYSVSHM_SERVER_PATH);

        if (this.envVars != null) envVars.putAll(this.envVars);

        File shmDir = new File(rootDir, "/tmp/shm");
        if (!shmDir.isDirectory()) shmDir.mkdirs();

        String command = ProotLauncher.buildCommand(environment.getContext(), rootDir, guestExecutable, launchMode);

        return ProcessHelper.exec(command, envVars, rootDir, (status) -> {
            synchronized (lock) {
                pid = -1;
            }
            if (terminationCallback != null) terminationCallback.call(status);
        });
    }




    @Override
    public void onPause() {
        synchronized (lock) {
            if (pid != -1) {
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = processes.size()-1; i >= 0; i--) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state != ProcessHelper.PState.STOPPED) {
                        ProcessHelper.suspendProcess(process.pid);
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        synchronized (lock) {
            if (pid != -1) {
                List<ProcessHelper.PStat> processes = ProcessHelper.getChildProcesses();
                for (int i = 0; i < processes.size(); i++) {
                    ProcessHelper.PStat process = processes.get(i);
                    if (process.guestProcess && process.state == ProcessHelper.PState.STOPPED) {
                        ProcessHelper.resumeProcess(process.pid);
                    }
                }
            }
        }
    }
}