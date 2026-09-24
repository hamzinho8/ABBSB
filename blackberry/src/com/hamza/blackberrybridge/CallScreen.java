package com.hamza.blackberrybridge;

import java.util.Timer;
import java.util.TimerTask;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Écran d'appel plein écran pour BlackBerry Curve / Bold (OS 5.0).
 * Gère :
 * - Les appels entrants et sortants avec nom de contact, numéro et SIM utilisée.
 * - Le décrochage (Touche Verte) et raccrochage (Touche Rouge / Échap).
 * - La communication active avec chronomètre temps réel (00:00).
 * - Le basculement du haut-parleur smartphone (SPEAKER_TOGGLE).
 * - La sélection de la route audio :
 *     * Bluetooth / BlackBerry (AUDIO_ROUTE|BLUETOOTH)
 *     * Haut-parleur Smartphone (AUDIO_ROUTE|SPEAKERPHONE)
 *     * Écouteur Smartphone (AUDIO_ROUTE|EARPIECE)
 * - Le routage local BlackBerry (combiné / haut-parleur).
 * - Fermeture automatique avec bip de fin d'appel.
 */
public class CallScreen extends MainScreen {
    private CallManager callManager;
    private String callId;
    private String name;
    private String number;
    private String simName;
    private boolean isOutbound;
    private boolean isActive = false;
    private boolean isEnded = false;
    private boolean speakerOn = false;
    private String audioRoute = "BLUETOOTH";
    private boolean localSpeakerOn = false;
    
    // Chronomètre d'appel
    private Timer callTimer;
    private int callDurationSeconds = 0;
    
    // Composants visuels
    private DarkLabelField headerLabel;
    private DarkLabelField simLabel;
    private DarkLabelField nameLabel;
    private DarkLabelField numberLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField audioRouteLabel;
    private DarkLabelField speakerStatusLabel;
    private DarkLabelField localAudioLabel;
    
    private CallButtonField btnAnswer;
    private CallButtonField btnRejectOrHangup;
    private CallButtonField btnSpeaker;
    private CallButtonField btnRouteAudio;
    private HorizontalFieldManager buttonsManager;

    public CallScreen(CallManager cm, String id, String name, String number) {
        this(cm, id, name, number, "", false);
    }
    
