package com.hamza.blackberrybridge;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.DiscoveryListener;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

/**
 * High-performance Bluetooth Server & Connection Manager for BlackBerry OS 5.0.
 * Listens for incoming RFCOMM SPP connections from Android companion app
 * using the custom UUID (27B4426543A74744A6F1C20531AE40F1) and standard SPP (0x1101).
 * Also supports on-demand client connection to paired Android devices.
 */
public class BluetoothServer {
    public static final String UUID_CUSTOM = "27B4426543A74744A6F1C20531AE40F1";
    public static final String UUID_SPP    = "0000110100001000800000805F9B34FB";
    public static final String UUID_HFP_HF = "111E"; // Hands-Free Unit Profile
    public static final String UUID_HSP_HS = "1108"; // Headset Profile

    private ConnectionManager connectionManager;
    private boolean running = false;
    private boolean connected = false;

    private StreamConnection activeConnection;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final Object connectionLock = new Object();

    private ServerListenerThread customListener;
    private ServerListenerThread sppListener;
    private HfpListenerThread hfpListener;

    public BluetoothServer(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public synchronized void startServer() {
        if (running) {
            return;
        }
        running = true;

        // Ensure Bluetooth radio is discoverable
        try {
            LocalDevice localDevice = LocalDevice.getLocalDevice();
            localDevice.setDiscoverable(DiscoveryAgent.GIAC);
            LogManager.log("BT_SRV", "BlackBerry discoverable as: " + localDevice.getFriendlyName());
        } catch (Throwable t) {
            LogManager.error("BT_SRV", "setDiscoverable failed: " + t.getMessage());
        }

        // 1. Primary listener for Custom UUID (BB Compagnon Android app)
        String customUrl = "btspp://localhost:" + UUID_CUSTOM + ";name=BBSmartBridge;authorize=false;authenticate=false;encrypt=false;master=false";
        customListener = new ServerListenerThread(customUrl, "CustomUUID");
        customListener.start();

        // 2. Secondary listener for Standard SerialPort SPP UUID (0x1101 fallback)
        try {
            String sppUrl = "btspp://localhost:" + UUID_SPP + ";name=SmartBridgeSPP;authorize=false;authenticate=false;encrypt=false;master=false";
            sppListener = new ServerListenerThread(sppUrl, "SPP_1101");
            sppListener.start();
        } catch (Throwable t) {
            LogManager.log("BT_SRV", "Secondary SPP listener skipped: " + t.getMessage());
        }

        // 3. Hands-Free Profile (HFP 0x111E) listener & SDP Service Record registration
        try {
            String hfpUrl = "btspp://localhost:" + UUID_HFP_HF + ";name=BlackBerry Handsfree Unit;authorize=false;authenticate=false;encrypt=false;master=false";
            hfpListener = new HfpListenerThread(hfpUrl, "HFP_111E");
            hfpListener.start();
        } catch (Throwable t) {
            LogManager.log("BT_SRV", "HFP listener skipped: " + t.getMessage());
        }
    }

    public synchronized void stopServer() {
        running = false;
        if (customListener != null) {
            customListener.stopListener();
            customListener = null;
        }
        if (sppListener != null) {
            sppListener.stopListener();
            sppListener = null;
        }
        if (hfpListener != null) {
            hfpListener.stopListener();
            hfpListener = null;
        }
        cleanupActiveConnection();
    }

    public boolean isConnected() {
        return connected;
    }

    public void forceDisconnect() {
        cleanupActiveConnection();
    }

    public void cleanupActiveConnection() {
        boolean wasConnected;
        synchronized (connectionLock) {
            wasConnected = connected;
            connected = false;
            if (inputStream != null) {
                try { inputStream.close(); } catch (Throwable t) {}
                inputStream = null;
            }
            if (outputStream != null) {
                try { outputStream.close(); } catch (Throwable t) {}
                outputStream = null;
            }
            if (activeConnection != null) {
                try { activeConnection.close(); } catch (Throwable t) {}
                activeConnection = null;
            }
        }
        if (wasConnected && running) {
            LogManager.log("BT_SRV", "Connection closed, notifying ConnectionManager");
            connectionManager.onDisconnected();
        }
    }

    public synchronized void send(String data) {
        if (!connected || outputStream == null) {
            LogManager.error("BT_SRV", "Cannot send data: disconnected");
            return;
        }
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
        } catch (IOException e) {
            LogManager.error("BT_SRV", "Send error: " + e.getMessage());
            cleanupActiveConnection();
        }
    }

