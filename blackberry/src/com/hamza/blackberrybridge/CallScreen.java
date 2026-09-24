package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Écran d'appel plein écran pour BlackBerry Curve 9300 (OS 5.0).
 * Gère les appels entrants (avec nom de la SIM), les appels sortants,
 * le décrochage, le raccrochage et le basculement du haut-parleur audio.
 */
public class CallScreen extends MainScreen {
    private CallManager callManager;
    private String callId;
    private String name;
    private String number;
    private String simName;
    private boolean isOutbound;
    private boolean isActive = false;
    private boolean speakerOn = false;
    
    private DarkLabelField headerLabel;
    private DarkLabelField simLabel;
    private DarkLabelField nameLabel;
    private DarkLabelField numberLabel;
    private DarkLabelField statusLabel;
    private DarkLabelField speakerStatusLabel;
    
    private CallButtonField btnAnswer;
    private CallButtonField btnRejectOrHangup;
    private CallButtonField btnSpeaker;
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
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER);
        vfm.setPadding(6, 6, 6, 6);
        
        // 1. En-tête : Type d'appel
        String headerText = isOutbound ? "APPEL SORTANT" : "APPEL ENTRANT";
        int headerColor = isOutbound ? 0x00E5FF : 0x00FF00;
        headerLabel = new DarkLabelField(headerText, Field.FIELD_HCENTER, headerColor);
        try { headerLabel.setFont(Font.getDefault().derive(Font.BOLD, 15)); } catch(Exception ignored){}
        vfm.add(headerLabel);
        
        // 2. Ligne SIM : ex "sur [inwi]" ou "sur [Orange]"
        String simText = (this.simName.length() > 0) ? "sur [" + this.simName + "]" : "";
        simLabel = new DarkLabelField(simText, Field.FIELD_HCENTER, 0xFFD700); // Gold
        try { simLabel.setFont(Font.getDefault().derive(Font.BOLD, 13)); } catch(Exception ignored){}
        vfm.add(simLabel);
        
        vfm.add(new SeparatorField());
        
        // Spacer
        VerticalFieldManager sp1 = new VerticalFieldManager();
        sp1.setPadding(4, 0, 0, 0);
        vfm.add(sp1);
        
        // 3. Nom de l'interlocuteur
        nameLabel = new DarkLabelField(this.name, Field.FIELD_HCENTER, Color.WHITE);
        try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 20)); } catch(Exception ignored){}
        vfm.add(nameLabel);
        
        // 4. Numéro de téléphone
        numberLabel = new DarkLabelField(this.number, Field.FIELD_HCENTER, 0xAAAAAA);
        try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch(Exception ignored){}
        vfm.add(numberLabel);
        
        // 5. Statut actuel de l'appel
        String initialStatus = isOutbound ? "Numérotation..." : "Sonnerie...";
        int initialStatusColor = isOutbound ? 0x00E5FF : 0xFFCC00;
        statusLabel = new DarkLabelField(initialStatus, Field.FIELD_HCENTER, initialStatusColor);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 13)); } catch(Exception ignored){}
        vfm.add(statusLabel);
        
        // 6. Indicateur Haut-parleur
        speakerStatusLabel = new DarkLabelField("[Haut-parleur : ÉTEINT]", Field.FIELD_HCENTER, 0x777777);
        try { speakerStatusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 11)); } catch(Exception ignored){}
        vfm.add(speakerStatusLabel);
        
        // Spacer avant boutons
        VerticalFieldManager sp2 = new VerticalFieldManager();
        sp2.setPadding(8, 0, 0, 0);
        vfm.add(sp2);
        
        // 7. Boutons d'action
        buttonsManager = new HorizontalFieldManager(Field.FIELD_HCENTER);
        rebuildButtons();
        vfm.add(buttonsManager);
        
        // Indication des touches physiques Curve
        DarkLabelField hintLabel = new DarkLabelField("(Touche Verte: Décrocher | Rouge/Échap: Raccrocher)", Field.FIELD_HCENTER, 0x666666);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 10)); } catch(Exception ignored){}
        VerticalFieldManager hintSpacer = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSpacer.setPadding(6, 0, 0, 0);
        hintSpacer.add(hintLabel);
        vfm.add(hintSpacer);
        
        add(vfm);
    }
    
    private void rebuildButtons() {
        buttonsManager.deleteAll();
        
        if (!isOutbound && !isActive) {
            // Mode Appel Entrant : Décrocher (Vert) + Refuser (Rouge)
            btnAnswer = new CallButtonField("Décrocher", 0x008800, 0x00DD00, 125, 34);
            btnAnswer.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    answer();
                }
            });
            
            btnRejectOrHangup = new CallButtonField("Refuser", 0xAA0000, 0xEE2222, 125, 34);
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
            // Mode Appel Actif ou Appel Sortant : Raccrocher (Rouge) + Haut-parleur
            btnRejectOrHangup = new CallButtonField("Raccrocher", 0xAA0000, 0xEE2222, 125, 34);
            btnRejectOrHangup.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    hangup();
                }
            });
            
            btnSpeaker = new CallButtonField(speakerOn ? "HP: ACTIF" : "Haut-Parleur", speakerOn ? 0x005588 : 0x333333, 0x00A2E8, 125, 34);
            btnSpeaker.setChangeListener(new FieldChangeListener() {
                public void fieldChanged(Field field, int context) {
                    toggleSpeaker();
                }
            });
            
            buttonsManager.add(btnRejectOrHangup);
            HorizontalFieldManager spacer = new HorizontalFieldManager();
            spacer.setPadding(0, 4, 0, 4);
            buttonsManager.add(spacer);
            buttonsManager.add(btnSpeaker);
        }
    }
    
    public void setSimName(final String sim) {
        this.simName = sim;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (simLabel != null && sim != null && sim.length() > 0) {
                    simLabel.setText("sur [" + sim + "]");
                }
            }
        });
    }
    
    public void setStatus(final String status) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (statusLabel != null) {
                    statusLabel.setText(status);
                }
            }
        });
    }
    
    public void setCallActive() {
        this.isActive = true;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (headerLabel != null) {
                    headerLabel.setText("COMMUNICATION ACTIVE");
                    headerLabel.setColor(0x00FF00); // Lime green
                }
                if (statusLabel != null) {
                    statusLabel.setText("En communication");
                    statusLabel.setColor(0x00FF00);
                }
                rebuildButtons();
            }
        });
    }
    
    public void updateSpeakerStatus(final boolean on) {
        this.speakerOn = on;
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (speakerStatusLabel != null) {
                    if (on) {
                        speakerStatusLabel.setText("[Haut-parleur : ACTIVÉ]");
                        speakerStatusLabel.setColor(0x00FF00); // Vert
                    } else {
                        speakerStatusLabel.setText("[Haut-parleur : ÉTEINT]");
                        speakerStatusLabel.setColor(0x777777); // Gris
                    }
                }
                rebuildButtons();
            }
        });
    }
    
    private void answer() {
        callManager.answerCall(callId);
        setCallActive();
    }
    
    private void reject() {
        callManager.rejectCall(callId);
        close();
    }
    
    private void hangup() {
        callManager.endCurrentCall();
        close();
    }
    
    private void toggleSpeaker() {
        callManager.toggleSpeaker();
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) { // Touche Verte physique
            if (!isOutbound && !isActive) {
                answer();
                return true;
            }
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { // Touche Rouge ou Retour physique
            if (!isOutbound && !isActive) {
                reject();
            } else {
                hangup();
            }
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);
        
        // Option Haut-parleur ON/OFF demandée explicitement dans le protocole
        menu.add(new MenuItem("Haut-parleur ON/OFF", 100, 10) {
            public void run() {
                toggleSpeaker();
            }
        });
        
        if (!isOutbound && !isActive) {
            menu.add(new MenuItem("Décrocher", 100, 20) {
                public void run() {
                    answer();
                }
            });
            menu.add(new MenuItem("Refuser l'appel", 100, 30) {
                public void run() {
                    reject();
                }
            });
        } else {
            menu.add(new MenuItem("Raccrocher", 100, 20) {
                public void run() {
                    hangup();
                }
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
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16); 
            
            // Bordure nette
            graphics.setColor(focused ? 0xFFFFFF : 0x555555); 
            graphics.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 16, 16);
            
            graphics.setColor(focused ? Color.BLACK : fontColor);
            Font f = graphics.getFont();
            try { f = Font.getDefault().derive(Font.BOLD, 14); graphics.setFont(f); } catch(Exception ignored){}
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
