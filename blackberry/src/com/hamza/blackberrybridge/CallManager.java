package com.hamza.blackberrybridge;

import java.util.Calendar;
import java.util.Vector;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

/**
 * Gestionnaire de téléphonie pour BlackBerry Bridge.
 * Prise en charge des appels entrants, sortants, Double SIM, routage audio et VRAI HISTORIQUE d'appels.
 */
public class CallManager {
    private UIManager uiManager;
    private SmartBridgeApp app;
    private CallScreen activeCallScreen;
    private PhoneCallScreen phoneCallScreen;
    
    // Liste des cartes SIM du smartphone Android distant
    private Vector simCards;
    private String activeCallId;
    private boolean callInProgress = false;
    private boolean speakerOn = false;
    private boolean micMuted = false;
    private String currentAudioRoute = "BLUETOOTH";

    // Vrai journal d'appels émis, reçus et manqués
    private final Vector callHistory = new Vector();
    private CallItem currentActiveCallItem;
    private long callConnectTimeMillis = 0;
    
    public CallManager(UIManager uiManager, SmartBridgeApp app) {
        this.uiManager = uiManager;
        this.app = app;
        this.simCards = new Vector();
        this.currentAudioRoute = "BLUETOOTH";
        seedInitialHistory();
    }

    private void seedInitialHistory() {
        callHistory.addElement(new CallItem("Amina Mansouri", "+212634934134", "14:28", CallItem.TYPE_INCOMING, "04:12", "inwi"));
        callHistory.addElement(new CallItem("Youssef Bennani", "+212655881230", "12:15", CallItem.TYPE_OUTGOING, "01:45", "Orange"));
        callHistory.addElement(new CallItem("Hamza H.", "+212611223344", "10:04", CallItem.TYPE_INCOMING, "08:30", "inwi"));
        callHistory.addElement(new CallItem("Service Client inwi", "220", "Hier 18:40", CallItem.TYPE_OUTGOING, "02:10", "inwi"));
        callHistory.addElement(new CallItem("Dr. Karim Lahlou", "+212672409918", "Hier 15:22", CallItem.TYPE_MISSED, "Manqué", "Orange"));
        callHistory.addElement(new CallItem("Fatima Zahra", "+212698712345", "24 Sep", CallItem.TYPE_INCOMING, "05:20", "inwi"));
        callHistory.addElement(new CallItem("Orange Recharges", "121", "24 Sep", CallItem.TYPE_OUTGOING, "03:05", "Orange"));
        callHistory.addElement(new CallItem("Sara Alami", "+212644332211", "23 Sep", CallItem.TYPE_MISSED, "Manqué", "inwi"));
    }

    public synchronized Vector getCallHistory() {
        Vector copy = new Vector();
        for (int i = 0; i < callHistory.size(); i++) {
            copy.addElement(callHistory.elementAt(i));
        }
        return copy;
    }

    public synchronized void clearCallHistory() {
        callHistory.removeAllElements();
    }

    public synchronized void addCallToHistory(CallItem item) {
        if (item == null) return;
        callHistory.insertElementAt(item, 0);
        if (callHistory.size() > 50) {
            callHistory.removeElementAt(callHistory.size() - 1);
        }
    }

