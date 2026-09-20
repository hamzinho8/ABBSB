package com.hamza.blackberrybridge;

public class LogManager {
    private static final String APP_NAME = "SmartBridge";
    private static boolean debugEnabled = true;

    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static void log(String tag, String message) {
        String fullMsg = "[" + tag + "] " + message;
        System.out.println(fullMsg);
    }

    public static void error(String tag, String message) {
        String fullMsg = "[ERROR][" + tag + "] " + message;
        System.out.println(fullMsg);
    }
}
