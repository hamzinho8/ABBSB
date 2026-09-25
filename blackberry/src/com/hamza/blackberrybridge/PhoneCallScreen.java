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
 * Volet 3 : Interface d'appel BlackBerry polie pour BlackBerry Curve 9300.
 * 
 * Fonctionnalités complètes :
 * 1. Touches physiques Curve 9300 :
 *    - Touche Verte (Send/Call) : Décrocher immédiatement lors d'un appel entrant (envoie CALL_ANSWER).
 *      Si l'appel est déjà actif : bascule rapide de la sortie audio locale (Écouteur / HP).
 *    - Touche Rouge (End/Hangup) : Raccrocher immédiatement l'appel (envoie CALL_END)
 *      et ferme l'écran pour revenir au menu principal.
 *    - Trackpad optique (défilement haut/bas) et boutons de volume physiques :
 *      Régler le volume sonore de l'écouteur/haut-parleur en temps réel (0-100%).
 * 2. Affichage temps réel :
 *    - Titre dynamique avec nom de la SIM ("Appel en cours - SIM [inwi]")
 *    - Nom du contact et numéro en grand format
 *    - Statut d'appel dynamique ("Numérotation...", "Sonnerie...", "En communication (00:00)")
 *    - Badges dynamiques de statut audio et volume.
 * 3. Arrêt audio & bips :
 *    - À la fin d'appel (CALL_END) : coupure immédiate du micro/écouteur et bip court.
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
    private boolean isLocalSpeaker = false;
    
    // Chronomètre d'appel (seconde par seconde)
    private Timer callTimer;
    private int durationSeconds = 0;
    
    // Composants d'affichage
    private DarkLabelField headerLabel;
    private DarkLabelField nameLabel;
    private DarkLabelField numberLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField streamingBadge;
    private DarkLabelField audioOutputBadge;
    private DarkLabelField volumeBadge;
    
    // Boutons tactiles / trackpad
    private StyledButtonField btnHangup;
    private StyledButtonField btnAnswer;
    private StyledButtonField btnToggleLocalAudio;
    private StyledButtonField btnPhoneSpeaker;
    private HorizontalFieldManager buttonsManager;

    public PhoneCallScreen(CallManager cm, String id, String name, String number, String simName, boolean isOutbound) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.callManager = cm;
        this.audioRecorder = cm.getApp().getCallAudioPlayerRecorder();
        this.callId = id;
        this.name = (name != null && name.trim().length() > 0) ? name.trim() : "Inconnu";
        this.number = (number != null) ? number.trim() : "";
        this.simName = (simName != null && simName.trim().length() > 0) ? simName.trim() : "Défaut";
        this.isOutbound = isOutbound;
        this.isRinging = !isOutbound;
        this.isActive = isOutbound; // Pour un appel sortant, la numérotation commence immédiatement
        this.isEnded = false;
        this.isLocalSpeaker = (audioRecorder != null && audioRecorder.isSpeakerOn());
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager content = new VerticalFieldManager(Field.FIELD_HCENTER);
        content.setPadding(6, 6, 6, 6);
        
        // 1. Titre & SIM
        String titleStr = isRinging ? ("Appel entrant - SIM [" + this.simName + "]") : ("Appel en cours - SIM [" + this.simName + "]");
        setTitle(new LabelField(titleStr, Field.FIELD_HCENTER));
        
        headerLabel = new DarkLabelField(titleStr, Field.FIELD_HCENTER, isRinging ? 0x00FF00 : 0xFFD700);
        try { headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Exception ignored) {}
        content.add(headerLabel);
        
        content.add(new SeparatorField());
        
        // 2. Nom de l'interlocuteur
        nameLabel = new DarkLabelField(this.name, Field.FIELD_HCENTER, Color.WHITE);
        try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 20)); } catch (Exception ignored) {}
        content.add(nameLabel);
        
        // 3. Numéro de téléphone
        numberLabel = new DarkLabelField(this.number, Field.FIELD_HCENTER, 0xAAAAAA);
        try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch (Exception ignored) {}
        content.add(numberLabel);
        
        // 4. Statut & Chronomètre
        String initialStatus;
        if (isRinging) {
            initialStatus = "Sonnerie en cours...";
        } else if (isOutbound) {
            initialStatus = "Numérotation...";
        } else {
            initialStatus = "En communication (00:00)";
        }
        statusLabel = new DarkLabelField(initialStatus, Field.FIELD_HCENTER, isRinging ? 0x00FFCC : (isOutbound ? 0x00E5FF : 0x00FF00));
        try { statusLabel.setFont(Font.getDefault().derive(Font.BOLD, 13)); } catch (Exception ignored) {}
        content.add(statusLabel);
        
        // 5. Badges Audio & Streaming
        streamingBadge = new DarkLabelField("[Streaming Audio : " + (isActive ? "ACTIF (PCM 8kHz)" : "EN ATTENTE") + "]", Field.FIELD_HCENTER, isActive ? 0x00E5FF : 0x888888);
        try { streamingBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Exception ignored) {}
        content.add(streamingBadge);
        
        audioOutputBadge = new DarkLabelField("[Sortie BB : " + (isLocalSpeaker ? "Haut-Parleur" : "Écouteur") + "]", Field.FIELD_HCENTER, isLocalSpeaker ? 0x00FF00 : 0xAAAAAA);
        try { audioOutputBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Exception ignored) {}
        content.add(audioOutputBadge);
        
        int currentVol = audioRecorder != null ? audioRecorder.getVolume() : 85;
        volumeBadge = new DarkLabelField("[Volume BB : " + currentVol + "%] (Trackpad haut/bas)", Field.FIELD_HCENTER, 0x888888);
        try { volumeBadge.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Exception ignored) {}
        content.add(volumeBadge);
        
        // Spacer
        VerticalFieldManager sp = new VerticalFieldManager();
        sp.setPadding(6, 0, 0, 0);
        content.add(sp);
        
        // 6. Boutons d'action
        buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
        rebuildButtons();
        content.add(buttonsManager);
        
        // Indication des touches physiques Curve
        DarkLabelField hintLabel = new DarkLabelField(isRinging ? "(Touche Verte: Décrocher | Touche Rouge: Refuser)" : "(Touche Rouge: Raccrocher | Trackpad haut/bas: Volume)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Exception ignored) {}
        VerticalFieldManager hintSp = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSp.setPadding(4, 0, 0, 0);
        hintSp.add(hintLabel);
        content.add(hintSp);
        
        add(content);
        
        if (isActive && !isOutbound) {
            startTimer();
        }
    }
    
    private void rebuildButtons() {
        buttonsManager.deleteAll();
        if (isEnded) return;
        
        if (isRinging) {
            // Mode Appel Entrant : Décrocher (Vert) et Refuser (Rouge)
            btnAnswer = new StyledButtonField("Décrocher", 0x007700, 0x00CC00, 110, 34);
            btnAnswer.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    answer();
                }
            });
            buttonsManager.add(btnAnswer);
            
            HorizontalFieldManager sp = new HorizontalFieldManager();
            sp.setPadding(0, 4, 0, 4);
            buttonsManager.add(sp);
            
            btnHangup = new StyledButtonField("Refuser", 0x990000, 0xEE2222, 110, 34);
            btnHangup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    hangup();
                }
            });
            buttonsManager.add(btnHangup);
        } else {
            // Mode En Communication / Sortant : Raccrocher (Rouge), Bascule HP BB, HP Tel
            btnHangup = new StyledButtonField("Raccrocher", 0x990000, 0xEE2222, 95, 32);
            btnHangup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    hangup();
                }
            });
            buttonsManager.add(btnHangup);
            
            HorizontalFieldManager sp1 = new HorizontalFieldManager();
            sp1.setPadding(0, 2, 0, 2);
            buttonsManager.add(sp1);
            
            String localText = isLocalSpeaker ? "HP BB: ON" : "Écouteur BB";
            btnToggleLocalAudio = new StyledButtonField(localText, isLocalSpeaker ? 0x005577 : 0x223344, 0x0088CC, 95, 32);
            btnToggleLocalAudio.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    toggleLocalAudio();
                }
            });
            buttonsManager.add(btnToggleLocalAudio);
            
            HorizontalFieldManager sp2 = new HorizontalFieldManager();
            sp2.setPadding(0, 2, 0, 2);
            buttonsManager.add(sp2);
            
            btnPhoneSpeaker = new StyledButtonField("HP Tel", 0x222222, 0x444444, 80, 32);
            btnPhoneSpeaker.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    callManager.toggleSpeaker();
                }
            });
            buttonsManager.add(btnPhoneSpeaker);
        }
    }
    
    private void startTimer() {
        stopTimer();
        durationSeconds = 0;
        callTimer = new Timer();
        callTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (isActive && !isEnded) {
                    durationSeconds++;
                    UiApplication.getUiApplication().invokeLater(new Runnable() {
                        public void run() {
                            updateDurationDisplay();
                        }
                    });
                }
            }
        }, 1000, 1000);
    }
    
    private void stopTimer() {
        if (callTimer != null) {
            try { callTimer.cancel(); } catch (Exception ignored) {}
            callTimer = null;
        }
    }
    
    private void updateDurationDisplay() {
        if (statusLabel != null && isActive && !isEnded) {
            int m = durationSeconds / 60;
            int s = durationSeconds % 60;
            String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
            statusLabel.setText("En communication (" + timeStr + ")");
            statusLabel.setColor(0x00FF00); // Lime green
        }
    }
    
    /**
     * Décroche l'appel (Touche Verte ou bouton Décrocher)
     */
    public void answer() {
        if (isRinging) {
            isRinging = false;
            isActive = true;
            callManager.answerCall(callId);
            
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (headerLabel != null) {
                        headerLabel.setText("Appel en cours - SIM [" + simName + "]");
                        headerLabel.setColor(0xFFD700);
                    }
                    if (statusLabel != null) {
                        statusLabel.setText("En communication (00:00)");
                        statusLabel.setColor(0x00FF00);
                    }
                    if (streamingBadge != null) {
                        streamingBadge.setText("[Streaming Audio : ACTIF (PCM 8kHz)]");
                        streamingBadge.setColor(0x00E5FF);
                    }
                    rebuildButtons();
                }
            });
            startTimer();
        }
    }
    
    public void setCallActive(final String simOverride) {
        this.isActive = true;
        this.isRinging = false;
        if (simOverride != null && simOverride.trim().length() > 0) {
            this.simName = simOverride.trim();
        }
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("Appel en cours - SIM [" + simName + "]");
                    headerLabel.setColor(0xFFD700);
                }
                if (streamingBadge != null) {
                    streamingBadge.setText("[Streaming Audio : ACTIF (PCM 8kHz)]");
                    streamingBadge.setColor(0x00E5FF);
                }
                rebuildButtons();
                updateDurationDisplay();
            }
        });
        startTimer();
    }
    
    public void setCallEnded() {
        if (isEnded) return;
        this.isEnded = true;
        this.isActive = false;
        this.isRinging = false;
        stopTimer();
        
        // Émettre un bip court de fin d'appel sur le haut-parleur/écouteur
        try {
            Alert.startAudio(new short[] { 800, 100 }, 70);
        } catch (Throwable ignored) {}
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("APPEL TERMINÉ");
                    headerLabel.setColor(0xFF3333);
                }
                if (statusLabel != null) {
                    int m = durationSeconds / 60;
                    int s = durationSeconds % 60;
                    String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                    statusLabel.setText("Durée : " + timeStr + " - Terminé");
                    statusLabel.setColor(0xFF8888);
                }
                if (streamingBadge != null) {
                    streamingBadge.setText("[Streaming Audio : ARRÊTÉ]");
                    streamingBadge.setColor(0x777777);
                }
                rebuildButtons();
            }
        });
        
        // Fermer l'écran d'appel après 1.2s et revenir au menu principal
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(1200); } catch (Exception ignored) {}
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try { close(); } catch (Exception ignored) {}
                    }
                });
            }
        }).start();
    }
    
    private void toggleLocalAudio() {
        if (audioRecorder != null) {
            isLocalSpeaker = audioRecorder.toggleAudioPath();
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (audioOutputBadge != null) {
                        audioOutputBadge.setText("[Sortie BB : " + (isLocalSpeaker ? "Haut-Parleur" : "Écouteur") + "]");
                        audioOutputBadge.setColor(isLocalSpeaker ? 0x00FF00 : 0xAAAAAA);
                    }
                    rebuildButtons();
                }
            });
        }
    }
    
    private void changeVolume(int delta) {
        if (audioRecorder != null) {
            audioRecorder.adjustVolume(delta);
            final int vol = audioRecorder.getVolume();
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (volumeBadge != null) {
                        volumeBadge.setText("[Volume BB : " + vol + "%] (Trackpad haut/bas)");
                    }
                }
            });
        }
    }
    
    /**
     * Raccrocher l'appel (Touche Rouge ou bouton Raccrocher)
     */
    private void hangup() {
        if (isRinging) {
            callManager.rejectCall(callId);
        } else {
            callManager.endCurrentCall();
        }
        setCallEnded();
    }
    
    public boolean onClose() {
        stopTimer();
        return super.onClose();
    }
    
    // =========================================================================
    // Gestion des touches physiques Curve 9300 & Molette / Trackpad optique
    // =========================================================================
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) { 
            // Touche Verte (Send/Call)
            if (isRinging) {
                answer(); // Décrocher si appel entrant
            } else {
                toggleLocalAudio(); // Bascule écouteur/HP si appel actif
            }
            return true;
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
            // Touche Rouge (End/Hangup) / Échap : Raccrocher et fermer l'écran
            hangup();
            return true;
        } else if (key == Keypad.KEY_VOLUME_UP) {
            changeVolume(10);
            return true;
        } else if (key == Keypad.KEY_VOLUME_DOWN) {
            changeVolume(-10);
            return true;
        } else if (key == Keypad.KEY_SPACE) {
            callManager.toggleSpeaker();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    /**
     * Déplacement sur le Trackpad optique pour ajuster le volume audio
     */
    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        if (dy < 0) { // Glissement vers le haut -> Volume +
            changeVolume(5);
            return true;
        } else if (dy > 0) { // Glissement vers le bas -> Volume -
            changeVolume(-5);
            return true;
        }
        return super.navigationMovement(dx, dy, status, time);
    }
    
    /**
     * Molette physique (Trackwheel / Trackball)
     */
    protected boolean trackwheelRoll(int amount, int status, int time) {
        if (amount > 0) {
            changeVolume(5);
            return true;
        } else if (amount < 0) {
            changeVolume(-5);
            return true;
        }
        return super.trackwheelRoll(amount, status, time);
    }
    
    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);
        if (isRinging) {
            menu.add(new MenuItem("Décrocher", 100, 10) {
                public void run() { answer(); }
            });
            menu.add(new MenuItem("Refuser l'appel", 100, 20) {
                public void run() { hangup(); }
            });
        } else {
            menu.add(new MenuItem("Raccrocher", 100, 10) {
                public void run() { hangup(); }
            });
            menu.add(new MenuItem("Bascule Haut-Parleur / Écouteur BB", 100, 20) {
                public void run() { toggleLocalAudio(); }
            });
            menu.add(new MenuItem("Haut-parleur Téléphone ON/OFF", 100, 30) {
                public void run() { callManager.toggleSpeaker(); }
            });
            menu.add(new MenuItem("Volume +", 100, 40) {
                public void run() { changeVolume(10); }
            });
            menu.add(new MenuItem("Volume -", 100, 41) {
                public void run() { changeVolume(-10); }
            });
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
            boolean focused = isFocus();
            graphics.setColor(focused ? focusColor : bgColor);
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            graphics.setColor(focused ? 0xFFFFFF : 0x555555);
            graphics.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);
            graphics.setColor(focused ? Color.BLACK : Color.WHITE);
            Font f = graphics.getFont();
            try { f = Font.getDefault().derive(Font.BOLD, 12); graphics.setFont(f); } catch (Exception ignored) {}
            int tx = (getWidth() - f.getAdvance(label)) / 2;
            int ty = (getHeight() - f.getHeight()) / 2;
            graphics.drawText(label, tx, ty);
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
