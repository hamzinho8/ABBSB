package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectionManager {
    private BluetoothClient btClient;
    private ProtocolManager protocolManager;
    private UIManager uiManager;
    private SmartBridgeApp app;
    private long lastDataTime = 0;
    private Timer watchdogTimer;
    private Timer weatherTimer;
    
    public ConnectionManager(UIManager uiManager, SmartBridgeApp app) {
        this.uiManager = uiManager;
        this.app = app;
        this.protocolManager = new ProtocolManager(this, app);
    }
    
    public void startServer() {
        uiManager.updateConnectionStatus("CONNECTING");
        if (btClient != null) {
            btClient.stopClient();
        }
        btClient = new BluetoothClient(this);
        btClient.start();
        
        startWatchdog();
    }
    
    public void stopServer() {
        stopWatchdog();
        if (btClient != null) {
            btClient.stopClient();
            btClient = null;
        }
    }
    
    private void startWatchdog() {
        stopWatchdog();
        watchdogTimer = new Timer();
        watchdogTimer.schedule(new TimerTask() {
            public void run() {
                if (btClient != null && btClient.isConnected()) {
                    long now = System.currentTimeMillis();
                    // If no data for 20 seconds, connection might be dead
                    if (now - lastDataTime > 20000) {
                        LogManager.error("ConnMgr", "Watchdog timeout. Restarting connection.");
                        btClient.forceDisconnect();
                    } else {
                        // Send PING
                        sendData("PING\n");
                    }
                }
            }
        }, 10000, 10000); // Check every 10 seconds
    }
    
    private void stopWatchdog() {
        if (watchdogTimer != null) {
            watchdogTimer.cancel();
            watchdogTimer = null;
        }
    }
    
    private void startWeatherTimer() {
        stopWeatherTimer();
        weatherTimer = new Timer();
        weatherTimer.schedule(new TimerTask() {
            public void run() {
                if (btClient != null && btClient.isConnected()) {
                    sendData("WEATHER\n");
                }
            }
        }, 3600000, 3600000); // Check every 1 hour
    }

    private void stopWeatherTimer() {
        if (weatherTimer != null) {
            weatherTimer.cancel();
            weatherTimer = null;
        }
    }
    
    public void onConnected() {
        lastDataTime = System.currentTimeMillis();
        LogManager.log("ConnMgr", "Android connected");
        uiManager.updateConnectionStatus("CONNECTED");
        sendData("GET_PHONE_BATTERY\n");
        sendData("WEATHER\n");
        startWeatherTimer();
    }
    
    public void onDisconnected() {
        LogManager.log("ConnMgr", "Connection lost");
        uiManager.updateConnectionStatus("DISCONNECTED");
        stopWeatherTimer();
        // BluetoothClient has its own retry loop now. We do NOT call startServer() here.
    }
    
    public void onDataReceived(String data) {
        lastDataTime = System.currentTimeMillis();
        protocolManager.processMessage(data);
    }
    
    public void sendData(String data) {
        if (btClient != null) {
            btClient.send(data);
        } else {
            LogManager.error("ConnMgr", "Cannot send, disconnected");
        }
    }
}
