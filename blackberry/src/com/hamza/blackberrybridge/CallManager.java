package com.hamza.blackberrybridge;

public class CallManager {
    private UIManager uiManager;
    private SmartBridgeApp app;
    private CallScreen activeCallScreen;
    
    public CallManager(UIManager uiManager, SmartBridgeApp app) {
        this.uiManager = uiManager;
        this.app = app;
    }
    
    public void handleIncomingCall(String id, String name, String number) {
        HardwareManager.triggerCallAlert(app);
        
        activeCallScreen = new CallScreen(this, id, name, number);
        uiManager.pushGlobalScreen(activeCallScreen);
    }
    
    public void handleCallActive(String id) {
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        if (activeCallScreen != null) {
            activeCallScreen.setStatus("Active");
        }
    }
    
    public void handleCallEnd(String id) {
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        if (activeCallScreen != null) {
            activeCallScreen.close();
            activeCallScreen = null;
        }
    }
    
    public void handleCallMissed(String id, String name, String number) {
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        if (activeCallScreen != null) {
            activeCallScreen.close();
            activeCallScreen = null;
        }
        
        app.getNotificationManager().handleNotification("missed_" + id, "Phone", name, "Missed call from " + number);
    }
    
    public void answerCall(String id) {
        app.getAudioManager().stopCallRingtone();
        HardwareManager.stopAlerts();
        app.getConnectionManager().sendData("CALL_ANSWER|" + id + "\n");
    }
    
    public void rejectCall(String id) {
        app.getAudioManager().stopCallRingtone();
        HardwareManager.stopAlerts();
        app.getConnectionManager().sendData("CALL_REJECT|" + id + "\n");
    }
}
