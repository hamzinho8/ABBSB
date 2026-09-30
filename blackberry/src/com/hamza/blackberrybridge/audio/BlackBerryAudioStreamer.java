package com.hamza.blackberrybridge.audio;

import net.rim.device.api.media.control.AudioPathControl;

/**
 * Adaptateur de compatibilité audio pour BlackBerry Curve 9300.
 * Délégué direct vers le moteur de double tampon {@link AudioStreamReceiver}.
 */
public class BlackBerryAudioStreamer {
    private static BlackBerryAudioStreamer instance;

    public static synchronized BlackBerryAudioStreamer getInstance() {
        if (instance == null) {
            instance = new BlackBerryAudioStreamer();
        }
        return instance;
    }

    public void startAudio() {
        AudioStreamReceiver.getInstance().start();
    }

    public void enqueueChunk(String base64Data) {
        AudioStreamReceiver.getInstance().onAudioChunk(base64Data);
    }

    public void stopAudio() {
        AudioStreamReceiver.getInstance().stop();
    }

    public boolean isRunning() {
        return AudioStreamReceiver.getInstance().isRunning();
    }

    public int getChunksPlayed() {
        return AudioStreamReceiver.getInstance().getTotalChunksPlayed();
    }

    public void setVolume(int vol) {
        AudioStreamReceiver.getInstance().setVolume(vol);
    }

    public int getVolume() {
        return AudioStreamReceiver.getInstance().getVolume();
    }

    public void setForcedAudioPath(int path) {
        AudioStreamReceiver.getInstance().setAudioRoute(path);
    }

    public void toggleSpeakerHandset() {
        AudioStreamReceiver.getInstance().toggleAudioRoute();
    }
}
