package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;

public class ConnectionManager {
    private BluetoothServer btServer;
    private ProtocolManager protocolManager;
    private UIManager uiManager;
    private long lastDataTime = 0;
    private Timer watchdogTimer;
    private Timer weatherTimer;
    
    public ConnectionManager(UIManager uiManager, SmartBridgeApp app) {
        this.uiManager = uiManager;
        this.protocolManager = new ProtocolManager(this, app);
    }
    
    public synchronized void startServer() {
        uiManager.updateConnectionStatus("CONNECTING");
        if (btServer != null) {
            btServer.stopServer();
        }
        btServer = new BluetoothServer(this);
        btServer.startServer();
        
        startWatchdog();
    }
    
    public synchronized void stopServer() {
        stopWatchdog();
        stopWeatherTimer();
        if (btServer != null) {
            btServer.stopServer();
            btServer = null;
        }
        uiManager.updateConnectionStatus("DISCONNECTED");
    }

    public synchronized void restartServer() {
        LogManager.log("ConnMgr", "Restarting Bluetooth Server...");
        stopServer();
        try {
            Thread.sleep(500);
        } catch (Exception e) {}
        startServer();
    }

    public void connectToPairedDevice() {
        if (btServer != null) {
            btServer.connectToPairedDevice();
        }
    }
    
    private void startWatchdog() {
        stopWatchdog();
        watchdogTimer = new Timer();
        watchdogTimer.schedule(new TimerTask() {
            public void run() {
                if (btServer != null && btServer.isConnected()) {
                    long now = System.currentTimeMillis();
                    // If no data received for 60 seconds, connection might be stale
                    if (now - lastDataTime > 60000) {
                        LogManager.error("ConnMgr", "Watchdog timeout (60s silence). Restarting link.");
                        btServer.forceDisconnect();
                    } else if (now - lastDataTime > 15000) {
                        // Send periodic PING heartbeat
                        sendData("PING\n");
                    }
                }
            }
        }, 15000, 15000);
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
                if (btServer != null && btServer.isConnected()) {
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
        LogManager.log("ConnMgr", "Android connected successfully");
        uiManager.updateConnectionStatus("CONNECTED");

        // Send full BSB/1 protocol handshake and initial sync
        sendData("HELLO|BSB/1|BLACKBERRY_9790\n");
        sendData("READY\n");
        sendData("BATTERY|" + BatteryManager.getBatteryLevel() + "\n");
        sendData("GET_PHONE_BATTERY\n");
        sendData("WEATHER\n");
        startWeatherTimer();
    }
    
    public void onDisconnected() {
        LogManager.log("ConnMgr", "Connection lost, server still listening for incoming reconnection");
        uiManager.updateConnectionStatus("DISCONNECTED");
        stopWeatherTimer();
    }
    
    public void onDataReceived(String data) {
        lastDataTime = System.currentTimeMillis();
        protocolManager.processMessage(data);
    }
    
    public void sendData(String data) {
        if (btServer != null) {
            btServer.send(data);
        } else {
            LogManager.error("ConnMgr", "Cannot send, server not initialized");
        }
    }

    public boolean isConnected() {
        return btServer != null && btServer.isConnected();
    }
}
