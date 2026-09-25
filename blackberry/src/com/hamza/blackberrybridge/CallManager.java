package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

/**
 * Gestionnaire de téléphonie pour BlackBerry Bridge.
 * Prise en charge des appels entrants, sortants, Double SIM et routage audio (Haut-parleur).
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
    private String currentAudioRoute = "BLUETOOTH";
    
    public CallManager(UIManager uiManager, SmartBridgeApp app) {
        this.uiManager = uiManager;
        this.app = app;
        this.simCards = new Vector();
        this.currentAudioRoute = "BLUETOOTH";
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
        
        // Exécuté sur le thread UI pour afficher la boîte de dialogue si nécessaire
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
                    // Annuler ou touche Retour : ne rien envoyer
                } else if (simCards != null && simCards.size() == 1) {
                    SimCard singleSim = (SimCard) simCards.elementAt(0);
                    sendOutboundCallPacket(cleanNumber, singleSim.getSlot(), singleSim.getName(), displayName);
                } else {
                    // Aucune info SIM reçue : envoyer simplement CALL_OUTBOUND|<numero>
                    sendOutboundCallPacket(cleanNumber, -1, null, displayName);
                }
            }
        });
    }
    
    private void sendOutboundCallPacket(final String number, final int slot, final String simName, final String displayName) {
        callInProgress = true;
        speakerOn = false;
        
        // Ouvrir l'écran d'appel sortant PhoneCallScreen sur le BlackBerry
        if (phoneCallScreen != null) {
            try { phoneCallScreen.close(); } catch (Exception ignored) {}
            phoneCallScreen = null;
        }
        if (activeCallScreen != null) {
            try { activeCallScreen.close(); } catch (Exception ignored) {}
            activeCallScreen = null;
        }
        phoneCallScreen = new PhoneCallScreen(this, "outbound_" + System.currentTimeMillis(), displayName, number, simName != null ? simName : "", true);
        uiManager.pushScreen(phoneCallScreen);
        
        // Démarrer le streaming audio bidirectionnel dès l'envoi de l'appel
        app.getCallAudioPlayerRecorder().startVoiceBridge();
        
        // Envoi réseau sur un thread en arrière-plan
        new Thread(new Runnable() {
            public void run() {
                String packet;
                if (slot >= 0) {
                    packet = "CALL_OUTBOUND|" + number + "|" + slot + "\n";
                } else {
                    packet = "CALL_OUTBOUND|" + number + "\n";
                }
                LogManager.log("CALL", "Sending outbound call: " + packet.trim());
                app.getConnectionManager().sendData(packet);
            }
        }).start();
    }
    
    /**
     * Confirmation de lancement d'appel reçue de l'Android:
     * CALL_OUTBOUND_OK|<numero>|<nom_sim>|<slot>
     */
    public void handleCallOutboundOk(final String number, final String simName, final String slot) {
        LogManager.log("CALL", "CALL_OUTBOUND_OK: " + number + " on " + simName + " (slot " + slot + ")");
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                String simDisplay = (simName != null && simName.trim().length() > 0) ? simName.trim() : ("SIM " + slot);
                String msg = "Appel en cours sur " + simDisplay + "...";
                if (phoneCallScreen != null) {
                    phoneCallScreen.setCallActive(simDisplay);
                }
                if (activeCallScreen != null) {
                    activeCallScreen.setStatus(msg);
                    activeCallScreen.setSimName(simDisplay);
                }
                Dialog.inform(msg);
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
                activeCallScreen = new CallScreen(CallManager.this, id, name, number, simName, false);
                uiManager.pushScreen(activeCallScreen);
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
        activeCallId = id;
        callInProgress = true;
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        app.getAudioManager().playCallConnectBeep();
        
        // Démarrer la capture et la lecture audio temps réel
        app.getCallAudioPlayerRecorder().startVoiceBridge();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (phoneCallScreen != null) {
                    phoneCallScreen.setCallActive(simName);
                }
                if (activeCallScreen != null) {
                    activeCallScreen.setCallActive(simName);
                    activeCallScreen.updateAudioRoute(currentAudioRoute);
                }
            }
        });
    }
    
    public void handleCallEnd(final String id) {
        callInProgress = false;
        activeCallId = null;
        speakerOn = false;
        HardwareManager.stopAlerts();
        app.getAudioManager().stopCallRingtone();
        app.getAudioManager().playCallEndBeep();
        
        // Arrêter immédiatement le pont audio
        app.getCallAudioPlayerRecorder().stopVoiceBridge();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (phoneCallScreen != null) {
                    phoneCallScreen.setCallEnded();
                    phoneCallScreen = null;
                }
                if (activeCallScreen != null) {
                    activeCallScreen.setCallEnded();
                    activeCallScreen = null;
                }
            }
        });
    }
    
    public void handleCallMissed(String id, String name, String number) {
        callInProgress = false;
        activeCallId = null;
        speakerOn = false;
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
    // Actions utilisateur (Décrocher, Refuser, Raccrocher, Haut-Parleur, Audio Routing)
    // =========================================================================
    
    public void answerCall(final String id) {
        app.getAudioManager().stopCallRingtone();
        HardwareManager.stopAlerts();
        app.getAudioManager().playCallConnectBeep();
        
        // Démarrer le streaming audio bidirectionnel dès que l'utilisateur décroche
        app.getCallAudioPlayerRecorder().startVoiceBridge();
        
        new Thread(new Runnable() {
            public void run() {
                app.getConnectionManager().sendData("CALL_ANSWER|" + id + "\n");
            }
        }).start();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (activeCallScreen != null) {
                    try { activeCallScreen.close(); } catch (Exception ignored) {}
                    activeCallScreen = null;
                }
                if (phoneCallScreen == null) {
                    phoneCallScreen = new PhoneCallScreen(CallManager.this, id, "Appel", "", "", false);
                    uiManager.pushScreen(phoneCallScreen);
                }
            }
        });
    }
    
    public void rejectCall(final String id) {
        app.getAudioManager().stopCallRingtone();
        HardwareManager.stopAlerts();
        app.getCallAudioPlayerRecorder().stopVoiceBridge();
        callInProgress = false;
        activeCallId = null;
        
        new Thread(new Runnable() {
            public void run() {
                app.getConnectionManager().sendData("CALL_REJECT|" + id + "\n");
            }
        }).start();
        
        if (phoneCallScreen != null) {
            try { phoneCallScreen.close(); } catch (Exception ignored) {}
            phoneCallScreen = null;
        }
        if (activeCallScreen != null) {
            try { activeCallScreen.close(); } catch (Exception ignored) {}
            activeCallScreen = null;
        }
    }
    
    public void endCurrentCall() {
        app.getAudioManager().stopCallRingtone();
        HardwareManager.stopAlerts();
        app.getAudioManager().playCallEndBeep();
        app.getCallAudioPlayerRecorder().stopVoiceBridge();
        callInProgress = false;
        activeCallId = null;
        speakerOn = false;
        
        new Thread(new Runnable() {
            public void run() {
                app.getConnectionManager().sendData("CALL_END\n");
            }
        }).start();
        
        if (phoneCallScreen != null) {
            phoneCallScreen.setCallEnded();
            phoneCallScreen = null;
        }
        if (activeCallScreen != null) {
            activeCallScreen.setCallEnded();
            activeCallScreen = null;
        }
    }
    
    public void toggleSpeaker() {
        new Thread(new Runnable() {
            public void run() {
                LogManager.log("CALL", "Sending SPEAKER_TOGGLE");
                app.getConnectionManager().sendData("SPEAKER_TOGGLE\n");
            }
        }).start();
    }
    
    public void handleSpeakerStatus(final String status) {
        this.speakerOn = "ON".equalsIgnoreCase(status);
        LogManager.log("CALL", "Speaker status updated: " + status + " (" + speakerOn + ")");
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (activeCallScreen != null) {
                    activeCallScreen.updateSpeakerStatus(speakerOn);
                }
            }
        });
    }
    
    // =========================================================================
    // Routage Audio Téléphonie Bluetooth & Local
    // =========================================================================
    
    /**
     * Envoie la commande de sélection de route audio au smartphone Android :
     * - AUDIO_ROUTE|BLUETOOTH    -> Audio Bluetooth / BlackBerry
     * - AUDIO_ROUTE|SPEAKERPHONE -> Haut-parleur Smartphone
     * - AUDIO_ROUTE|EARPIECE     -> Écouteur Smartphone
     */
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
            // S'assurer que le routage audio local RIM est enclenché
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
    
    /**
     * Confirmation de route audio reçue d'Android : AUDIO_STATUS|<ROUTE>
     */
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
    
    /**
     * Bascule la sortie locale BlackBerry entre le combiné (écouteur) et le haut-parleur physique.
     */
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
