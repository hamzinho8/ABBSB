package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.system.Alert;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * SmartWatch In-Call Screen (PhoneCallScreen) pour BlackBerry Curve 9300 (OS 5.0 / 6.0).
 * 
 * Interface moderne, fluide et ergonomique optimisée pour l'écran 320x240 :
 * 1. En-tête : "Appel en cours - SIM: [inwi / Orange]" ou "Appel entrant - SIM: [inwi / Orange]"
 * 2. Centre : Nom du contact en grand et en gras, Numéro de téléphone en dessous.
 * 3. Chronomètre en direct : "00:00", incrémenté chaque seconde dès réception de CALL_ACTIVE.
 * 4. Statut matériel en direct : "Haut-parleur : ON/OFF | Micro : Actif/Muet"
 * 
 * Contrôle matériel complet & touches physiques :
 * - Touche Rouge (KEY_END / KEY_ESCAPE) : Envoie CALL_END, bip court et retour écran principal.
 * - Touche Verte (KEY_SEND) : Si appel entrant, décroche immédiatement (CALL_ANSWER).
 * - Touches de volume physiques (côté droit Curve 9300) :
 *   * Volume + (KEY_VOLUME_UP)   -> envoie "VOLUME_UP"
 *   * Volume - (KEY_VOLUME_DOWN) -> envoie "VOLUME_DOWN"
 * - Touche Espace (KEY_SPACE) ou Clic Trackpad sur Mute :
 *   * Envoie "MUTE_TOGGLE" et bascule l'état du micro (Actif / Muet).
 * - Trackpad optique haut/bas : Volume +/-
 * - Menu contextuel BlackBerry : Raccrocher, Mute, Haut-Parleur, Volume.
 * 
 * 100% Asynchrone (UiApplication.getUiApplication().invokeLater) & Anti-Exception (try/catch).
 */
public class PhoneCallScreen extends MainScreen {
    private CallManager callManager;
    private CallAudioPlayerRecorder audioRecorder;
    private String callId;
    private String name;
    private String number;
    private String simName;
    private boolean isOutbound;
    private boolean isRinging;
    private boolean isActive;
    private boolean isEnded = false;
    
    // États matériels du SmartWatch Call Screen
    private boolean speakerOn = false;
    private boolean micMuted = false;
    
    // Chronomètre d'appel (seconde par seconde)
    private Timer callTimer;
    private int durationSeconds = 0;
    
    // Composants visuels UI
    private DarkLabelField headerLabel;
    private DarkLabelField nameLabel;
    private DarkLabelField numberLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField timerLabel;
    private DarkLabelField hardwareStatusBadge;
    private DarkLabelField volumeBadge;
    
    // Boutons d'action tactiles / trackpad
    private StyledButtonField btnHangup;
    private StyledButtonField btnAnswer;
    private StyledButtonField btnMute;
    private StyledButtonField btnSpeaker;
    private HorizontalFieldManager buttonsManager;

    public PhoneCallScreen(CallManager cm, String id, String name, String number, String simName, boolean isOutbound) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.callManager = cm;
        this.audioRecorder = cm.getApp().getCallAudioPlayerRecorder();
        this.callId = (id != null && id.length() > 0) ? id : "call_" + System.currentTimeMillis();
        this.name = (name != null && name.trim().length() > 0) ? name.trim() : "Inconnu";
        this.number = (number != null && number.trim().length() > 0) ? number.trim() : "";
        this.simName = (simName != null && simName.trim().length() > 0) ? simName.trim() : "SIM 1";
        this.isOutbound = isOutbound;
        this.isRinging = !isOutbound;
        this.isActive = isOutbound; // Pour un appel sortant, la numérotation commence immédiatement
        this.isEnded = false;
        
        this.speakerOn = cm.isSpeakerOn();
        this.micMuted = cm.isMicMuted();
        
