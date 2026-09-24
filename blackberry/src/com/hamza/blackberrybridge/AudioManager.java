package com.hamza.blackberrybridge;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import net.rim.device.api.media.control.AudioPathControl;
import net.rim.device.api.system.Alert;
import net.rim.device.api.system.Audio;
import java.io.InputStream;

public class AudioManager {
    private SmartBridgeApp app;
    private Player callPlayer;
    private Player notifPlayer;
    private long lastNotifTime = 0;
    
    // Local BlackBerry audio output routing
    private boolean localSpeakerOn = false;
    private int currentLocalAudioPath = AudioPathControl.AUDIO_PATH_HANDSET;

    public AudioManager(SmartBridgeApp app) {
        this.app = app;
    }

    private boolean canPlayAudio() {
        SettingsManager sm = app.getSettingsManager();
        String p = sm.getProfile();
        return p != null && (p.equals("Ring") || p.equals("Ring+Vibrate"));
    }

    public void playCallRingtone() {
        if (!app.getSettingsManager().isSoundCalls() || !canPlayAudio()) return;

        try {
            stopCallRingtone();
            InputStream is = getClass().getResourceAsStream("/ringtone.mp3");
            if (is != null) {
                callPlayer = Manager.createPlayer(is, "audio/mpeg");
                callPlayer.realize();
                VolumeControl vc = (VolumeControl) callPlayer.getControl("VolumeControl");
                if (vc != null) vc.setLevel(app.getSettingsManager().getVolCalls());
                callPlayer.prefetch();
                callPlayer.setLoopCount(-1);
                callPlayer.start();
            } else {
                LogManager.error("AUDIO", "Missing /ringtone.mp3");
            }
        } catch (Throwable e) {
            LogManager.error("AUDIO", "Call ring error: " + e.getMessage());
        }
    }

    public void stopCallRingtone() {
        try {
            if (callPlayer != null) {
                if (callPlayer.getState() == Player.STARTED) {
                    callPlayer.stop();
                }
                callPlayer.deallocate();
                callPlayer.close();
                callPlayer = null;
            }
        } catch (Throwable e) {}
    }

    /**
     * Bip sonore court émis à la fin de l'appel (style natif BlackBerry).
     */
    public void playCallEndBeep() {
        try {
            if (Alert.isAudioSupported()) {
                // Séquence [Fréquence Hz, Durée ms, Fréquence 2, Durée 2...]
                short[] endBeep = new short[] { 900, 120, 0, 40, 700, 180 };
                Alert.startAudio(endBeep, 80);
            }
        } catch (Throwable t) {
            LogManager.error("AUDIO", "Call end beep error: " + t.getMessage());
        }
    }

    /**
     * Bip sonore doux émis lors de la mise en communication de l'appel.
     */
    public void playCallConnectBeep() {
        try {
            if (Alert.isAudioSupported()) {
                short[] connectBeep = new short[] { 700, 80, 0, 30, 950, 100 };
                Alert.startAudio(connectBeep, 75);
            }
        } catch (Throwable t) {
            LogManager.error("AUDIO", "Call connect beep error: " + t.getMessage());
        }
    }

    /**
     * Configuration du routage audio local sur le haut-parleur ou l'écouteur du BlackBerry.
     * Utilise net.rim.device.api.media.control.AudioPathControl.
     */
    public synchronized boolean setLocalAudioPath(int path) {
        this.currentLocalAudioPath = path;
        this.localSpeakerOn = (path == AudioPathControl.AUDIO_PATH_HANDSFREE);
        LogManager.log("AUDIO", "Local audio path set to: " + (localSpeakerOn ? "HANDSFREE (HP)" : "HANDSET (Combiné)"));
        
        try {
            if (callPlayer != null) {
                AudioPathControl apc = (AudioPathControl) callPlayer.getControl("net.rim.device.api.media.control.AudioPathControl");
                if (apc != null && apc.canSwitchToPath(path)) {
                    apc.setAudioPath(path);
                    return true;
                }
            }
        } catch (Throwable t) {
            LogManager.error("AUDIO", "AudioPathControl error: " + t.getMessage());
        }
        return false;
    }

    public synchronized boolean toggleLocalAudioPath() {
        int targetPath = localSpeakerOn ? AudioPathControl.AUDIO_PATH_HANDSET : AudioPathControl.AUDIO_PATH_HANDSFREE;
        setLocalAudioPath(targetPath);
        return localSpeakerOn;
    }

    public boolean isLocalSpeakerOn() {
        return localSpeakerOn;
    }

    public int getLocalAudioPath() {
        return currentLocalAudioPath;
    }

    public void playNotificationSound(String appName) {
        if (!app.getSettingsManager().isSoundNotifs() || !canPlayAudio()) return;

        long now = System.currentTimeMillis();
        if (now - lastNotifTime < 1500) return;
        lastNotifTime = now;

        try {
            if (notifPlayer != null) {
                if (notifPlayer.getState() == Player.STARTED) {
                    notifPlayer.stop();
                }
                notifPlayer.deallocate();
                notifPlayer.close();
                notifPlayer = null;
            }

            String file = "/notif.mp3";
            String lApp = appName != null ? appName.toLowerCase() : "";
            
            if (lApp.indexOf("whatsapp") != -1) file = "/whatsapp.mp3";
            else if (lApp.indexOf("messenger") != -1 || lApp.indexOf("facebook") != -1) file = "/messenger.mp3";
            else if (lApp.indexOf("telegram") != -1) file = "/telegram.mp3";
            else if (lApp.indexOf("sms") != -1 || lApp.indexOf("message") != -1) file = "/sms.mp3";
            
            InputStream is = getClass().getResourceAsStream(file);
            if (is != null) {
                notifPlayer = Manager.createPlayer(is, "audio/mpeg");
                notifPlayer.realize();
                VolumeControl vc = (VolumeControl) notifPlayer.getControl("VolumeControl");
                if (vc != null) vc.setLevel(app.getSettingsManager().getVolNotifs());
                notifPlayer.prefetch();
                notifPlayer.start();
            }
        } catch (Throwable e) {
            LogManager.error("AUDIO", "Notif error: " + e.getMessage());
        }
    }
    
    public void testSound() {
        playNotificationSound("sms");
    }
}
