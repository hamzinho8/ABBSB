package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Application;

public class CallScreen extends MainScreen {
    private CallManager callManager;
    private String callId;
    private DarkLabelField statusLabel;
    
    public CallScreen(CallManager cm, String id, String name, String number) {
        super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
        this.callManager = cm;
        this.callId = id;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER | Field.USE_ALL_HEIGHT);
        vfm.setPadding(30, 0, 0, 0);
        
        DarkLabelField title = new DarkLabelField("Appel Entrant...", Field.FIELD_HCENTER, 0x00FF00); // Green title
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 22)); } catch(Exception e){}
        
        DarkLabelField nameLabel = new DarkLabelField(name != null && name.length() > 0 ? name : "Inconnu", Field.FIELD_HCENTER, Color.WHITE);
        try { nameLabel.setFont(Font.getDefault().derive(Font.BOLD, 45)); } catch(Exception e){}
        
        DarkLabelField numberLabel = new DarkLabelField(number != null ? number : "", Field.FIELD_HCENTER, 0xAAAAAA);
        try { numberLabel.setFont(Font.getDefault().derive(Font.PLAIN, 24)); } catch(Exception e){}
        
        statusLabel = new DarkLabelField("Sonnerie...", Field.FIELD_HCENTER, 0xFFCC00);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 20)); } catch(Exception e){}
        
        vfm.add(title);
        vfm.add(new SeparatorField());
        vfm.add(nameLabel);
        vfm.add(numberLabel);
        vfm.add(new SeparatorField());
        vfm.add(statusLabel);
        
        // Spacer
        VerticalFieldManager spacer = new VerticalFieldManager();
        spacer.setPadding(30, 0, 0, 0);
        vfm.add(spacer);
        
        HorizontalFieldManager hfm = new HorizontalFieldManager(Field.FIELD_HCENTER);
        
        CallButtonField btnAnswer = new CallButtonField("Décrocher", 0x009900, 0x00FF00, 150, 60);
        btnAnswer.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { answer(); }
        });
        
        CallButtonField btnReject = new CallButtonField("Refuser", 0xCC0000, 0xFF3333, 150, 60);
        btnReject.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { reject(); }
        });
        
        HorizontalFieldManager btnSpacing = new HorizontalFieldManager();
        btnSpacing.setPadding(0, 10, 0, 10);
        
        hfm.add(btnAnswer);
        hfm.add(btnSpacing);
        hfm.add(btnReject);
        
        vfm.add(hfm);
        
        DarkLabelField hintLabel = new DarkLabelField("(Use Physical Green/Red Keys)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 16)); } catch(Exception e){}
        VerticalFieldManager hintSpacer = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSpacer.setPadding(15, 0, 0, 0);
        hintSpacer.add(hintLabel);
        vfm.add(hintSpacer);
        
        add(vfm);
    }
    
    public void setStatus(final String status) {
        Application.getApplication().invokeLater(new Runnable() {
            public void run() {
                statusLabel.setText(status);
                if (status.equals("Active")) {
                    statusLabel.setColor(0x00FF00); // Green
                    statusLabel.setText("Active - In Call");
                }
            }
        });
    }
    
    private void answer() {
        callManager.answerCall(callId);
        HardwareManager.stopAlerts();
        close();
    }
    
    private void reject() {
        callManager.rejectCall(callId);
        HardwareManager.stopAlerts();
        close();
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) { // Physical Green key
            answer();
            return true;
        } else if (key == Keypad.KEY_END) { // Physical Red key
            reject();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    // --- Custom UI Component for Colored Buttons ---
    private class CallButtonField extends Field {
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
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20); 
            
            // Draw a subtle inner border for style
            graphics.setColor(0xFFFFFF); 
            // setGlobalAlpha doesn't exist in all JDE 7.1 graphics, fallback to standard drawing if needed.
            // Let's avoid setGlobalAlpha to be safe and just draw a simple border.
            // graphics.setGlobalAlpha(50);
            graphics.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, 18, 18);
            // graphics.setGlobalAlpha(255);
            
            graphics.setColor(focused ? Color.BLACK : fontColor);
            Font f = graphics.getFont();
            try { f = Font.getDefault().derive(Font.BOLD, 20); graphics.setFont(f); } catch(Exception e){}
            int tx = (getWidth() - f.getAdvance(label)) / 2;
            int ty = (getHeight() - f.getHeight()) / 2;
            graphics.drawText(label, tx, ty);
        }
        
        protected boolean navigationClick(int status, int time) {
            fieldChangeNotify(0);
            return true;
        }
        
        protected boolean invokeAction(int action) {
            switch(action) {
                case ACTION_INVOKE: { fieldChangeNotify(0); return true; }
            }
            return super.invokeAction(action);
        }
        
        protected boolean touchEvent(net.rim.device.api.ui.TouchEvent message) {
            if(message.getEvent() == net.rim.device.api.ui.TouchEvent.CLICK) {
                fieldChangeNotify(0);
                return true;
            }
            return super.touchEvent(message);
        }
    }
}
