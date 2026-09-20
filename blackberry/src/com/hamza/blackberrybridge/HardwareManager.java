package com.hamza.blackberrybridge;

import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.notification.NotificationsConstants;
import java.util.Timer;
import java.util.TimerTask;

public class HardwareManager {
    private static Timer vibrateTimer;
    private static Timer findPhoneTimer;
    private static final long NOTIF_ID = 0x5a3b92c4L; // Unique ID for SmartBridge

    static {
        try {
            // Register application notification source in BlackBerry OS
            NotificationsManager.registerSource(NOTIF_ID, "SmartBridge", NotificationsConstants.DEFAULT_LEVEL);
        } catch (Throwable t) {}
    }

    public static void triggerMessageAlert() {
        try {
            NotificationsManager.triggerNotification(NOTIF_ID, -1, 500, null);
        } catch (Throwable t) {}
    }
    
    public static void stopMessageAlert() {
        try {
            NotificationsManager.cancelNotification(NOTIF_ID);
        } catch (Throwable t) {}
    }
    
    public static void triggerNotificationAlert(SmartBridgeApp app, String appName) {
        try {
            NotificationsManager.triggerNotification(NOTIF_ID, -1, 500, null);
        } catch (Throwable t) {}
    }
    
    public static void triggerCallAlert(SmartBridgeApp app) {
        stopAlerts();
        vibrateTimer = new Timer();
        vibrateTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    NotificationsManager.triggerNotification(NOTIF_ID, -1, 255, null);
                } catch (Throwable t) {}
            }
        }, 0, 1000);
    }
    
    public static void stopAlerts() {
        if (vibrateTimer != null) {
            vibrateTimer.cancel();
            vibrateTimer = null;
        }
        try {
            NotificationsManager.cancelNotification(NOTIF_ID);
        } catch (Throwable t) {}
    }
    
    public static void startFindPhoneAlert() {
        stopFindPhoneAlert();
        findPhoneTimer = new Timer();
        findPhoneTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    NotificationsManager.triggerNotification(NOTIF_ID, -1, 255, null);
                } catch (Throwable t) {}
            }
        }, 0, 1000);
    }
    
    public static void stopFindPhoneAlert() {
        if (findPhoneTimer != null) {
            findPhoneTimer.cancel();
            findPhoneTimer = null;
        }
        try {
            NotificationsManager.cancelNotification(NOTIF_ID);
        } catch (Throwable t) {}
    }
}