    public CallScreen(CallManager cm, String id, String name, String number, String simName, boolean isOutbound) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.callManager = cm;
        this.callId = id;
        this.name = (name != null && name.trim().length() > 0) ? name.trim() : "Inconnu";
        this.number = (number != null) ? number.trim() : "";
        this.simName = (simName != null) ? simName.trim() : "";
        this.isOutbound = isOutbound;
        this.isActive = false;
        this.isEnded = false;
        this.audioRoute = "BLUETOOTH";
        this.localSpeakerOn = false;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER);
        vfm.setPadding(4, 6, 4, 6);
        
        // 1. En-tête : Type d'appel
        String headerText = isOutbound ? "APPEL SORTANT" : "APPEL ENTRANT";
        int headerColor = isOutbound ? 0x00E5FF : 0x00FF00;
        headerLabel = new DarkLabelField(headerText, Field.FIELD_HCENTER, headerColor);
        try { headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 15)); } catch(Exception ignored){}
        vfm.add(headerLabel);
        
        // 2. Ligne SIM : ex "Appel via SIM : inwi" ou "sur [Orange]"
        String simText = (this.simName.length() > 0) ? "Appel via [" + this.simName + "]" : "";
        simLabel = new DarkLabelField(simText, Field.FIELD_HCENTER, 0xFFD700); // Gold
        try { simLabel.setFont(Font.getDefault().derive(Font.BOLD, 12)); } catch(Exception ignored){}
        vfm.add(simLabel);
        
        vfm.add(new SeparatorField());
        
        // Spacer
        VerticalFieldManager sp1 = new VerticalFieldManager();
        sp1.setPadding(3, 0, 0, 0);
        vfm.add(sp1);
        
        // 3. Nom de l'interlocuteur
        nameLabel = new DarkLabelField(this.name, Field.FIELD_HCENTER, Color.WHITE);
        try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 19)); } catch(Exception ignored){}
        vfm.add(nameLabel);
        
        // 4. Numéro de téléphone
        numberLabel = new DarkLabelField(this.number, Field.FIELD_HCENTER, 0xAAAAAA);
        try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch(Exception ignored){}
        vfm.add(numberLabel);
        
        // 5. Statut actuel de l'appel / Chronomètre
        String initialStatus = isOutbound ? "Numérotation..." : "Sonnerie...";
        int initialStatusColor = isOutbound ? 0x00E5FF : 0xFFCC00;
        statusLabel = new DarkLabelField(initialStatus, Field.FIELD_HCENTER, initialStatusColor);
        try { statusLabel.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch(Exception ignored){}
        vfm.add(statusLabel);
        
        // 6. Badges Audio : Route & Haut-parleur
        audioRouteLabel = new DarkLabelField("[Audio: Bluetooth / BlackBerry]", Field.FIELD_HCENTER, 0x00E5FF);
        try { audioRouteLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch(Exception ignored){}
        vfm.add(audioRouteLabel);
        
        HorizontalFieldManager audioSubStatus = new HorizontalFieldManager(Field.FIELD_HCENTER);
        speakerStatusLabel = new DarkLabelField("[HP Tel: OFF] ", 0x777777);
        localAudioLabel = new DarkLabelField("[Sortie BB: Combiné]", 0xAAAAAA);
        try {
            Font miniFont = Font.getDefault().derive(Font.PLAIN, 10);
            speakerStatusLabel.setFont(miniFont);
            localAudioLabel.setFont(miniFont);
        } catch(Exception ignored){}
        audioSubStatus.add(speakerStatusLabel);
        audioSubStatus.add(localAudioLabel);
        vfm.add(audioSubStatus);
        
        // Spacer avant boutons
        VerticalFieldManager sp2 = new VerticalFieldManager();
        sp2.setPadding(6, 0, 0, 0);
        vfm.add(sp2);
        
        // 7. Boutons d'action tactiles / trackpad
        buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
        rebuildButtons();
        vfm.add(buttonsManager);
        
        // Indication des touches physiques Curve
        DarkLabelField hintLabel = new DarkLabelField("(Vert: Répondre | Rouge: Raccrocher | Espace: HP)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch(Exception ignored){}
        VerticalFieldManager hintSpacer = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSpacer.setPadding(4, 0, 0, 0);
        hintSpacer.add(hintLabel);
        vfm.add(hintSpacer);
        
        add(vfm);
    }
    
    private void rebuildButtons() {
        buttonsManager.deleteAll();
        
        if (isEnded) {
            // Aucun bouton actif si l'appel est terminé
            return;
        }
        
        if (!isOutbound && !isActive) {
            // Mode Appel Entrant : Décrocher (Vert) + Refuser (Rouge)
            btnAnswer = new CallButtonField("Décrocher", 0x007700, 0x00CC00, 125, 32);
            btnAnswer.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    answer();
                }
            });
            
            btnRejectOrHangup = new CallButtonField("Refuser", 0x880000, 0xDD2222, 125, 32);
            btnRejectOrHangup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    reject();
                }
            });
            
            buttonsManager.add(btnAnswer);
            HorizontalFieldManager spacer = new HorizontalFieldManager();
            spacer.setPadding(0, 4, 0, 4);
            buttonsManager.add(spacer);
            buttonsManager.add(btnRejectOrHangup);
        } else {
            // Mode Appel Actif ou Appel Sortant : Raccrocher (Rouge) + Haut-parleur + Route Audio
            btnRejectOrHangup = new CallButtonField("Raccrocher", 0x990000, 0xEE2222, 95, 32);
            btnRejectOrHangup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    hangup();
                }
            });
            
            btnSpeaker = new CallButtonField(speakerOn ? "HP: ON" : "HP Tel", speakerOn ? 0x005588 : 0x2b2b2b, 0x00A2E8, 85, 32);
            btnSpeaker.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    toggleSpeaker();
                }
            });
            
            btnRouteAudio = new CallButtonField("Audio...", 0x223344, 0x0088CC, 85, 32);
            btnRouteAudio.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    showAudioRouteDialog();
                }
            });
            
            buttonsManager.add(btnRejectOrHangup);
            HorizontalFieldManager spA = new HorizontalFieldManager();
            spA.setPadding(0, 2, 0, 2);
            buttonsManager.add(spA);
            buttonsManager.add(btnSpeaker);
            HorizontalFieldManager spB = new HorizontalFieldManager();
            spB.setPadding(0, 2, 0, 2);
            buttonsManager.add(spB);
            buttonsManager.add(btnRouteAudio);
        }
    }
    
    public void setSimName(final String sim) {
        this.simName = sim;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (simLabel != null && sim != null && sim.length() > 0) {
                    simLabel.setText("Appel via [" + sim + "]");
                }
            }
        });
    }
    
    public void setStatus(final String status) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (statusLabel != null && !isActive && !isEnded) {
                    statusLabel.setText(status);
                }
            }
        });
    }
    
    public void setCallActive() {
        setCallActive(null);
    }
    
    public void setCallActive(final String simNameOverride) {
        this.isActive = true;
        if (simNameOverride != null && simNameOverride.trim().length() > 0) {
            this.simName = simNameOverride.trim();
        }
        
        startDurationTimer();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("COMMUNICATION ACTIVE");
                    headerLabel.setColor(0x00FF00); // Lime green
                }
                if (simLabel != null && simName.length() > 0) {
                    simLabel.setText("Appel via [" + simName + "]");
                }
                updateDurationLabel();
                rebuildButtons();
            }
        });
    }
    
    private void startDurationTimer() {
        stopDurationTimer();
        callDurationSeconds = 0;
        callTimer = new Timer();
        callTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                callDurationSeconds++;
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        updateDurationLabel();
                    }
                });
            }
        }, 1000, 1000);
    }
    
    private void stopDurationTimer() {
        if (callTimer != null) {
            try { callTimer.cancel(); } catch (Exception ignored) {}
            callTimer = null;
        }
    }
    
    private void updateDurationLabel() {
        if (statusLabel != null && isActive && !isEnded) {
            int m = callDurationSeconds / 60;
            int s = callDurationSeconds % 60;
            String timeStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
            statusLabel.setText("En communication (" + timeStr + ")");
            statusLabel.setColor(0x00FF00);
        }
    }
    
    /**
     * Marque l'appel comme terminé, met à jour l'en-tête et programme la fermeture automatique.
     */
    public void setCallEnded() {
        if (isEnded) return;
        this.isEnded = true;
        this.isActive = false;
        stopDurationTimer();
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("APPEL TERMINÉ");
                    headerLabel.setColor(0xFF3333); // Rouge
                }
                if (statusLabel != null) {
                    int m = callDurationSeconds / 60;
                    int s = callDurationSeconds % 60;
                    String durationStr = (m < 10 ? "0" : "") + m + ":" + (s < 10 ? "0" : "") + s;
                    statusLabel.setText("Durée : " + durationStr + " - Terminé");
                    statusLabel.setColor(0xFF8888);
                }
                rebuildButtons();
            }
        });
        
        // Fermeture automatique après 1.4 seconde
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1400);
                } catch (InterruptedException ignored) {}
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        try { close(); } catch (Exception ignored) {}
                    }
                });
            }
        }).start();
    }
    
    public void updateSpeakerStatus(final boolean on) {
        this.speakerOn = on;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (speakerStatusLabel != null) {
                    if (on) {
                        speakerStatusLabel.setText("[HP Tel: ON] ");
                        speakerStatusLabel.setColor(0x00FF00); // Vert
                    } else {
                        speakerStatusLabel.setText("[HP Tel: OFF] ");
                        speakerStatusLabel.setColor(0x777777); // Gris
                    }
                }
                rebuildButtons();
            }
        });
    }
    
    public void updateAudioRoute(final String route) {
        this.audioRoute = (route != null) ? route.toUpperCase() : "BLUETOOTH";
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (audioRouteLabel != null) {
                    if ("BLUETOOTH".equals(audioRoute)) {
                        audioRouteLabel.setText("[Audio: Bluetooth / BlackBerry]");
                        audioRouteLabel.setColor(0x00E5FF); // Cyan
                    } else if ("SPEAKERPHONE".equals(audioRoute)) {
                        audioRouteLabel.setText("[Audio: Haut-parleur Smartphone]");
                        audioRouteLabel.setColor(0xFFA500); // Amber
                    } else if ("EARPIECE".equals(audioRoute)) {
                        audioRouteLabel.setText("[Audio: Écouteur Smartphone]");
                        audioRouteLabel.setColor(0xCCCCCC); // Blanc cassé
                    } else {
                        audioRouteLabel.setText("[Audio: " + audioRoute + "]");
                        audioRouteLabel.setColor(0x00E5FF);
                    }
                }
            }
        });
    }
    
    public void updateLocalAudioBadge(final boolean isSpeaker) {
        this.localSpeakerOn = isSpeaker;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (localAudioLabel != null) {
                    if (isSpeaker) {
                        localAudioLabel.setText("[Sortie BB: Haut-Parleur]");
                        localAudioLabel.setColor(0x00FF00);
                    } else {
                        localAudioLabel.setText("[Sortie BB: Combiné]");
                        localAudioLabel.setColor(0xAAAAAA);
                    }
                }
            }
        });
    }
    
    private void showAudioRouteDialog() {
        String[] options = new String[] {
            "1: Audio Bluetooth / BlackBerry",
            "2: Haut-parleur Smartphone",
            "3: Écouteur Smartphone",
            "4: Basculer HP/Combiné BlackBerry",
            "Annuler"
        };
        int choice = Dialog.ask("Choisir la sortie audio :", options, 0);
        if (choice == 0) {
            callManager.setAudioRoute("BLUETOOTH");
        } else if (choice == 1) {
            callManager.setAudioRoute("SPEAKERPHONE");
        } else if (choice == 2) {
            callManager.setAudioRoute("EARPIECE");
        } else if (choice == 3) {
            callManager.toggleLocalAudio();
        }
    }
    
    private void answer() {
        callManager.answerCall(callId);
        setCallActive();
    }
    
    private void reject() {
        callManager.rejectCall(callId);
        stopDurationTimer();
        close();
    }
    
    private void hangup() {
        callManager.endCurrentCall();
        setCallEnded();
    }
    
    private void toggleSpeaker() {
        callManager.toggleSpeaker();
    }
    
    public boolean onClose() {
        stopDurationTimer();
        return super.onClose();
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) { // Touche Verte physique
            if (!isOutbound && !isActive && !isEnded) {
                answer();
                return true;
            } else if (isActive) {
                // Basculer la route audio en cycle
                showAudioRouteDialog();
                return true;
            }
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { // Touche Rouge ou Retour
            if (!isOutbound && !isActive && !isEnded) {
                reject();
            } else if (!isEnded) {
                hangup();
            }
            return true;
        } else if (key == Keypad.KEY_SPACE) {
            toggleSpeaker();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);
        
        if (!isOutbound && !isActive && !isEnded) {
            menu.add(new MenuItem("Décrocher", 100, 10) {
                public void run() { answer(); }
            });
            menu.add(new MenuItem("Refuser l'appel", 100, 20) {
                public void run() { reject(); }
            });
        } else if (!isEnded) {
            // Options audio demandées par le protocole
            menu.add(new MenuItem("Haut-parleur ON/OFF", 100, 10) {
                public void run() { toggleSpeaker(); }
            });
            menu.add(new MenuItem("Route : Audio Bluetooth / BB", 100, 15) {
                public void run() { callManager.setAudioRoute("BLUETOOTH"); }
            });
            menu.add(new MenuItem("Route : HP Smartphone", 100, 16) {
                public void run() { callManager.setAudioRoute("SPEAKERPHONE"); }
            });
            menu.add(new MenuItem("Route : Écouteur Smartphone", 100, 17) {
                public void run() { callManager.setAudioRoute("EARPIECE"); }
            });
            menu.add(new MenuItem("Bascule Audio Local BlackBerry", 100, 18) {
                public void run() { callManager.toggleLocalAudio(); }
            });
            menu.add(new MenuItem("Raccrocher", 100, 30) {
                public void run() { hangup(); }
            });
        }
    }
    
    // --- Custom UI Component for Colored Buttons ---
    private static class CallButtonField extends Field {
        private String label;
        private int bgColor;
        private int focusColor;
        private int fontColor = Color.WHITE;
        private int width, height;

        public CallButtonField(String label, int bgColor, int focusColor, int width, int height) {
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
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12); 
            
            // Bordure
            graphics.setColor(focused ? 0xFFFFFF : 0x444444); 
            graphics.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
            
            graphics.setColor(focused ? Color.BLACK : fontColor);
            Font f = graphics.getFont();
            try { f = Font.getDefault().derive(Font.BOLD, 12); graphics.setFont(f); } catch(Exception ignored){}
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
