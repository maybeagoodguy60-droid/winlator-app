package com.winlator.linux;

import java.io.File;

public class LinuxSessionLauncher {
    
    private static final String[][] DE_CHECKS = {
        {"/usr/bin/startxfce4", "xfce4-session"},
        {"/usr/bin/startlxqt", "lxqt-session"},
        {"/usr/bin/startplasma-x11", "plasma-session"},
        {"/usr/bin/gnome-session", "gnome-session"},
        {"/usr/bin/startmate", "mate-session"},
        {"/usr/bin/budgie-desktop", "budgie-desktop"}
    };
    
    private static final String[][] TERM_CHECKS = {
        {"/usr/bin/xterm", "xterm"},
        {"/usr/bin/qterminal", "qterminal"},
        {"/usr/bin/xfce4-terminal", "xfce4-terminal"},
        {"/usr/bin/gnome-terminal", "gnome-terminal"},
        {"/usr/bin/mate-terminal", "mate-terminal"},
        {"/usr/bin/lxterminal", "lxterminal"},
        {"/usr/bin/konsole", "konsole"}
    };

    public static String detectDesktopEnvironment(File rootDir) {
        for (String[] de : DE_CHECKS) {
            if (new File(rootDir, de[0]).exists()) {
                return de[1];
            }
        }
        return null;
    }

    public static String detectTerminal(File rootDir) {
        for (String[] term : TERM_CHECKS) {
            if (new File(rootDir, term[0]).exists()) {
                return term[1];
            }
        }
        return null;
    }

    public static String buildLaunchCommand(File rootDir, String desktopEnv, String customLaunchCommand) {
        if (customLaunchCommand != null && !customLaunchCommand.isEmpty()) {
            return customLaunchCommand;
        }
        
        if (desktopEnv != null && !desktopEnv.equals("auto")) {
            switch (desktopEnv) {
                case "xfce": return "startxfce4";
                case "lxqt": return "lxqt-session";
                case "kde": return "plasma-session";
                case "gnome": return "gnome-session";
                case "mate": return "mate-session";
                case "terminal":
                    String term = detectTerminal(rootDir);
                    return term != null ? term : "xterm";
            }
        }
        
        String detected = detectDesktopEnvironment(rootDir);
        if (detected != null) return detected;
        
        String term = detectTerminal(rootDir);
        return term != null ? term : "xterm";
    }

    public static boolean hasDesktopEnvironment(File rootDir) {
        return detectDesktopEnvironment(rootDir) != null;
    }

    public static boolean hasTerminal(File rootDir) {
        return detectTerminal(rootDir) != null;
    }

    public static String getPackageManager(File rootDir) {
        if (new File(rootDir, "/usr/bin/apt").exists()) return "apt";
        if (new File(rootDir, "/usr/bin/apk").exists()) return "apk";
        if (new File(rootDir, "/usr/bin/pacman").exists()) return "pacman";
        if (new File(rootDir, "/usr/bin/dnf").exists()) return "dnf";
        return null;
    }

    public static String getInstallCommand(String pkgManager, String pkg) {
        switch (pkgManager) {
            case "apt": return "apt-get update && apt-get install -y " + pkg;
            case "apk": return "apk add " + pkg;
            case "pacman": return "pacman -S --noconfirm " + pkg;
            case "dnf": return "dnf install -y " + pkg;
            default: return null;
        }
    }
}