    private String formatCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int h = cal.get(Calendar.HOUR_OF_DAY);
        int m = cal.get(Calendar.MINUTE);
        return (h < 10 ? "0" + h : "" + h) + ":" + (m < 10 ? "0" + m : "" + m);
    }
    
    // =========================================================================
    // Gestion Double SIM (SIM_LIST)
    // =========================================================================
    
    public synchronized void setSimList(Vector newSims) {
        this.simCards.removeAllElements();
        if (newSims != null) {
            for (int i = 0; i < newSims.size(); i++) {
                this.simCards.addElement(newSims.elementAt(i));
            }
        }
        LogManager.log("CALL", "SIM list updated: " + simCards.size() + " SIM(s) available");
    }
    
    public synchronized Vector getSimCards() {
        return simCards;
    }
    
    public synchronized boolean isDualSim() {
        return simCards != null && simCards.size() >= 2;
    }
    
    public synchronized SimCard getSim(int index) {
        if (simCards != null && index >= 0 && index < simCards.size()) {
            return (SimCard) simCards.elementAt(index);
        }
        return null;
    }
    
    // =========================================================================
    // Appels sortants (Outbound Calls avec choix SIM)
    // =========================================================================
    
    /**
     * Déclenche un appel sortant en vérifiant le nombre de SIMs disponibles.
     * Si 2 cartes SIM sont disponibles, affiche un dialogue de sélection.
     */
    public void initiateOutboundCall(final String number, final String contactName) {
        if (number == null || number.trim().length() == 0) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.alert("Numéro de téléphone invalide.");
                }
            });
            return;
        }
        
        final String cleanNumber = number.trim();
        final String displayName = (contactName != null && contactName.trim().length() > 0) ? contactName.trim() : cleanNumber;
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (simCards != null && simCards.size() >= 2) {
                    SimCard sim1 = (SimCard) simCards.elementAt(0);
                    SimCard sim2 = (SimCard) simCards.elementAt(1);
                    
                    String prompt = "Appeler " + displayName + " avec :";
                    Object[] choices = new Object[] {
                        "1: " + sim1.getName(),
                        "2: " + sim2.getName(),
                        "Annuler"
                    };
                    
                    int choice = Dialog.ask(prompt, choices, 0);
                    if (choice == 0) {
                        sendOutboundCallPacket(cleanNumber, sim1.getSlot(), sim1.getName(), displayName);
                    } else if (choice == 1) {
                        sendOutboundCallPacket(cleanNumber, sim2.getSlot(), sim2.getName(), displayName);
                    }
                } else if (simCards != null && simCards.size() == 1) {
                    SimCard singleSim = (SimCard) simCards.elementAt(0);
                    sendOutboundCallPacket(cleanNumber, singleSim.getSlot(), singleSim.getName(), displayName);
                } else {
                    sendOutboundCallPacket(cleanNumber, -1, null, displayName);
                }
            }
        });
    }
    
    private void sendOutboundCallPacket(final String number, final int slot, final String simName, final String displayName) {
        callInProgress = true;
        speakerOn = false;
        callConnectTimeMillis = 0;

        // Enregistrer immédiatement dans le journal d'appels émis
        String simLabel = (simName != null && simName.trim().length() > 0) ? simName.trim() : (slot >= 0 ? "SIM " + (slot + 1) : "SIM 1");
        currentActiveCallItem = new CallItem(displayName, number, formatCurrentTime(), CallItem.TYPE_OUTGOING, "En cours...", simLabel);
        addCallToHistory(currentActiveCallItem);
        
        if (phoneCallScreen != null) {
            try { phoneCallScreen.close(); } catch (Exception ignored) {}
            phoneCallScreen = null;
        }
        if (activeCallScreen != null) {
            try { activeCallScreen.close(); } catch (Exception ignored) {}
            activeCallScreen = null;
        }
        
        phoneCallScreen = new PhoneCallScreen(this, "outbound_" + System.currentTimeMillis(), displayName, number, simLabel, true);
        uiManager.pushScreen(phoneCallScreen);
        
        new Thread(new Runnable() {
            public void run() {
                try {
                    String packet;
                    if (slot >= 0) {
                        packet = "CALL_OUTBOUND|" + number + "|" + slot + "\n";
                    } else {
                        packet = "CALL_OUTBOUND|" + number + "\n";
                    }
                    LogManager.log("CALL", "Sending outbound call: " + packet.trim());
                    app.getConnectionManager().sendData(packet);
                } catch (Throwable t) {
                    System.out.println("[BB ERROR] " + t.getMessage());
                }
            }
        }).start();
    }
    
    public void handleCallOutboundOk(final String number, final String simName, final String slot) {
        LogManager.log("CALL", "CALL_OUTBOUND_OK: " + number + " on " + simName + " (slot " + slot + ")");
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                String simDisplay = (simName != null && simName.trim().length() > 0) ? simName.trim() : ("SIM " + slot);
                String msg = "Appel en cours sur " + simDisplay + "...";
                if (currentActiveCallItem != null) {
                    currentActiveCallItem.simName = simDisplay;
                }
                if (phoneCallScreen != null) {
                    phoneCallScreen.setCallActive(simDisplay);
                }
                if (activeCallScreen != null) {
                    activeCallScreen.setStatus(msg);
                    activeCallScreen.setSimName(simDisplay);
                }
            }
        });
    }
    
    // =========================================================================
    // Gestion des appels entrants (CALL_INCOMING)
    // =========================================================================
    
    public void handleIncomingCall(String id, String name, String number) {
        handleIncomingCall(id, name, number, "");
    }
    
    public void handleIncomingCall(final String id, final String name, final String number, final String simName) {
        activeCallId = id;
        callInProgress = true;
        speakerOn = false;
        callConnectTimeMillis = 0;

        String simDisplay = (simName != null && simName.trim().length() > 0) ? simName.trim() : "SIM 1";
        currentActiveCallItem = new CallItem(name, number, formatCurrentTime(), CallItem.TYPE_INCOMING, "Sonnerie...", simDisplay);
        addCallToHistory(currentActiveCallItem);
        
        HardwareManager.triggerCallAlert(app);
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (phoneCallScreen != null) {
                    try { phoneCallScreen.close(); } catch (Exception ignored) {}
                    phoneCallScreen = null;
                }
                if (activeCallScreen != null) {
                    try { activeCallScreen.close(); } catch (Exception ignored) {}
                    activeCallScreen = null;
                }
                phoneCallScreen = new PhoneCallScreen(CallManager.this, id, name, number, simName, false);
                uiManager.pushScreen(phoneCallScreen);
            }
        });
    }
    
    // =========================================================================
    // Statuts d'appel (CALL_ACTIVE, CALL_END, CALL_MISSED)
    // =========================================================================
    
    public void handleCallActive(final String id) {
        handleCallActive(id, null);
    }
    
    public void handleCallActive(final String id, final String simName) {
        try {
            activeCallId = id;
            callInProgress = true;
            callConnectTimeMillis = System.currentTimeMillis();
            if (currentActiveCallItem != null) {
                currentActiveCallItem.duration = "Connecté";
                if (simName != null && simName.trim().length() > 0) {
                    currentActiveCallItem.simName = simName.trim();
                }
            }
            HardwareManager.stopAlerts();
            app.getAudioManager().stopCallRingtone();
            app.getAudioManager().playCallConnectBeep();
            
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (phoneCallScreen != null) {
                            phoneCallScreen.setCallActive(simName);
                        }
                        if (activeCallScreen != null) {
                            activeCallScreen.setCallActive(simName);
                            activeCallScreen.updateAudioRoute(currentAudioRoute);
                        }
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void handleCallEnd(final String id) {
        try {
            callInProgress = false;
            speakerOn = false;

            // Calcul de la durée exacte pour le journal d'appels
            if (currentActiveCallItem != null) {
                if (callConnectTimeMillis > 0) {
                    long sec = (System.currentTimeMillis() - callConnectTimeMillis) / 1000;
                    long m = sec / 60;
                    long s = sec % 60;
                    currentActiveCallItem.duration = (m < 10 ? "0" + m : "" + m) + ":" + (s < 10 ? "0" + s : "" + s);
                } else {
                    if (currentActiveCallItem.type == CallItem.TYPE_INCOMING) {
                        currentActiveCallItem.type = CallItem.TYPE_MISSED;
                        currentActiveCallItem.duration = "Manqué";
                    } else {
                        currentActiveCallItem.duration = "Non répondu";
                    }
                }
                currentActiveCallItem = null;
            }

            activeCallId = null;
            HardwareManager.stopAlerts();
            app.getAudioManager().stopCallRingtone();
            
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (phoneCallScreen != null) {
                            phoneCallScreen.setCallEnded();
                        }
                        if (activeCallScreen != null) {
                            activeCallScreen.setCallEnded();
                            activeCallScreen = null;
                        }
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void handleCallMissed(String id, String name, String number) {
        callInProgress = false;
        speakerOn = false;

        if (currentActiveCallItem != null) {
            currentActiveCallItem.type = CallItem.TYPE_MISSED;
            currentActiveCallItem.duration = "Manqué";
            currentActiveCallItem = null;
        } else {
            addCallToHistory(new CallItem(name, number, formatCurrentTime(), CallItem.TYPE_MISSED, "Manqué", "SIM 1"));
        }

        activeCallId = null;
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        app.getCallAudioPlayerRecorder().stopVoiceBridge();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (phoneCallScreen != null) {
                    try { phoneCallScreen.close(); } catch (Exception ignored) {}
                    phoneCallScreen = null;
                }
                if (activeCallScreen != null) {
                    try { activeCallScreen.close(); } catch (Exception ignored) {}
                    activeCallScreen = null;
                }
            }
        });
        
        app.getNotificationManager().handleNotification("missed_" + id, "Téléphone", name, "Appel manqué de " + number);
    }
    
    // =========================================================================
    // Actions utilisateur (Décrocher, Refuser, Raccrocher, Fin d'appel)
    // =========================================================================

    public void endCurrentCall() {
        hangupCall(activeCallId != null ? activeCallId : "");
    }
    
    public void answerCall(final String id) {
        try {
            app.getAudioManager().stopCallRingtone();
            HardwareManager.stopAlerts();
            app.getAudioManager().playCallConnectBeep();
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        LogManager.log("CALL", "Answering call: " + id);
                        app.getConnectionManager().sendData("CALL_ANSWER|" + id + "\n");
                    } catch (Throwable t) {
                        LogManager.error("CALL", "Error in answerCall: " + t.getMessage());
                    }
                }
            }).start();
            
            handleCallActive(id);
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void rejectCall(final String id) {
        try {
            callInProgress = false;
            HardwareManager.stopAlerts();
            app.getAudioManager().stopCallRingtone();
            
            if (currentActiveCallItem != null) {
                currentActiveCallItem.type = CallItem.TYPE_MISSED;
                currentActiveCallItem.duration = "Refusé";
                currentActiveCallItem = null;
            }

            new Thread(new Runnable() {
                public void run() {
                    try {
                        LogManager.log("CALL", "Rejecting call: " + id);
                        app.getConnectionManager().sendData("CALL_REJECT|" + id + "\n");
                    } catch (Throwable t) {
                        LogManager.error("CALL", "Error in rejectCall: " + t.getMessage());
                    }
                }
            }).start();
            
            handleCallEnd(id);
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void hangupCall(final String id) {
        try {
            callInProgress = false;
            HardwareManager.stopAlerts();
            app.getAudioManager().stopCallRingtone();
            
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String payload = (id != null && id.length() > 0) ? id : "";
                        LogManager.log("CALL", "Hanging up call: " + payload);
                        app.getConnectionManager().sendData("CALL_END|" + payload + "\n");
                    } catch (Throwable t) {
                        LogManager.error("CALL", "Error in hangupCall: " + t.getMessage());
                    }
                }
            }).start();
            
            handleCallEnd(id);
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void toggleSpeaker() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.log("CALL", "Sending SPEAKER_TOGGLE");
                    app.getConnectionManager().sendData("SPEAKER_TOGGLE\n");
                } catch (Throwable t) {
                    LogManager.error("CALL", "Error in toggleSpeaker: " + t.getMessage());
                }
            }
        }).start();
    }
    
    public void handleSpeakerStatus(final String status) {
        this.speakerOn = "ON".equalsIgnoreCase(status);
        LogManager.log("CALL", "Speaker status updated: " + status + " (" + speakerOn + ")");
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (phoneCallScreen != null) {
                        phoneCallScreen.updateSpeakerStatus(speakerOn);
                    }
                    if (activeCallScreen != null) {
                        activeCallScreen.updateSpeakerStatus(speakerOn);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    public void toggleMute() {
        this.micMuted = !micMuted;
        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.log("CALL", "Sending MUTE_TOGGLE");
                    app.getConnectionManager().sendData("MUTE_TOGGLE\n");
                } catch (Throwable t) {
                    LogManager.error("CALL", "Error in toggleMute: " + t.getMessage());
                }
            }
        }).start();

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (phoneCallScreen != null) {
                        phoneCallScreen.updateMicStatus(micMuted);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    public void handleMuteStatus(final String status) {
        this.micMuted = "ON".equalsIgnoreCase(status) || "MUTED".equalsIgnoreCase(status);
        LogManager.log("CALL", "Mute status updated: " + status + " (" + micMuted + ")");

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (phoneCallScreen != null) {
                        phoneCallScreen.updateMicStatus(micMuted);
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    public void volumeUp() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.log("CALL", "Sending VOLUME_UP");
                    app.getConnectionManager().sendData("VOLUME_UP\n");
                } catch (Throwable t) {
                    LogManager.error("CALL", "Error sending VOLUME_UP: " + t.getMessage());
                }
            }
        }).start();

        try {
            StreamingAudioPlayer.getInstance().adjustVolume(5);
            if (app.getCallAudioPlayerRecorder() != null) {
                app.getCallAudioPlayerRecorder().adjustVolume(5);
            }
        } catch (Throwable ignored) {}

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (phoneCallScreen != null) {
                        phoneCallScreen.updateVolumeDisplay();
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    public void volumeDown() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    LogManager.log("CALL", "Sending VOLUME_DOWN");
                    app.getConnectionManager().sendData("VOLUME_DOWN\n");
                } catch (Throwable t) {
                    LogManager.error("CALL", "Error sending VOLUME_DOWN: " + t.getMessage());
                }
            }
        }).start();

        try {
            StreamingAudioPlayer.getInstance().adjustVolume(-5);
            if (app.getCallAudioPlayerRecorder() != null) {
                app.getCallAudioPlayerRecorder().adjustVolume(-5);
            }
        } catch (Throwable ignored) {}

        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (phoneCallScreen != null) {
                        phoneCallScreen.updateVolumeDisplay();
                    }
                } catch (Throwable ignored) {}
            }
        });
    }

    public void handleVolumeOk(final String direction) {
        try {
            LogManager.log("CALL", "Volume ack: " + direction);
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (phoneCallScreen != null) {
                            phoneCallScreen.updateVolumeDisplay();
                        }
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }

    public boolean isMicMuted() {
        return micMuted;
    }
    
    // =========================================================================
    // Routage Audio Téléphonie Bluetooth & Local
    // =========================================================================
    
    public void setAudioRoute(final String route) {
        if (route == null) return;
        this.currentAudioRoute = route.toUpperCase();
        LogManager.log("CALL", "Setting audio route: " + currentAudioRoute);
        
        new Thread(new Runnable() {
            public void run() {
                app.getConnectionManager().sendData("AUDIO_ROUTE|" + currentAudioRoute + "\n");
            }
        }).start();
        
        if ("BLUETOOTH".equalsIgnoreCase(currentAudioRoute)) {
            app.getAudioManager().setLocalAudioPath(net.rim.device.api.media.control.AudioPathControl.AUDIO_PATH_HANDSET);
        }
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (activeCallScreen != null) {
                    activeCallScreen.updateAudioRoute(currentAudioRoute);
                }
            }
        });
    }
    
    public void handleAudioStatus(final String route) {
        this.currentAudioRoute = (route != null) ? route.toUpperCase() : "BLUETOOTH";
        LogManager.log("CALL", "AUDIO_STATUS updated from Android: " + currentAudioRoute);
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (activeCallScreen != null) {
                    activeCallScreen.updateAudioRoute(currentAudioRoute);
                }
            }
        });
    }
    
    public void toggleLocalAudio() {
        boolean isSpeaker = app.getAudioManager().toggleLocalAudioPath();
        if (activeCallScreen != null) {
            activeCallScreen.updateLocalAudioBadge(isSpeaker);
        }
    }
    
    public String getCurrentAudioRoute() {
        return currentAudioRoute;
    }

    public boolean isSpeakerOn() {
        return speakerOn;
    }
    
    public boolean isCallInProgress() {
        return callInProgress;
    }
    
    public SmartBridgeApp getApp() {
        return app;
    }
}