    /**
     * Handles an accepted incoming connection.
     */
    private void handleAcceptedConnection(StreamConnection conn, String listenerName) {
        synchronized (connectionLock) {
            if (connected && activeConnection != null) {
                LogManager.log("BT_SRV", "Already connected. Rejecting extra incoming connection from " + listenerName);
                try { conn.close(); } catch (Throwable t) {}
                return;
            }
            activeConnection = conn;
            connected = true;
        }

        try {
            LogManager.log("BT_SRV", "Connected via " + listenerName);
            inputStream = conn.openInputStream();
            outputStream = conn.openOutputStream();

            connectionManager.onConnected();

            // Read stream loop
            StringBuffer buffer = new StringBuffer();
            int ch;
            while (running && connected) {
                ch = inputStream.read();
                if (ch == -1) {
                    LogManager.log("BT_SRV", "Stream closed by peer");
                    break;
                }

                if (ch == '\n') {
                    String line = buffer.toString().trim();
                    buffer.setLength(0);
                    if (line.length() > 0) {
                        connectionManager.onDataReceived(line);
                    }
                } else if (ch != '\r') {
                    buffer.append((char) ch);
                    if (buffer.length() > 4096) {
                        LogManager.error("BT_SRV", "Buffer overflow, dropping line");
                        buffer.setLength(0);
                    }
                }
            }
        } catch (IOException e) {
            LogManager.error("BT_SRV", "Connection IO exception: " + e.getMessage());
        } finally {
            cleanupActiveConnection();
        }
    }

    /**
     * Background listener thread that accepts incoming Bluetooth connections.
     */
    private class ServerListenerThread extends Thread {
        private String url;
        private String name;
        private StreamConnectionNotifier notifier;
        private boolean active = true;

        public ServerListenerThread(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public void run() {
            while (running && active) {
                try {
                    LogManager.log("BT_SRV", "[" + name + "] Opening server socket...");
                    notifier = (StreamConnectionNotifier) Connector.open(url);
                    LogManager.log("BT_SRV", "[" + name + "] Listening for Android connection...");

                    while (running && active) {
                        StreamConnection conn = notifier.acceptAndOpen();
                        if (conn != null) {
                            handleAcceptedConnection(conn, name);
                        }
                    }
                } catch (Throwable t) {
                    if (running && active) {
                        LogManager.error("BT_SRV", "[" + name + "] Server socket error: " + t.getMessage());
                    }
                } finally {
                    if (notifier != null) {
                        try { notifier.close(); } catch (Throwable t) {}
                        notifier = null;
                    }
                }

                if (running && active) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {}
                }
            }
            LogManager.log("BT_SRV", "[" + name + "] Listener thread ended");
        }

        public void stopListener() {
            active = false;
            if (notifier != null) {
                try { notifier.close(); } catch (Throwable t) {}
            }
            this.interrupt();
        }
    }

    /**
     * Listener dédié pour le profil Bluetooth Mains-Libres (HFP - UUID 0x111E).
     * Enregistre l'enregistrement de service SDP (Service Discovery Protocol)
     * auprès de la pile Bluetooth locale RIM pour que le smartphone Android
     * reconnaisse le BlackBerry comme accessoire audio mains-libres.
     */
    private class HfpListenerThread extends Thread {
        private String url;
        private String name;
        private StreamConnectionNotifier notifier;
        private boolean active = true;

        public HfpListenerThread(String url, String name) {
            this.url = url;
            this.name = name;
        }

