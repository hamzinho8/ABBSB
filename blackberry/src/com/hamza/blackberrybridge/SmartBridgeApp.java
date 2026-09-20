package com.hamza.blackberrybridge;

import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

public class SmartBridgeApp extends UiApplication {
    private UIManager uiManager;
    private ConnectionManager connectionManager;
    private CallManager callManager;
    private NotificationManager notificationManager;
    private ContactManager contactManager;
    private MediaManager mediaManager;
    private SettingsManager settingsManager;
    private AudioManager audioManager;

    public static void main(String[] args) {
        SmartBridgeApp app = new SmartBridgeApp();
        app.enterEventDispatcher();
    }

    public SmartBridgeApp() {
        try {
            settingsManager = new SettingsManager();
            
            LogManager.setDebugEnabled(settingsManager.isDebugMode());
            LogManager.log("APP", "Starting SmartBridge");
            
            uiManager = new UIManager(this);
            audioManager = new AudioManager(this);
            connectionManager = new ConnectionManager(uiManager, this);
            callManager = new CallManager(uiManager, this);
            notificationManager = new NotificationManager(uiManager, this);
            contactManager = new ContactManager(this);
            mediaManager = new MediaManager(this);
            
            pushScreen(uiManager.getMainScreen());
            
            // Auto-connect if enabled
            if (settingsManager.isAutoReconnect()) {
                connectionManager.startServer();
            }
        } catch (final Throwable t) {
            invokeLater(new Runnable() {
                public void run() {
                    Dialog.alert("Init Error: " + t.toString() + " - " + t.getMessage());
                }
            });
        }
    }

    public UIManager getUIManager() { return uiManager; }
    public ConnectionManager getConnectionManager() { return connectionManager; }
    public CallManager getCallManager() { return callManager; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public ContactManager getContactManager() { return contactManager; }
    public MediaManager getMediaManager() { return mediaManager; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public AudioManager getAudioManager() { return audioManager; }
    
    protected void onExit() {
        LogManager.log("APP", "Exiting");
        if (audioManager != null) audioManager.stopCallRingtone();
        if (connectionManager != null) connectionManager.stopServer();
    }
}
