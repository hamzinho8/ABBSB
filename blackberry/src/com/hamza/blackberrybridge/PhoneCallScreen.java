package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.system.Alert;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.SeparatorField;
import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import net.rim.device.api.ui.decor.BackgroundFactory;

/**
 * SmartWatch In-Call Screen (PhoneCallScreen) pour BlackBerry Curve 9300 (RIM OS 5.0 à 7.1).
 * 
 * Mode SmartWatch Compagnon :
 * - Le smartphone Android diffuse la voix sur son haut-parleur mains-libres amplifié.
 * - Le BlackBerry Curve 9300 sert de télécommande complète pour piloter les appels.
 * 
 * Spécifications de l'écran 320x240 :
 * - Titre : "Appel en cours [SIM: inwi / Orange]"
 * - Corps :
 *   * Nom du contact en grand (Font Bold 22pt)
 *   * Numéro en dessous (Font Normal 16pt)
 *   * Durée : "00:00" incrémentée chaque seconde dès réception de CALL_ACTIVE
 *   * Statut : "Mains-libres smartphone : ACTIF"
 * 
 * Touches physiques du Curve 9300 :
 * - Touche Fin d'appel (Rouge / KEY_END ou ESCAPE) : Envoie CALL_END, affiche "Fin d'appel..." et ferme l'écran.
 * - Touches Volume physique (+ / - sur le côté droit) : Envoie "VOLUME_UP" ou "VOLUME_DOWN".
 * - Touche Espace ou Clic Trackpad : Envoie "MUTE_TOGGLE" pour couper/activer le micro du smartphone.
 * - Touche M : Envoie "SPEAKER_TOGGLE" pour basculer le haut-parleur du smartphone.
 * - Touche Verte (KEY_SEND) : Décroche si appel entrant.
 * 
 * Gestion de fin d'appel (CALL_END) :
 * - Arrêter immédiatement le chronomètre d'appel.
 * - Afficher en rouge au centre de l'écran : "Appel terminé".
 * - Émettre un bip court de fin d'appel : Alert.startBuzzer(150);
 * - Fermeture automatique de PhoneCallScreen après 1,5 seconde via Timer et UiApplication.invokeLater.
 * 
 * 100% Asynchrone & Protection Anti-Exception intégrale (try / catch Throwable).
 */
public class PhoneCallScreen extends MainScreen {
    private CallManager callManager;
    private String callId;
    private String name;
    private String number;
    private String simName;
    private boolean isOutbound;
    private boolean isRinging;
    private boolean isActive;
    private boolean isEnded = false;
    
    // États matériels du SmartWatch Call Screen
    private boolean speakerOn = true; // Actif par défaut en mode compagnon mains-libres
    private boolean micMuted = false;
    
    // Chronomètre d'appel (seconde par seconde)
    private Timer callTimer;
    private int durationSeconds = 0;
    
    // Composants visuels UI
    private DarkLabelField headerLabel;
    private DarkLabelField nameLabel;
    private DarkLabelField numberLabel;
    private DarkLabelField timerLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField microBadge;
    private DarkLabelField hintLabel;
    
    // Boutons tactiles / trackpad
    private HorizontalFieldManager buttonsManager;
    private DarkButtonField btnHangup;
    private DarkButtonField btnAnswer;
    private DarkButtonField btnMute;
    private DarkButtonField btnSpeaker;