        try {
            getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
            
            VerticalFieldManager content = new VerticalFieldManager(Field.FIELD_HCENTER);
            content.setPadding(4, 6, 4, 6);
            
            // 1. En-tête : "Appel en cours - SIM: [nom]" ou "Appel entrant - SIM: [nom]"
            String titleText = isRinging ? ("Appel entrant - SIM: [" + this.simName + "]") : ("Appel en cours - SIM: [" + this.simName + "]");
            setTitle(new LabelField(titleText, Field.FIELD_HCENTER));
            
            headerLabel = new DarkLabelField(titleText, Field.FIELD_HCENTER, isRinging ? 0x00FFCC : 0xFFD700);
            try { headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 13)); } catch (Throwable ignored) {}
            content.add(headerLabel);
            
            content.add(new SeparatorField());
            
            // 2. Centre : Nom du contact en grand et en gras
            nameLabel = new DarkLabelField(this.name, Field.FIELD_HCENTER, Color.WHITE);
            try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 19)); } catch (Throwable ignored) {}
            content.add(nameLabel);
            
            // Numéro de téléphone en dessous
            numberLabel = new DarkLabelField(this.number, Field.FIELD_HCENTER, 0xBBBBBB);
            try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch (Throwable ignored) {}
            content.add(numberLabel);
            
            // 3. Statut en direct et Chronomètre
            String initialStatus = isRinging ? "Sonnerie en cours..." : (isOutbound ? "Numérotation en cours..." : "En communication");
            statusLabel = new DarkLabelField(initialStatus, Field.FIELD_HCENTER, isRinging ? 0x00FFCC : (isOutbound ? 0x00E5FF : 0x00FF00));
            try { statusLabel.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch (Throwable ignored) {}
            content.add(statusLabel);
            
            timerLabel = new DarkLabelField(isActive ? "00:00" : "--:--", Field.FIELD_HCENTER, 0x00FF00);
            try { timerLabel.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Throwable ignored) {}
            content.add(timerLabel);
            
            // 4. Statut du haut-parleur et du micro : "Haut-parleur : ON | Micro : Actif"
            hardwareStatusBadge = new DarkLabelField(buildHardwareStatusText(), Field.FIELD_HCENTER, 0x00E5FF);
            try { hardwareStatusBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Throwable ignored) {}
            content.add(hardwareStatusBadge);
            
            volumeBadge = new DarkLabelField("[Vol Android / BB] (Touches latérales +/-)", Field.FIELD_HCENTER, 0x777777);
            try { volumeBadge.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Throwable ignored) {}
            content.add(volumeBadge);
            
            // Petit espaceur
            VerticalFieldManager sp = new VerticalFieldManager();
            sp.setPadding(4, 0, 0, 0);
            content.add(sp);
            
            // 5. Boutons tactiles et navigables au trackpad
            buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
            rebuildButtons();
            content.add(buttonsManager);
            
            // Indication des touches physiques Curve 9300
            String hintText = isRinging ? "(Touche Verte: Décrocher | Touche Rouge: Refuser)" 
                                        : "(Touche Rouge: Raccrocher | Espace: Mute | Vol +/-)";
            DarkLabelField hintLabel = new DarkLabelField(hintText, Field.FIELD_HCENTER, 0x555555);
            try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Throwable ignored) {}
            VerticalFieldManager hintSp = new VerticalFieldManager(Field.FIELD_HCENTER);
            hintSp.setPadding(3, 0, 0, 0);
            hintSp.add(hintLabel);
            content.add(hintSp);
            
            add(content);
            
            if (isActive && !isOutbound) {
                startTimer();
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error initializing PhoneCallScreen: " + t.getMessage());
        }
    }
    
    private String buildHardwareStatusText() {
        return "Haut-parleur : " + (speakerOn ? "ON" : "OFF") + " | Micro : " + (micMuted ? "Muet" : "Actif");
    }
    
    private void rebuildButtons() {
        try {
            buttonsManager.deleteAll();
            if (isEnded) return;
            
            if (isRinging) {
                // Mode Appel Entrant : Décrocher (Vert) et Refuser (Rouge)
                btnAnswer = new StyledButtonField("Décrocher", 0x007700, 0x00CC00, 110, 32);
                btnAnswer.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        answer();
                    }
                });
                buttonsManager.add(btnAnswer);
                
                HorizontalFieldManager sp = new HorizontalFieldManager();
                sp.setPadding(0, 4, 0, 4);
                buttonsManager.add(sp);
                
                btnHangup = new StyledButtonField("Refuser", 0x990000, 0xEE2222, 110, 32);
                btnHangup.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        hangup();
                    }
                });
                buttonsManager.add(btnHangup);
            } else {
                // Mode En Communication / Sortant : Raccrocher (Rouge), Mute (Bleu/Gris), Haut-Parleur (Gris/Vert)
                btnHangup = new StyledButtonField("Raccrocher", 0x990000, 0xEE2222, 95, 30);
                btnHangup.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        hangup();
                    }
                });
                buttonsManager.add(btnHangup);
                
                HorizontalFieldManager sp1 = new HorizontalFieldManager();
                sp1.setPadding(0, 2, 0, 2);
                buttonsManager.add(sp1);
                
                String muteText = micMuted ? "Micro: OFF" : "Micro: ON";
                btnMute = new StyledButtonField(muteText, micMuted ? 0x773300 : 0x223344, 0xCC5500, 95, 30);
                btnMute.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        toggleMute();
                    }
                });
                buttonsManager.add(btnMute);
                
                HorizontalFieldManager sp2 = new HorizontalFieldManager();
                sp2.setPadding(0, 2, 0, 2);
                buttonsManager.add(sp2);
                
                String spkText = speakerOn ? "HP: ON" : "HP: OFF";
                btnSpeaker = new StyledButtonField(spkText, speakerOn ? 0x005500 : 0x222222, 0x00AA00, 85, 30);
                btnSpeaker.setChangeListener(new FieldChangeListener() {
                    public void fieldChanged(Field field, int context) {
                        toggleSpeaker();
                    }
                });
                buttonsManager.add(btnSpeaker);
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in rebuildButtons: " + t.getMessage());
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
                    } catch (Throwable ignored) {}
                }
            }, 1000, 1000);
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error starting timer: " + t.getMessage());
        }
    }
    
    private void stopTimer() {
        try {
            if (callTimer != null) {
                callTimer.cancel();
                callTimer = null;
            }
        } catch (Throwable ignored) {}
    }
    
    private void updateDurationDisplay() {
        try {
            if (timerLabel != null && isActive && !isEnded) {
                int m = durationSeconds / 60;
                int s = durationSeconds % 60;
                String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                timerLabel.setText(timeStr);
                if (statusLabel != null) {
                    statusLabel.setText("En communication (" + timeStr + ")");
                    statusLabel.setColor(0x00FF00); // Lime green
                }
            }
        } catch (Throwable ignored) {}
    }
    
    /**
     * Décroche l'appel (Touche Verte ou bouton Décrocher)
     */
    public void answer() {
        try {
            if (isRinging) {
                isRinging = false;
                isActive = true;
                callManager.answerCall(callId);
                
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try {
                            if (headerLabel != null) {
                                headerLabel.setText("Appel en cours - SIM: [" + simName + "]");
                                headerLabel.setColor(0xFFD700);
                            }
                            if (statusLabel != null) {
                                statusLabel.setText("En communication");
                                statusLabel.setColor(0x00FF00);
                            }
                            if (timerLabel != null) {
                                timerLabel.setText("00:00");
                            }
                            rebuildButtons();
                        } catch (Throwable ignored) {}
                    }
                });
                startTimer();
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in answer: " + t.getMessage());
        }
    }
    
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
                            headerLabel.setText("Appel en cours - SIM: [" + simName + "]");
                            headerLabel.setColor(0xFFD700);
                        }
                        if (statusLabel != null) {
                            statusLabel.setText("En communication");
                            statusLabel.setColor(0x00FF00);
                        }
                        rebuildButtons();
                        updateDurationDisplay();
                    } catch (Throwable ignored) {}
                }
            });
            startTimer();
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in setCallActive: " + t.getMessage());
        }
    }
    
    public void setCallEnded() {
        try {
            if (isEnded) return;
            this.isEnded = true;
            this.isActive = false;
            this.isRinging = false;
            stopTimer();
            
            // Émettre un bip court de fin d'appel sur le BlackBerry Curve
            try {
                Alert.startAudio(new short[] { 800, 100 }, 75);
            } catch (Throwable ignored) {}
            
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    try {
                        if (headerLabel != null) {
                            headerLabel.setText("APPEL TERMINÉ");
                            headerLabel.setColor(0xFF3333);
                        }
                        if (statusLabel != null) {
                            int m = durationSeconds / 60;
                            int s = durationSeconds % 60;
                            String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                            statusLabel.setText("Appel terminé (" + timeStr + ")");
                            statusLabel.setColor(0xFF8888);
                        }
                        rebuildButtons();
                    } catch (Throwable ignored) {}
                }
            });
            
            // Fermeture automatique au bout de 1,5 seconde
            new Thread(new Runnable() {
                public void run() {
                    try { 
                        Thread.sleep(1500); 
                    } catch (Throwable ignored) {}
                    
                    UiApplication.getUiApplication().invokeLater(new Runnable() {
                        public void run() {
                            try { 
                                close(); 
                            } catch (Throwable ignored) {}
                        }
                    });
                }
            }).start();
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in setCallEnded: " + t.getMessage());
        }
    }
    
    /**
     * Raccrocher l'appel (Touche Rouge native / bouton Raccrocher)
     * Envoie immédiatement "CALL_END" et ferme l'écran.
     */
    private void hangup() {
        try {
            if (isRinging) {
                callManager.rejectCall(callId);
            } else {
                callManager.endCurrentCall();
            }
            setCallEnded();
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in hangup: " + t.getMessage());
        }
    }
    
    /**
     * Bascule le mode muet (MUTE_TOGGLE)
     */
    public void toggleMute() {
        try {
            callManager.toggleMute();
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error toggling mute: " + t.getMessage());
        }
    }
    
    public void updateMicStatus(final boolean isMuted) {
        this.micMuted = isMuted;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (hardwareStatusBadge != null) {
                        hardwareStatusBadge.setText(buildHardwareStatusText());
                        hardwareStatusBadge.setColor(micMuted ? 0xFF8800 : 0x00E5FF);
                    }
                    rebuildButtons();
                } catch (Throwable ignored) {}
            }
        });
    }
    
    /**
     * Bascule le haut-parleur (SPEAKER_TOGGLE)
     */
    public void toggleSpeaker() {
        try {
            callManager.toggleSpeaker();
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error toggling speaker: " + t.getMessage());
        }
    }
    
    public void updateSpeakerStatus(final boolean isSpeaker) {
        this.speakerOn = isSpeaker;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (hardwareStatusBadge != null) {
                        hardwareStatusBadge.setText(buildHardwareStatusText());
                    }
                    rebuildButtons();
                } catch (Throwable ignored) {}
            }
        });
    }
    
    public void updateVolumeDisplay() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                try {
                    if (volumeBadge != null) {
                        volumeBadge.setText("[Volume ajusté]");
                    }
                } catch (Throwable ignored) {}
            }
        });
    }
    
    public boolean onClose() {
        stopTimer();
        return super.onClose();
    }
    
    // =========================================================================
    // Gestion des touches physiques Curve 9300 & Molette / Trackpad optique
    // =========================================================================
    
    protected boolean keyDown(int keycode, int time) {
        try {
            int key = Keypad.key(keycode);
            
            // 1. Touche Rouge native (End/Hangup) ou Échap : Raccrocher immédiatement
            if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
                hangup();
                return true;
            }
            
            // 2. Touche Verte native (Send/Call) : Décrocher si appel entrant
            if (key == Keypad.KEY_SEND) { 
                if (isRinging) {
                    answer();
                    return true;
                } else {
                    toggleSpeaker();
                    return true;
                }
            }
            
            // 3. Touches de volume physiques (côté droit du Curve 9300)
            if (key == Keypad.KEY_VOLUME_UP) {
                callManager.volumeUp();
                return true;
            }
            if (key == Keypad.KEY_VOLUME_DOWN) {
                callManager.volumeDown();
                return true;
            }
            
            // 4. Touche Espace : Coupe / Réactive le micro à distance (MUTE_TOGGLE)
            if (key == Keypad.KEY_SPACE) {
                toggleMute();
                return true;
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in keyDown: " + t.getMessage());
        }
        return super.keyDown(keycode, time);
    }
    
    /**
     * Déplacement sur le Trackpad optique pour ajuster le volume audio (Volume +/-)
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        try {
            if (dy < 0) { // Glissement vers le haut -> Volume +
                callManager.volumeUp();
                return true;
            } else if (dy > 0) { // Glissement vers le bas -> Volume -
                callManager.volumeDown();
                return true;
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in navigationMovement: " + t.getMessage());
        }
        return super.navigationMovement(dx, dy, status, time);
    }
    
    /**
     * Molette physique (Trackwheel / Trackball)
     */
    protected boolean trackwheelRoll(int amount, int status, int time) {
        try {
            if (amount > 0) {
                callManager.volumeUp();
                return true;
            } else if (amount < 0) {
                callManager.volumeDown();
                return true;
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in trackwheelRoll: " + t.getMessage());
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
                    public void run() { hangup(); }
                });
            } else {
                menu.add(new MenuItem("Raccrocher", 100, 10) {
                    public void run() { hangup(); }
                });
                menu.add(new MenuItem("Couper / Réactiver Micro", 100, 20) {
                    public void run() { toggleMute(); }
                });
                menu.add(new MenuItem("Basculer Haut-Parleur", 100, 30) {
                    public void run() { toggleSpeaker(); }
                });
                menu.add(new MenuItem("Volume +", 100, 40) {
                    public void run() { callManager.volumeUp(); }
                });
                menu.add(new MenuItem("Volume -", 100, 41) {
                    public void run() { callManager.volumeDown(); }
                });
            }
        } catch (Throwable t) {
            LogManager.error("CALL_SCREEN", "Error in makeMenu: " + t.getMessage());
        }
    }
    
    // --- Composant bouton stylisé ---
    private static class StyledButtonField extends Field {
        private String label;
        private int bgColor;
        private int focusColor;
        private int width, height;

        public StyledButtonField(String label, int bgColor, int focusColor, int width, int height) {
            super(FOCUSABLE);
            this.label = label;
            this.bgColor = bgColor;
            this.focusColor = focusColor;
            this.width = width;
            this.height = height;
        }
        
        public int getPreferredWidth() { return width; }
        public int getPreferredHeight() { return height; }
        
        protected void layout(int width, int height) {
            setExtent(getPreferredWidth(), getPreferredHeight());
        }
        
        protected void paint(Graphics graphics) {
            try {
                boolean focused = isFocus();
                graphics.setColor(focused ? focusColor : bgColor);
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                graphics.setColor(focused ? 0xFFFFFF : 0x555555);
                graphics.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                graphics.setColor(focused ? Color.BLACK : Color.WHITE);
                Font f = graphics.getFont();
                try { 
                    f = Font.getDefault().derive(Font.BOLD, 12); 
                    graphics.setFont(f); 
                } catch (Throwable ignored) {}
                int tx = (getWidth() - f.getAdvance(label)) / 2;
                int ty = (getHeight() - f.getHeight()) / 2;
                graphics.drawText(label, tx, ty);
            } catch (Throwable ignored) {}
        }
        
        protected boolean navigationClick(int status, int time) {
            fieldChangeNotify(0);
            return true;
        }
        
        protected boolean trackwheelClick(int status, int time) {
            fieldChangeNotify(0);
            return true;
        }
        
        protected boolean invokeAction(int action) {
            if (action == ACTION_INVOKE) {
                fieldChangeNotify(0);
                return true;
            }
            return super.invokeAction(action);
        }
    }
}
