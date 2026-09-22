package com.hamza.blackberrybridge;

import net.rim.device.api.notification.NotificationsManager;
import net.rim.device.api.notification.NotificationsConstants;
import net.rim.device.api.system.Alert;
import net.rim.device.api.system.LED;
import java.util.Timer;
import java.util.TimerTask;

public class HardwareManager {
    private static Timer vibrateTimer;
    private static Timer findPhoneTimer;
    private static final long NOTIF_ID = 0x5a3b92c4L; // Unique ID for SmartBridge
    private static final short[] BEEP_TUNE = new short[] { 1760, 200, 0, 50, 1760, 200 };

    static {
        try {
            // Register application notification source in BlackBerry OS
            NotificationsManager.registerSource(NOTIF_ID, "SmartBridge", NotificationsConstants.DEFAULT_LEVEL);
        } catch (Throwable t) {}
    }

    public static void triggerMessageAlert() {
        try {
            if (Alert.isVibrateSupported()) {
                Alert.startVibrate(500);
            }
        } catch (Throwable t) {}
        try {
            LED.setState(LED.STATE_BLINKING);
        } catch (Throwable t) {}
        try {
            NotificationsManager.triggerImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
    }
    
    public static void stopMessageAlert() {
        try {
            LED.setState(LED.STATE_OFF);
        } catch (Throwable t) {}
        try {
            NotificationsManager.cancelImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
    }
    
    public static void triggerNotificationAlert(SmartBridgeApp app, String appName) {
        try {
            if (Alert.isVibrateSupported()) {
                Alert.startVibrate(500);
            }
        } catch (Throwable t) {}
        try {
            LED.setState(LED.STATE_BLINKING);
        } catch (Throwable t) {}
        try {
            NotificationsManager.triggerImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
    }
    
    public static void triggerCallAlert(SmartBridgeApp app) {
        stopAlerts();
        try {
            LED.setState(LED.STATE_BLINKING);
        } catch (Throwable t) {}
        try {
            NotificationsManager.triggerImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
        vibrateTimer = new Timer();
        vibrateTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    if (Alert.isVibrateSupported()) {
                        Alert.startVibrate(600);
                    }
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
            LED.setState(LED.STATE_OFF);
        } catch (Throwable t) {}
        try {
            NotificationsManager.cancelImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
    }
    
    public static void startFindPhoneAlert() {
        stopFindPhoneAlert();
        try {
            LED.setState(LED.STATE_BLINKING);
        } catch (Throwable t) {}
        try {
            NotificationsManager.triggerImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
        findPhoneTimer = new Timer();
        findPhoneTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Alert.startAudio(BEEP_TUNE, 100);
                } catch (Throwable t) {}
                try {
                    if (Alert.isVibrateSupported()) {
                        Alert.startVibrate(500);
                    }
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
            LED.setState(LED.STATE_OFF);
        } catch (Throwable t) {}
        try {
            NotificationsManager.cancelImmediateEvent(NOTIF_ID, 0, null, null);
        } catch (Throwable t) {}
    }
}
