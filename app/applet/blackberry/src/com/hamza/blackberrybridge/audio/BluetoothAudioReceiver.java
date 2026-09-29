package com.hamza.blackberrybridge.audio;

/**
 * Adaptateur de compatibilité audio Bluetooth pour BlackBerry Curve 9300.
 * Redirige les flux audio WAV vers le moteur séquentiel unique {@link BlackBerryAudioStreamer}
 * pour éliminer les conflits matériels et les erreurs JVM (226).
 */
public class BluetoothAudioReceiver {
    private static BluetoothAudioReceiver instance;

    public static synchronized BluetoothAudioReceiver getInstance() {
        if (instance == null) {
            instance = new BluetoothAudioReceiver();
        }
        return instance;
    }

    public void startReceiver() {
        BlackBerryAudioStreamer.getInstance().startAudio();
    }

    public boolean isRunning() {
        return BlackBerryAudioStreamer.getInstance().isRunning();
    }

    public void processAudioChunk(String base64Payload) {
        BlackBerryAudioStreamer.getInstance().enqueueChunk(base64Payload);
    }

    public void stopReceiver() {
        BlackBerryAudioStreamer.getInstance().stopAudio();
    }

    public void setVolume(int vol) {
        BlackBerryAudioStreamer.getInstance().setVolume(vol);
    }

    public int getVolume() {
        return BlackBerryAudioStreamer.getInstance().getVolume();
    }

    public void adjustVolume(int delta) {
        setVolume(getVolume() + delta);
    }
}