        public void run() {
            while (running && active) {
                try {
                    LogManager.log("BT_HFP", "[" + name + "] Opening HFP audio server socket...");
                    notifier = (StreamConnectionNotifier) Connector.open(url);

                    // Enrichir l'enregistrement de service SDP (ServiceRecord)
                    try {
                        LocalDevice localDev = LocalDevice.getLocalDevice();
                        ServiceRecord rec = localDev.getRecord(notifier);
                        if (rec != null) {
                            // 1. ServiceClassIDList (0x0001): Handsfree (0x111E) + GenericAudio (0x1203)
                            DataElement classSeq = new DataElement(DataElement.DATSEQ);
                            classSeq.addElement(new DataElement(DataElement.UUID, new UUID(0x111E)));
                            classSeq.addElement(new DataElement(DataElement.UUID, new UUID(0x1203)));
                            rec.setAttributeValue(0x0001, classSeq);

                            // 2. BluetoothProfileDescriptorList (0x0009): [UUID 0x111E, Version 0x0105]
                            DataElement profDesc = new DataElement(DataElement.DATSEQ);
                            DataElement hfpProf = new DataElement(DataElement.DATSEQ);
                            hfpProf.addElement(new DataElement(DataElement.UUID, new UUID(0x111E)));
                            hfpProf.addElement(new DataElement(DataElement.U_INT_2, 0x0105)); // HFP v1.5
                            profDesc.addElement(hfpProf);
                            rec.setAttributeValue(0x0009, profDesc);

                            // 3. ServiceName (0x0100)
                            rec.setAttributeValue(0x0100, new DataElement(DataElement.STRING, "BlackBerry Handsfree Unit"));

                            // 4. SupportedFeatures (0x0311): 0x001F (EC/NR, 3-way calling, CLI, Voice recog, Volume)
                            rec.setAttributeValue(0x0311, new DataElement(DataElement.U_INT_2, 0x001F));

                            localDev.updateRecord(rec);
                            LogManager.log("BT_HFP", "SDP Handsfree 0x111E service record registered and updated.");
                        }
                    } catch (Throwable sdpEx) {
                        LogManager.log("BT_HFP", "SDP registration notice: " + sdpEx.getMessage());
                    }

                    while (running && active) {
                        StreamConnection conn = notifier.acceptAndOpen();
                        if (conn != null) {
                            handleHfpConnection(conn);
                        }
                    }
                } catch (Throwable t) {
                    if (running && active) {
                        LogManager.log("BT_HFP", "[" + name + "] Server notice: " + t.getMessage());
                    }
                } finally {
                    if (notifier != null) {
                        try { notifier.close(); } catch (Throwable t) {}
                        notifier = null;
                    }
                }

                if (running && active) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {}
                }
            }
            LogManager.log("BT_HFP", "[" + name + "] Listener thread ended");
        }

