package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.media.Manager;

public class HardwareManager {
    private static Timer findPhoneTimer;

    public static void triggerMessageAlert() {
        try {
            // Standard J2ME MMAPI tone (A4 note, 150ms, volume 80)
            // 100% open and unrestricted - does not require RIM code signing keys
            Manager.playTone(69, 150, 80);
        } catch (Throwable t) {}
    }
    
    public static void stopMessageAlert() {
        // No-op
    }
    
    public static void triggerNotificationAlert(SmartBridgeApp app, String appName) {
        // Audio playback is handled cleanly by app.getAudioManager().playNotificationSound(appName)
    }
    
    public static void triggerCallAlert(SmartBridgeApp app) {
        stopAlerts();
        if (app != null && app.getAudioManager() != null) {
            app.getAudioManager().playCallRingtone();
        }
    }
    
    public static void stopAlerts() {
        stopFindPhoneAlert();
    }
    
    public static void startFindPhoneAlert() {
        stopFindPhoneAlert();
        findPhoneTimer = new Timer();
        findPhoneTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                try {
                    Manager.playTone(81, 350, 100);
                } catch (Throwable t) {}
            }
        }, 0, 700);
    }
    
    public static void stopFindPhoneAlert() {
        if (findPhoneTimer != null) {
            findPhoneTimer.cancel();
            findPhoneTimer = null;
        }
    }
}