    public PhoneCallScreen(CallManager cm, String id, String name, String number, String simName, boolean isOutbound) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        try {
            this.callManager = cm;
            this.callId = (id != null && id.length() > 0) ? id : "call_" + System.currentTimeMillis();
            this.name = (name != null && name.trim().length() > 0) ? name.trim() : "Inconnu";
            this.number = (number != null && number.trim().length() > 0) ? number.trim() : "";
            this.simName = (simName != null && simName.trim().length() > 0) ? simName.trim() : "SIM 1";
            this.isOutbound = isOutbound;
            this.isRinging = !isOutbound;
            this.isActive = isOutbound;
            this.isEnded = false;
            
            if (cm != null) {
                this.speakerOn = cm.isSpeakerOn();
                this.micMuted = cm.isMicMuted();
            }
            
            // Fond noir profond BlackBerry Curve 9300
            getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
            
            VerticalFieldManager content = new VerticalFieldManager(Field.FIELD_HCENTER);
            content.setPadding(4, 6, 4, 6);
            
            // Titre : "Appel en cours [SIM: inwi / Orange]" ou "Appel entrant [SIM: inwi / Orange]"
            String titleText = isRinging ? ("Appel entrant [SIM: " + this.simName + "]") 
                                         : ("Appel en cours [SIM: " + this.simName + "]");
            setTitle(new LabelField(titleText, Field.FIELD_HCENTER));
            
            headerLabel = new DarkLabelField(titleText, Field.FIELD_HCENTER, isRinging ? 0x00FFCC : 0x00E5FF);
            try { 
                headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 12)); 
            } catch (Throwable ignored) {}
            content.add(headerLabel);
            
            content.add(new SeparatorField());
            
            // 1. Nom du contact en grand (Font Bold 22pt)
            nameLabel = new DarkLabelField(this.name, Field.FIELD_HCENTER, Color.WHITE);
            try { 
                nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 22)); 
            } catch (Throwable ignored) {
                try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 18)); } catch (Throwable t2) {}
            }
            content.add(nameLabel);
            
            // 2. Numéro en dessous (Font Normal 16pt)
            numberLabel = new DarkLabelField(this.number, Field.FIELD_HCENTER, 0x94A3B8);
            try { 
                numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 16)); 
            } catch (Throwable ignored) {
                try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch (Throwable t2) {}
            }
            content.add(numberLabel);
            
            // 3. Durée : "00:00" incrémentée chaque seconde dès réception de CALL_ACTIVE
            timerLabel = new DarkLabelField(isActive ? "00:00" : "--:--", Field.FIELD_HCENTER, 0x22C55E);
            try { 
                timerLabel.setFont(Font.getDefault().derive(Font.BOLD, 18)); 
            } catch (Throwable ignored) {}
            content.add(timerLabel);
            
            // 4. Statut : "Mains-libres smartphone : ACTIF"
            statusLabel = new DarkLabelField(buildStatusText(), Field.FIELD_HCENTER, 0x00E5FF);
            try { 
                statusLabel.setFont(Font.getDefault().derive(Font.BOLD, 13)); 
            } catch (Throwable ignored) {}
            content.add(statusLabel);
            
            // Indicateur du microphone smartphone
            microBadge = new DarkLabelField(buildMicroText(), Field.FIELD_HCENTER, micMuted ? 0xF59E0B : 0x38BDF8);
            try { 
                microBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); 
            } catch (Throwable ignored) {}
            content.add(microBadge);
            
            // Boutons d'action tactiles / trackpad
            buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
            buttonsManager.setPadding(4, 0, 2, 0);
            rebuildButtons();
            content.add(buttonsManager);
            
            // Légende des touches physiques Curve 9300
            String hints = isRinging ? "[Verte: Décrocher | Rouge: Refuser]" 
                                     : "[Fin: Rouge | Espace: Mute | M: HP | Vol +/-]";
            hintLabel = new DarkLabelField(hints, Field.FIELD_HCENTER, 0x64748B);
            try { 
                hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); 
            } catch (Throwable ignored) {}
            content.add(hintLabel);
            
            add(content);
            
            if (isActive && !isOutbound) {
                startTimer();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    private String buildStatusText() {
        try {
            if (isEnded) return "Appel terminé";
            if (isRinging) return "Sonnerie en cours...";
            if (isOutbound && !isActive) return "Numérotation en cours...";
            return speakerOn ? "Mains-libres smartphone : ACTIF" : "Mains-libres smartphone : INACTIF";
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
            return "Mains-libres smartphone : ACTIF";
        }
    }
    
    private String buildMicroText() {
        try {
            if (isEnded) return "";
            return micMuted ? "Micro smartphone : MUTÉ" : "Micro smartphone : ACTIF";
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
            return "";
        }
    }
    
    /**
     * Bip court de notification de fin d'appel (150 ms)
     */
    public static void startBuzzer(int ms) {
        try {
            if (Alert.isBuzzerSupported()) {
                Alert.startBuzzer(new short[] { 1000, (short) ms }, 100);
            } else if (Alert.isAudioSupported()) {
                Alert.startAudio(new short[] { 1000, (short) ms }, 100);
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    private void rebuildButtons() {
        try {
            buttonsManager.deleteAll();
            if (isEnded) return;
            
            if (isRinging) {
                btnAnswer = new DarkButtonField("Décrocher", 115, 30, DarkButtonField.STYLE_GREEN);
                btnAnswer.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        answer();
                    }
                });
                buttonsManager.add(btnAnswer);
                
                HorizontalFieldManager sp = new HorizontalFieldManager();
                sp.setPadding(0, 4, 0, 4);
                buttonsManager.add(sp);
                
                btnHangup = new DarkButtonField("Refuser", 115, 30, DarkButtonField.STYLE_RED);
                btnHangup.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        handlePhysicalHangup();
                    }
                });
                buttonsManager.add(btnHangup);
            } else {
                btnHangup = new DarkButtonField("Raccrocher", 95, 28, DarkButtonField.STYLE_RED);
                btnHangup.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        handlePhysicalHangup();
                    }
                });
                buttonsManager.add(btnHangup);
                
                HorizontalFieldManager sp1 = new HorizontalFieldManager();
                sp1.setPadding(0, 2, 0, 2);
                buttonsManager.add(sp1);
                
                String muteLabel = micMuted ? "Micro: OFF" : "Micro: ON";
                btnMute = new DarkButtonField(muteLabel, 95, 28, micMuted ? DarkButtonField.STYLE_GOLD : DarkButtonField.STYLE_SLATE);
                btnMute.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        toggleMute();
                    }
                });
                buttonsManager.add(btnMute);
                
                HorizontalFieldManager sp2 = new HorizontalFieldManager();
                sp2.setPadding(0, 2, 0, 2);
                buttonsManager.add(sp2);
                
                String spkLabel = speakerOn ? "HP: ON" : "HP: OFF";
                btnSpeaker = new DarkButtonField(spkLabel, 105, 28, speakerOn ? DarkButtonField.STYLE_CYAN : DarkButtonField.STYLE_SLATE);
                btnSpeaker.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        toggleSpeaker();
                    }
                });
                buttonsManager.add(btnSpeaker);
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    private void startTimer() {
        try {
            stopTimer();
            durationSeconds = 0;
            callTimer = new Timer();
            callTimer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    try {
                        if (isActive && !isEnded) {
                            durationSeconds++;
                            UiApplication.getUiApplication().invokeLater(new Runnable() {
                                public void run() {
                                    updateDurationDisplay();
                                }
                            });
                        }
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            }, 1000, 1000);
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    private void stopTimer() {
        try {
            if (callTimer != null) {
                callTimer.cancel();
                callTimer = null;
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    private void updateDurationDisplay() {
        try {
            if (timerLabel != null && isActive && !isEnded) {
                int m = durationSeconds / 60;
                int s = durationSeconds % 60;
                String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                timerLabel.setText(timeStr);
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Décroche l'appel (Touche Verte ou bouton Décrocher)
     */
    public void answer() {
        try {
            if (isRinging) {
                isRinging = false;
                isActive = true;
                if (callManager != null) {
                    callManager.answerCall(callId);
                }
                
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            if (headerLabel != null) {
                                headerLabel.setText("Appel en cours [SIM: " + simName + "]");
                                headerLabel.setColor(0x00E5FF);
                            }
                            if (statusLabel != null) {
                                statusLabel.setText(buildStatusText());
                                statusLabel.setColor(0x00E5FF);
                            }
                            if (timerLabel != null) {
                                timerLabel.setText("00:00");
                            }
                            rebuildButtons();
                        } catch (Throwable t) {
                            System.out.println("[BB ERROR] " + t.getMessage());
                        }
                    }
                });
                startTimer();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Reçoit CALL_ACTIVE de Android : active le chronomètre et l'affichage.
     */
    public void setCallActive(final String simOverride) {
        try {
            this.isActive = true;
            this.isRinging = false;
            if (simOverride != null && simOverride.trim().length() > 0) {
                this.simName = simOverride.trim();
            }
            
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (headerLabel != null) {
                            headerLabel.setText("Appel en cours [SIM: " + simName + "]");
                            headerLabel.setColor(0x00E5FF);
                        }
                        if (statusLabel != null) {
                            statusLabel.setText(buildStatusText());
                            statusLabel.setColor(0x00E5FF);
                        }
                        if (microBadge != null) {
                            microBadge.setText(buildMicroText());
                        }
                        rebuildButtons();
                        updateDurationDisplay();
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
            startTimer();
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Fin d'appel (CALL_END reçu ou déclenché) :
     * 1. Arrêter immédiatement le chronomètre d'appel.
     * 2. Afficher en rouge au centre de l'écran : "Appel terminé".
     * 3. Émettre un bip court de fin d'appel : Alert.startBuzzer(150);
     * 4. Programmer la fermeture automatique de PhoneCallScreen après 1,5 seconde.
     */
    public void setCallEnded() {
        try {
            if (isEnded) return;
            this.isEnded = true;
            this.isActive = false;
            this.isRinging = false;
            
            // 1. Arrêter immédiatement le chronomètre d'appel
            stopTimer();
            
            // 2. Émettre un bip court de fin d'appel : Alert.startBuzzer(150);
            startBuzzer(150);
            
            // 3. Afficher en rouge au centre de l'écran : "Appel terminé"
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (statusLabel != null) {
                            statusLabel.setText("Appel terminé");
                            statusLabel.setColor(0xFF2222); // Rouge vif
                        }
                        if (headerLabel != null) {
                            headerLabel.setText("APPEL TERMINÉ");
                            headerLabel.setColor(0xFF2222);
                        }
                        if (timerLabel != null) {
                            int m = durationSeconds / 60;
                            int s = durationSeconds % 60;
                            String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                            timerLabel.setText(timeStr);
                            timerLabel.setColor(0xFF6666);
                        }
                        if (microBadge != null) {
                            microBadge.setText("Déconnexion...");
                            microBadge.setColor(0x888888);
                        }
                        rebuildButtons();
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
            
            // 4. Programmer la fermeture automatique de PhoneCallScreen après 1,5 seconde
            Timer closeTimer = new Timer();
            closeTimer.schedule(new TimerTask() {
                public void run() {
                    UiApplication.getUiApplication().invokeLater(new Runnable() {
                        public void run() {
                            try {
                                UiApplication.getUiApplication().popScreen(PhoneCallScreen.this);
                            } catch (Throwable t) {
                                try { close(); } catch (Throwable ignored) {}
                                System.out.println("[BB ERROR] " + t.getMessage());
                            }
                        }
                    });
                }
            }, 1500);
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Touche Fin d'appel physique (Rouge / KEY_END) :
     * Envoie le paquet Bluetooth "CALL_END", affiche "Fin d'appel..." et ferme l'écran.
     */
    private void handlePhysicalHangup() {
        try {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (statusLabel != null) {
                            statusLabel.setText("Fin d'appel...");
                            statusLabel.setColor(0xFF3333);
                        }
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
            
            if (isRinging) {
                if (callManager != null) callManager.rejectCall(callId);
            } else {
                if (callManager != null) callManager.endCurrentCall();
            }
            
            setCallEnded();
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Touche Espace ou Clic Trackpad : Envoie "MUTE_TOGGLE"
     */
    public void toggleMute() {
        try {
            if (callManager != null) {
                callManager.toggleMute();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void updateMicStatus(final boolean isMuted) {
        try {
            this.micMuted = isMuted;
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (microBadge != null && !isEnded) {
                            microBadge.setText(buildMicroText());
                            microBadge.setColor(micMuted ? 0xF59E0B : 0x38BDF8);
                        }
                        rebuildButtons();
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Touche M : Envoie "SPEAKER_TOGGLE" pour basculer le haut-parleur du smartphone
     */
    public void toggleSpeaker() {
        try {
            if (callManager != null) {
                callManager.toggleSpeaker();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void updateSpeakerStatus(final boolean isSpeaker) {
        try {
            this.speakerOn = isSpeaker;
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (statusLabel != null && !isEnded) {
                            statusLabel.setText(buildStatusText());
                        }
                        rebuildButtons();
                    } catch (Throwable t) {
                        System.out.println("[BB ERROR] " + t.getMessage());
                    }
                }
            });
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    /**
     * Touches Volume physique (+ / - sur le côté droit) : Envoie VOLUME_UP / VOLUME_DOWN
     */
    public void sendVolumeUp() {
        try {
            if (callManager != null) {
                callManager.volumeUp();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void sendVolumeDown() {
        try {
            if (callManager != null) {
                callManager.volumeDown();
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
    
    public void updateVolumeDisplay() {
        try {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (hintLabel != null && !isEnded) {
                            hintLabel.setText("[Volume ajusté sur Smartphone]");
                            hintLabel.setColor(0x00E5FF);
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
    
    public boolean onClose() {
        try {
            stopTimer();
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.onClose();
    }
    
    // =========================================================================
    // Gestion des touches physiques Curve 9300 & Trackpad optique
    // =========================================================================
    
    protected boolean keyDown(int keycode, int time) {
        try {
            int key = Keypad.key(keycode);
            
            // 1. Touche Fin d'appel (Rouge / KEY_END ou ESCAPE) : Envoie CALL_END, affiche "Fin d'appel..." et ferme l'écran
            if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
                handlePhysicalHangup();
                return true;
            }
            
            // 2. Touche Verte (KEY_SEND) : Décroche si appel entrant
            if (key == Keypad.KEY_SEND) { 
                if (isRinging) {
                    answer();
                    return true;
                }
                return true;
            }
            
            // 3. Touches Volume physique (+ / - sur le côté droit) : Envoie VOLUME_UP ou VOLUME_DOWN
            if (key == Keypad.KEY_VOLUME_UP) {
                sendVolumeUp();
                return true;
            }
            if (key == Keypad.KEY_VOLUME_DOWN) {
                sendVolumeDown();
                return true;
            }
            
            // 4. Touche Espace : Envoie MUTE_TOGGLE
            if (key == Keypad.KEY_SPACE) {
                toggleMute();
                return true;
            }
            
            // 5. Touche Haut-parleur physique (si présente)
            if (key == Keypad.KEY_SPEAKERPHONE) {
                toggleSpeaker();
                return true;
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.keyDown(keycode, time);
    }
    
    protected boolean keyChar(char ch, int status, int time) {
        try {
            // Touche M : Envoie SPEAKER_TOGGLE pour basculer le haut-parleur
            if (ch == 'm' || ch == 'M') {
                toggleSpeaker();
                return true;
            }
            // Touche Espace : Envoie MUTE_TOGGLE
            if (ch == ' ') {
                toggleMute();
                return true;
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.keyChar(ch, status, time);
    }
    
    /**
     * Clic Trackpad optique : Envoie MUTE_TOGGLE (ou actionne le bouton sélectionné)
     */
    protected boolean navigationClick(int status, int time) {
        try {
            Field focus = getLeafFieldWithFocus();
            if (focus instanceof DarkButtonField) {
                return super.navigationClick(status, time);
            }
            toggleMute();
            return true;
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.navigationClick(status, time);
    }
    
    /**
     * Déplacement sur le Trackpad optique (Haut/Bas) : Envoie VOLUME_UP / VOLUME_DOWN
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        try {
            Field focus = getLeafFieldWithFocus();
            if (focus instanceof DarkButtonField) {
                return super.navigationMovement(dx, dy, status, time);
            }
            if (dy < 0) {
                sendVolumeUp();
                return true;
            } else if (dy > 0) {
                sendVolumeDown();
                return true;
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.navigationMovement(dx, dy, status, time);
    }
    
    protected boolean trackwheelRoll(int amount, int status, int time) {
        try {
            if (amount > 0) {
                sendVolumeUp();
                return true;
            } else if (amount < 0) {
                sendVolumeDown();
                return true;
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
        return super.trackwheelRoll(amount, status, time);
    }
    
    protected void makeMenu(Menu menu, int instance) {
        try {
            super.makeMenu(menu, instance);
            if (isRinging) {
                menu.add(new MenuItem("Décrocher (Touche Verte)", 100, 10) {
                    public void run() { answer(); }
                });
                menu.add(new MenuItem("Refuser (Touche Rouge)", 100, 20) {
                    public void run() { handlePhysicalHangup(); }
                });
            } else {
                menu.add(new MenuItem("Raccrocher (Touche Rouge)", 100, 10) {
                    public void run() { handlePhysicalHangup(); }
                });
                menu.add(new MenuItem("Couper / Réactiver Micro (Espace)", 100, 20) {
                    public void run() { toggleMute(); }
                });
                menu.add(new MenuItem("Haut-Parleur Smartphone (Touche M)", 100, 30) {
                    public void run() { toggleSpeaker(); }
                });
                menu.add(new MenuItem("Volume + (Côté droit)", 100, 40) {
                    public void run() { sendVolumeUp(); }
                });
                menu.add(new MenuItem("Volume - (Côté droit)", 100, 41) {
                    public void run() { sendVolumeDown(); }
                });
            }
        } catch (Throwable t) {
            System.out.println("[BB ERROR] " + t.getMessage());
        }
    }
}