        public void stopListener() {
            active = false;
            if (notifier != null) {
                try { notifier.close(); } catch (Throwable t) {}
            }
            this.interrupt();
        }
    }

    /**
     * Traite les échanges de signalisation AT du profil Mains-Libres Bluetooth (HFP)
     * lorsque le sous-système audio Bluetooth d'Android se connecte au canal HFP.
     */
    private void handleHfpConnection(final StreamConnection conn) {
        new Thread(new Runnable() {
            public void run() {
                LogManager.log("BT_HFP", "Android Bluetooth Audio stack connected to Handsfree Unit");
                InputStream is = null;
                OutputStream os = null;
                try {
                    is = conn.openInputStream();
                    os = conn.openOutputStream();
                    StringBuffer sb = new StringBuffer();
                    int ch;
                    while (running && (ch = is.read()) != -1) {
                        if (ch == '\r' || ch == '\n') {
                            String cmd = sb.toString().trim();
                            sb.setLength(0);
                            if (cmd.length() > 0) {
                                LogManager.log("BT_HFP", "RX AT: " + cmd);
                                String resp = null;
                                if (cmd.startsWith("AT+BRSF")) {
                                    resp = "\r\n+BRSF: 31\r\n\r\nOK\r\n";
                                } else if (cmd.startsWith("AT+CIND=?")) {
                                    resp = "\r\n+CIND: (\"service\",(0,1)),(\"call\",(0,1)),(\"callsetup\",(0,3)),(\"callheld\",(0,2)),(\"signal\",(0,5)),(\"roam\",(0,1)),(\"battchg\",(0,5))\r\n\r\nOK\r\n";
                                } else if (cmd.startsWith("AT+CIND?")) {
                                    resp = "\r\n+CIND: 1,0,0,0,5,0,5\r\n\r\nOK\r\n";
                                } else if (cmd.startsWith("AT+CMER")) {
                                    resp = "\r\nOK\r\n";
                                } else if (cmd.startsWith("AT+CLIP") || cmd.startsWith("AT+CCWA")) {
                                    resp = "\r\nOK\r\n";
                                } else if (cmd.startsWith("AT+VGS") || cmd.startsWith("AT+VGM")) {
                                    resp = "\r\nOK\r\n";
                                } else {
                                    resp = "\r\nOK\r\n";
                                }
                                if (resp != null) {
                                    os.write(resp.getBytes());
                                    os.flush();
                                }
                            }
                        } else {
                            sb.append((char) ch);
                        }
                    }
                } catch (Throwable t) {
                    LogManager.log("BT_HFP", "HFP channel closed: " + t.getMessage());
                } finally {
                    try { if (is != null) is.close(); } catch (Throwable t) {}
                    try { if (os != null) os.close(); } catch (Throwable t) {}
                    try { conn.close(); } catch (Throwable t) {}
                }
            }
        }).start();
    }

    /**
     * Connect to paired Android device (Client mode fallback).
     * Only queries paired devices in memory (no inquiry scan, no blue LED blinking).
     */
    public void connectToPairedDevice() {
        if (connected) {
            LogManager.log("BT_CLI", "Already connected");
            return;
        }

        Thread clientThread = new Thread(new Runnable() {
            public void run() {
                try {
                    LocalDevice localDevice = LocalDevice.getLocalDevice();
                    DiscoveryAgent agent = localDevice.getDiscoveryAgent();
                    RemoteDevice[] pairedDevices = agent.retrieveDevices(DiscoveryAgent.PREKNOWN);

                    if (pairedDevices == null || pairedDevices.length == 0) {
                        LogManager.log("BT_CLI", "No paired devices found. Please pair Android via BlackBerry Bluetooth settings.");
                        return;
                    }

                    LogManager.log("BT_CLI", "Scanning " + pairedDevices.length + " paired devices for SmartBridge service...");
                    UUID[] uuidSet = new UUID[] {
                        new UUID(UUID_CUSTOM, false),
                        new UUID(UUID_SPP, false)
                    };

                    for (int i = 0; i < pairedDevices.length; i++) {
                        if (connected || !running) break;
                        final RemoteDevice dev = pairedDevices[i];
                        final Object searchLock = new Object();
                        final String[] foundUrl = new String[1];

                        DiscoveryListener listener = new DiscoveryListener() {
                            public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {}
                            public void inquiryCompleted(int discType) {}
                            public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {
                                if (servRecord != null && servRecord.length > 0) {
                                    foundUrl[0] = servRecord[0].getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
                                }
                            }
                            public void serviceSearchCompleted(int transID, int respCode) {
                                synchronized (searchLock) {
                                    searchLock.notify();
                                }
                            }
                        };

                        synchronized (searchLock) {
                            try {
                                agent.searchServices(null, uuidSet, dev, listener);
                                searchLock.wait(8000);
                            } catch (Exception e) {}
                        }

                        if (foundUrl[0] != null) {
                            LogManager.log("BT_CLI", "Found service URL: " + foundUrl[0] + ". Connecting...");
                            StreamConnection clientConn = (StreamConnection) Connector.open(foundUrl[0]);
                            handleAcceptedConnection(clientConn, "PairedClient");
                            break;
                        }
                    }
                } catch (Throwable t) {
                    LogManager.error("BT_CLI", "connectToPairedDevice error: " + t.getMessage());
                }
            }
        });
        clientThread.start();
    }
}
