package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.media.Manager;
import net.rim.device.api.system.Alert;
import net.rim.device.api.system.LED;

/**
 * Gestionnaire du matériel natif pour BlackBerry Curve 9300.
 * Contrôle des vibrations, de la LED rouge de notification et des tonalités sonores J2ME.
 */
public class HardwareManager {
    private static Timer findPhoneTimer;
    private static Timer ledTimer;

    /**
     * Déclenche une alerte complète pour l'arrivée d'un nouveau SMS :
     * 1. Vibration physique BlackBerry (600 ms)
     * 2. Tonalité sonore MMAPI nette
     * 3. LED rouge clignotante pendant 4 secondes
     */
    public static void triggerSmsAlert() {
        // 1. Vibration
        try {
            if (Alert.isVibrateSupported()) {
                Alert.startVibrate(600);
            }
        } catch (Throwable t) {}

        // 2. Tonalité sonore aiguë (note B6, 220 ms, volume 100%)
        try {
            Manager.playTone(83, 220, 100);
        } catch (Throwable t) {}

        // 3. LED Rouge clignotante BlackBerry
        try {
            if (ledTimer != null) {
                ledTimer.cancel();
                ledTimer = null;
            }
            LED.setConfiguration(300, 300, LED.BRIGHTNESS_100);
            LED.setState(LED.STATE_BLINKING);

            ledTimer = new Timer();
            ledTimer.schedule(new TimerTask() {
                public void run() {
                    try {
                        LED.setState(LED.STATE_OFF);
                    } catch (Throwable ignored) {}
                }
            }, 4500);
        } catch (Throwable t) {}
    }

    public static void stopSmsAlert() {
        try {
            if (ledTimer != null) {
                ledTimer.cancel();
                ledTimer = null;
            }
            LED.setState(LED.STATE_OFF);
        } catch (Throwable ignored) {}
    }

    public static void triggerMessageAlert() {
        triggerSmsAlert();
    }
    
    public static void stopMessageAlert() {
        stopSmsAlert();
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
        stopSmsAlert();
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
