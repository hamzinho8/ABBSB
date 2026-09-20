package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

public class BluetoothClient extends Thread implements DiscoveryListener {
    // UUID personnalisé sans tirets
    private static final String UUID_STRING = "27B4426543A74744A6F1C20531AE40F1";
    
    private boolean running = true;
    private boolean connected = false;
    private StreamConnection connection;
    private InputStream inputStream;
    private OutputStream outputStream;
    
    private ConnectionManager connectionManager;
    private DiscoveryAgent discoveryAgent;
    
    private Vector devicesFound = new Vector();
    private String serviceUrl = null;
    private Object lock = new Object();

    public BluetoothClient(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void run() {
        while (running) {
            try {
                if (!connected) {
                    LogManager.log("BT_CLIENT", "Starting Android discovery...");
                    connectToAndroid();
                }

                if (connected && connection != null) {
                    inputStream = connection.openInputStream();
                    outputStream = connection.openOutputStream();
                    
                    connectionManager.onConnected();
                    LogManager.log("BT_CLIENT", "Streams opened successfully.");

                    StringBuffer buffer = new StringBuffer();
                    int ch;
                    
                    // Boucle de lecture permanente
                    while (running && connected) {
                        try {
                            ch = inputStream.read();
                            if (ch == -1) {
                                break; // Connexion perdue
                            }
                            
                            if (ch == '\n') {
                                String msg = buffer.toString();
                                connectionManager.onDataReceived(msg);
                                buffer.setLength(0);
                            } else if (ch != '\r') {
                                buffer.append((char) ch);
                            }
                            
                            if (buffer.length() > 4096) {
                                LogManager.error("BT_CLIENT", "Buffer overflow");
                                buffer.setLength(0);
                            }
                        } catch (IOException e) {
                            break; // Erreur de lecture
                        }
                    }
                }
            } catch (Throwable e) {
                LogManager.error("BT_CLIENT_ERR", "Client Exception: " + e.getMessage());
            } finally {
                cleanupConnection();
                if (running) {
                    try { Thread.sleep(5000); } catch (Exception e) {} // Attendre avant reconnexion
                }
            }
        }
    }

    private void connectToAndroid() throws Exception {
        LocalDevice localDevice = LocalDevice.getLocalDevice();
        discoveryAgent = localDevice.getDiscoveryAgent();
        serviceUrl = null;
        devicesFound.removeAllElements();
        
        // 1. Recherche des appareils (Inquiry)
        synchronized (lock) {
            discoveryAgent.startInquiry(DiscoveryAgent.GIAC, this);
            try { lock.wait(); } catch (InterruptedException e) {}
        }
        
        // 2. Recherche du service avec notre UUID sur chaque appareil trouvé
        UUID[] uuidSet = new UUID[] { new UUID(UUID_STRING, false) };
        for (int i = 0; i < devicesFound.size(); i++) {
            RemoteDevice device = (RemoteDevice) devicesFound.elementAt(i);
            synchronized (lock) {
                discoveryAgent.searchServices(null, uuidSet, device, this);
                try { lock.wait(); } catch (InterruptedException e) {}
            }
            if (serviceUrl != null) {
                break; // Service trouvé !
            }
        }
        
        // 3. Connexion
        if (serviceUrl != null) {
            LogManager.log("BT_CLIENT", "Service found! Connecting to " + serviceUrl);
            connection = (StreamConnection) Connector.open(serviceUrl);
            connected = true;
            LogManager.log("BT_CLIENT", "Connected to Android.");
        } else {
            LogManager.error("BT_CLIENT", "Android service not found.");
        }
    }

    // --- DiscoveryListener Methods ---

    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
        devicesFound.addElement(btDevice);
    }

    public void inquiryCompleted(int discType) {
        synchronized (lock) { lock.notify(); }
    }

    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
        if (servRecord != null && servRecord.length > 0) {
            serviceUrl = servRecord[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
        }
    }

    public void serviceSearchCompleted(int transID, int respCode) {
        synchronized (lock) { lock.notify(); }
    }

    // --- Cleanup & Utilities ---

    private void cleanupConnection() {
        connected = false;
        try { if (inputStream != null) { inputStream.close(); inputStream = null; } } catch (Throwable e) {}
        try { if (outputStream != null) { outputStream.close(); outputStream = null; } } catch (Throwable e) {}
        try { if (connection != null) { connection.close(); connection = null; } } catch (Throwable e) {}
        if (running) connectionManager.onDisconnected();
    }

    public void send(String data) {
        try {
            if (outputStream != null) {
                outputStream.write(data.getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            cleanupConnection();
        }
    }

    public void stopClient() {
        running = false;
        cleanupConnection();
        if (discoveryAgent != null) discoveryAgent.cancelInquiry(this);
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public void forceDisconnect() {
        cleanupConnection();
    }
}
