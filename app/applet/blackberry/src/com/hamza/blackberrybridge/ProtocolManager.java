package com.hamza.blackberrybridge;

import java.util.Vector;
import com.hamza.blackberrybridge.audio.BlackBerryAudioStreamer;
import com.hamza.blackberrybridge.audio.BluetoothAudioReceiver;

/**
 * Gestionnaire du protocole Bluetooth RFCOMM bidirectionnel BSB/1.
 * Optimisé pour le streaming audio temps réel (VOICE_TX / AUDIO_CHUNK) et la téléphonie double SIM.
 */
public class ProtocolManager {
    private ConnectionManager connectionManager;
    private SmartBridgeApp app;
    
    public ProtocolManager(ConnectionManager connectionManager, SmartBridgeApp app) {
        this.connectionManager = connectionManager;
        this.app = app;
    }
    
    public void processMessage(String message) {
        if (message == null || message.length() == 0) return;
        
        // 1. Traitement ultra-rapide des flux audio WAV autonomes (500ms ou 200ms)
        // Moteur séquentiel unique sur thread dédié à haute priorité (Thread.MAX_PRIORITY)
        if (message.startsWith("AUDIO_CHUNK|")) {
            BlackBerryAudioStreamer.getInstance().enqueueChunk(message.substring(12));
            return;
        }
        if (message.startsWith("VOICE_TX|")) {
            BlackBerryAudioStreamer.getInstance().enqueueChunk(message.substring(9));
            return;
        }
        if (message.startsWith("AUDIO_START") || message.startsWith("VOICE_START")) {
            BlackBerryAudioStreamer.getInstance().startAudio();
            return;
        }
        if (message.equals("AUDIO_STOP") || message.equals("VOICE_STOP")) {
            BlackBerryAudioStreamer.getInstance().stopAudio();
            return;
        }
        if (message.startsWith("VOICE_RX|") || message.startsWith("VOICE_BRIDGE")) {
            return;
        }

        String[] parts = split(message, '|');
        if (parts.length == 0) return;
        
        String command = parts[0];
        LogManager.log("PROTOCOL", "Processing command: " + command);
        
        try {
            if (command.equals("PING")) {
                connectionManager.sendData("PONG\n");
            } 
            else if (command.equals("PONG")) {
                // Heartbeat response from Android
            }
            else if (command.equals("HELLO")) {
                connectionManager.sendData("HELLO|BSB/1|BLACKBERRY_9790\n");
                connectionManager.sendData("READY\n");
            }
            else if (command.equals("READY")) {
                // Handshake acknowledged by Android
            }
            else if (command.equals("PHONE_BATTERY") || command.equals("BATTERY")) {
                if (parts.length > 1) app.getUIManager().updateBattery(parts[1]);
            }
            else if (command.equals("GET_PHONE_BATTERY") || command.equals("GET_BATTERY")) {
                connectionManager.sendData("BATTERY|" + BatteryManager.getBatteryLevel() + "\n");
            }
            else if (command.equals("NOTIFICATION")) {
                if (parts.length >= 5) {
                    app.getNotificationManager().handleNotification(parts[1], parts[2], parts[3], parts[4]);
                } else {
                    connectionManager.sendData("ERROR|INVALID_PACKET\n");
                }
            }
            else if (command.equals("SIM_LIST")) {
                int count = 0;
                if (parts.length >= 2) {
                    try { count = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                }
                Vector sims = new Vector();
                int idx = 2;
                for (int i = 0; i < count && idx < parts.length; i++) {
                    String simName = parts[idx];
                    int slot = i;
                    if (idx + 1 < parts.length) {
                        try { slot = Integer.parseInt(parts[idx + 1].trim()); } catch (Exception ignored) {}
                    }
                    sims.addElement(new SimCard(simName, slot));
                    idx += 2;
                }
                app.getCallManager().setSimList(sims);
            }
            else if (command.equals("CALL_OUTBOUND_OK")) {
                String num = parts.length >= 2 ? parts[1] : "";
                String sim = parts.length >= 3 ? parts[2] : "";
                String slot = parts.length >= 4 ? parts[3] : "";
                app.getCallManager().handleCallOutboundOk(num, sim, slot);
            }
            else if (command.equals("CALL_INCOMING")) {
                if (parts.length >= 5) {
                    app.getCallManager().handleIncomingCall(parts[1], parts[2], parts[3], parts[4]);
                } else if (parts.length >= 4) {
                    app.getCallManager().handleIncomingCall(parts[1], parts[2], parts[3], "");
                } else {
                    connectionManager.sendData("ERROR|INVALID_PACKET\n");
                }
            }
            else if (command.equals("CALL_ACTIVE")) {
                String id = (parts.length >= 2) ? parts[1] : "";
                String sim = (parts.length >= 3) ? parts[2] : null;
                app.getCallManager().handleCallActive(id, sim);
            }
            else if (command.equals("CALL_END")) {
                String id = (parts.length >= 2) ? parts[1] : "";
                BlackBerryAudioStreamer.getInstance().stopAudio();
                app.getCallManager().handleCallEnd(id);
            }
            else if (command.equals("CALL_MISSED")) {
                if (parts.length >= 4) app.getCallManager().handleCallMissed(parts[1], parts[2], parts[3]);
            }
            else if (command.equals("AUDIO_START") || command.equals("VOICE_START")) {
                BlackBerryAudioStreamer.getInstance().startAudio();
            }
            else if (command.equals("AUDIO_CHUNK") || command.equals("VOICE_TX")) {
                if (parts.length > 1) {
                    BlackBerryAudioStreamer.getInstance().enqueueChunk(parts[1]);
                }
            }
            else if (command.equals("AUDIO_STOP") || command.equals("VOICE_STOP")) {
                BlackBerryAudioStreamer.getInstance().stopAudio();
            }
            else if (command.equals("AUDIO_PLAYBACK_START")) {
                BlackBerryAudioStreamer.getInstance().startAudio();
            }
            else if (command.equals("AUDIO_PLAYBACK_STOP")) {
                BlackBerryAudioStreamer.getInstance().stopAudio();
            }
            else if (command.equals("SPEAKER_STATUS")) {
                String status = (parts.length >= 2) ? parts[1] : "OFF";
                app.getCallManager().handleSpeakerStatus(status);
            }
            else if (command.equals("MUTE_STATUS")) {
                String status = (parts.length >= 2) ? parts[1] : "OFF";
                app.getCallManager().handleMuteStatus(status);
            }
            else if (command.equals("VOLUME_OK")) {
                String dir = (parts.length >= 2) ? parts[1] : "";
                app.getCallManager().handleVolumeOk(dir);
            }
            else if (command.equals("AUDIO_STATUS") || command.equals("AUDIO_ROUTE")) {
                String route = (parts.length >= 2) ? parts[1] : "BLUETOOTH";
                app.getCallManager().handleAudioStatus(route);
            }
            else if (command.equals("CONTACTS_CLEAR")) {
                app.getContactManager().clearContacts();
            }
            else if (command.equals("CONTACTS_START")) {
                int count = 10;
                String type = "VIP";
                if (parts.length >= 2) {
                    try { count = Integer.parseInt(parts[1].trim()); } catch (Exception ignored) {}
                }
                if (parts.length >= 3) {
                    type = parts[2];
                }
                app.getContactManager().startContactsBatch(count, type);
            }
            else if (command.equals("CONTACT")) {
                if (parts.length >= 4) {
                    app.getContactManager().handleContact(parts[1], parts[2], parts[3]);
                } else if (parts.length == 3) {
                    app.getContactManager().handleContact("", parts[1], parts[2]);
                }
            }
            else if (command.equals("CONTACTS_END")) {
                String countStr = parts.length >= 2 ? parts[1] : "";
                app.getContactManager().handleContactsEnd(countStr);
            }
            else if (command.equals("SMS")) {
                if (parts.length >= 4) {
                    app.getUIManager().showNewMessagePopup(parts[1], parts[2], parts[3]);
                    HardwareManager.triggerMessageAlert();
                } else {
                    connectionManager.sendData("ERROR|INVALID_PACKET\n");
                }
            }
            else if (command.equals("WEATHER")) {
                if (parts.length >= 5) {
                    app.getUIManager().updateWeather(parts[1], parts[2], parts[3], parts[4]);
                } else if (parts.length >= 3) {
                    app.getUIManager().updateWeather(parts[1], "C", parts[2], "");
                }
            }
            else if (command.equals("MEDIA")) {
                if (parts.length >= 4) {
                    app.getMediaManager().updateMedia(parts[1], parts[2], parts[3]);
                }
            }
            else if (command.equals("FIND_PHONE")) {
                if (parts.length >= 2) {
                    if (parts[1].equals("START")) {
                        app.getUIManager().showFindPhonePopup();
                        HardwareManager.startFindPhoneAlert();
                    } else if (parts[1].equals("STOP")) {
                        app.getUIManager().hideFindPhonePopup();
                        HardwareManager.stopFindPhoneAlert();
                    }
                }
            }
            else if (command.equals("PHONE_FOUND") || command.equals("FIND_PHONE_STOPPED")) {
                app.getUIManager().onPhoneFound();
            }
            else if (command.equals("CELL_TELEMETRY") || command.equals("NETWORK_STATUS")) {
                if (parts.length >= 5) {
                    String operator = parts[1];
                    String netType = parts[2];
                    int signalBars = 0;
                    try {
                        signalBars = Integer.parseInt(parts[3]);
                    } catch (Exception ex) {
                        signalBars = 0;
                    }
                    String status = parts[4];
                    app.getUIManager().updateNetworkTelemetry(operator, netType, signalBars, status);
                } else if (parts.length >= 4) {
                    String operator = parts[1];
                    String netType = parts[2];
                    int signalBars = 0;
                    try {
                        signalBars = Integer.parseInt(parts[3]);
                    } catch (Exception ex) {
                        signalBars = 0;
                    }
                    app.getUIManager().updateNetworkTelemetry(operator, netType, signalBars, "ONLINE");
                }
            }
            else if (command.equals("CLIPBOARD")) {
                // Clipboard sync disabled due to signature requirement
            }
            else {
                LogManager.log("PROTOCOL", "Ignored command: " + command);
            }
        } catch (Exception e) {
            LogManager.error("PROTOCOL", "Parse error: " + e.getMessage());
        }
    }
    
    private String[] split(String str, char separator) {
        Vector nodes = new Vector();
        int index = str.indexOf(separator);
        while (index >= 0) {
            nodes.addElement(str.substring(0, index));
            str = str.substring(index + 1);
            index = str.indexOf(separator);
        }
        nodes.addElement(str);
        
        String[] result = new String[nodes.size()];
        if (nodes.size() > 0) {
            for (int loop = 0; loop < nodes.size(); loop++) {
                result[loop] = (String) nodes.elementAt(loop);
            }
        }
        return result;
    }
}
