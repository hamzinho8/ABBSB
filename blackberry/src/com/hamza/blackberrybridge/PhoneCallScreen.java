package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Volet 3 : Interface d'appel BlackBerry polie pour Curve 9300.
 * Écran d'appel plein écran personnalisé avec :
 * - Titre dynamique : "Appel en cours - SIM [inwi / Orange]"
 * - Affichage élégant du Nom du contact et du Numéro
 * - Bouton Raccrocher (Touche Fin d'appel ou clic trackpad) envoyant : CALL_END
 * - Bouton Haut-Parleur BlackBerry / Écouteur pour basculer le son local
 * - Gestion des touches physiques : Touche Verte (Menu/Rappeler), Touche Rouge (Raccrocher),
 *   et Molette/Trackpad optique pour régler le volume audio en temps réel.
 */
public class PhoneCallScreen extends MainScreen {
    private CallManager callManager;
    private CallAudioPlayerRecorder audioRecorder;
    private String callId;
    private String name;
    private String number;
    private String simName;
    private boolean isOutbound;
    private boolean isActive = false;
    private boolean isEnded = false;
    private boolean isLocalSpeaker = false;
    
    // Chronomètre d'appel
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
    
    // Boutons d'action
    private StyledButtonField btnHangup;
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
        this.isActive = !isOutbound;
        this.isEnded = false;
        this.isLocalSpeaker = (audioRecorder != null && audioRecorder.isSpeakerOn());
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager content = new VerticalFieldManager(Field.FIELD_HCENTER);
        content.setPadding(6, 6, 6, 6);
        
        // 1. Titre & SIM
        String titleStr = "Appel en cours - SIM [" + this.simName + "]";
        setTitle(new LabelField(titleStr, Field.FIELD_HCENTER));
        
        headerLabel = new DarkLabelField(titleStr, Field.FIELD_HCENTER, 0xFFD700); // Gold
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
        String initialStatus = isOutbound ? "Numérotation..." : "En communication (00:00)";
        statusLabel = new DarkLabelField(initialStatus, Field.FIELD_HCENTER, isOutbound ? 0x00E5FF : 0x00FF00);
        try { statusLabel.setFont(Font.getDefault().derive(Font.BOLD, 13)); } catch (Exception ignored) {}
        content.add(statusLabel);
        
        // 5. Badges Audio & Streaming
        streamingBadge = new DarkLabelField("[Streaming Audio : ACTIF (PCM 8kHz)]", Field.FIELD_HCENTER, 0x00E5FF);
        try { streamingBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Exception ignored) {}
        content.add(streamingBadge);
        
        audioOutputBadge = new DarkLabelField("[Sortie BB : " + (isLocalSpeaker ? "Haut-Parleur" : "Écouteur") + "]", Field.FIELD_HCENTER, isLocalSpeaker ? 0x00FF00 : 0xAAAAAA);
        try { audioOutputBadge.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch (Exception ignored) {}
        content.add(audioOutputBadge);
        
        int currentVol = audioRecorder != null ? audioRecorder.getVolume() : 85;
        volumeBadge = new DarkLabelField("[Volume BB : " + currentVol + "%] (Molette / Trackpad)", Field.FIELD_HCENTER, 0x888888);
        try { volumeBadge.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch (Exception ignored) {}
        content.add(volumeBadge);
        
        // Spacer
        VerticalFieldManager sp = new VerticalFieldManager();
        sp.setPadding(6, 0, 0, 0);
        content.add(sp);
        
        // 6. Boutons tactiles / trackpad
        buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
        rebuildButtons();
        content.add(buttonsManager);
        
        // Raccourcis touches physiques Curve
        DarkLabelField hintLabel = new DarkLabelField("(Touche Rouge: Raccrocher | Trackpad haut/bas: Volume)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Exception ignored) {}
        VerticalFieldManager hintSp = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSp.setPadding(4, 0, 0, 0);
        hintSp.add(hintLabel);
        content.add(hintSp);
        
        add(content);
        
        startTimer();
    }
    
    private void rebuildButtons() {
        buttonsManager.deleteAll();
        if (isEnded) return;
        
        // Bouton Raccrocher (Rouge)
        btnHangup = new StyledButtonField("Raccrocher", 0x990000, 0xEE2222, 100, 32);
        btnHangup.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                hangup();
            }
        });
        
        // Bouton Bascule Écouteur / Haut-Parleur BB
        String localText = isLocalSpeaker ? "HP BB: ON" : "Écouteur BB";
        btnToggleLocalAudio = new StyledButtonField(localText, isLocalSpeaker ? 0x005577 : 0x223344, 0x0088CC, 100, 32);
        btnToggleLocalAudio.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                toggleLocalAudio();
            }
        });
        
        // Bouton HP Téléphone Android
        btnPhoneSpeaker = new StyledButtonField("HP Tel", 0x222222, 0x444444, 85, 32);
        btnPhoneSpeaker.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                callManager.toggleSpeaker();
            }
        });
        
        buttonsManager.add(btnHangup);
        HorizontalFieldManager sp1 = new HorizontalFieldManager();
        sp1.setPadding(0, 2, 0, 2);
        buttonsManager.add(sp1);
        buttonsManager.add(btnToggleLocalAudio);
        HorizontalFieldManager sp2 = new HorizontalFieldManager();
        sp2.setPadding(0, 2, 0, 2);
        buttonsManager.add(sp2);
        buttonsManager.add(btnPhoneSpeaker);
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
    
    public void setCallActive(final String simOverride) {
        this.isActive = true;
        if (simOverride != null && simOverride.trim().length() > 0) {
            this.simName = simOverride.trim();
        }
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("Appel en cours - SIM [" + simName + "]");
                }
                updateDurationDisplay();
            }
        });
    }
    
    public void setCallEnded() {
        if (isEnded) return;
        this.isEnded = true;
        this.isActive = false;
        stopTimer();
        
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
        
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(1400); } catch (Exception ignored) {}
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
                        volumeBadge.setText("[Volume BB : " + vol + "%] (Molette / Trackpad)");
                    }
                }
            });
        }
    }
    
    private void hangup() {
        callManager.endCurrentCall();
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
        if (key == Keypad.KEY_SEND) { // Touche Verte : Menu / Rappeler
            toggleLocalAudio();
            return true;
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { // Touche Rouge : Raccrocher
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
        if (dy < 0) { // Glissement vers le haut
            changeVolume(5);
            return true;
        } else if (dy > 0) { // Glissement vers le bas
            changeVolume(-5);
            return true;
        }
        return super.navigationMovement(dx, dy, status, time);
    }
    
    /**
     * Molette BlackBerry physique (Trackwheel / Trackball)
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
