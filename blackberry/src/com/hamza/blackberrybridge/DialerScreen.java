package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.ui.text.TextFilter;
import net.rim.device.api.system.Application;

public class DialerScreen extends MainScreen {
    
    private BasicEditField phoneField;
    private CallManager callManager;
    private SmartBridgeApp app;
    
    public DialerScreen(SmartBridgeApp app, CallManager callManager) {
        super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
        this.app = app;
        this.callManager = callManager;
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER | Field.USE_ALL_HEIGHT);
        vfm.setPadding(30, 10, 20, 10);
        
        DarkLabelField title = new DarkLabelField("COMPOSER UN NUMÉRO", Field.FIELD_HCENTER, 0x00A2E8);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 22)); } catch(Exception e){}
        vfm.add(title);
        vfm.add(new SeparatorField());
        
        VerticalFieldManager spacerTop = new VerticalFieldManager();
        spacerTop.setPadding(40, 0, 0, 0);
        vfm.add(spacerTop);
        
        // Custom background for the edit field
        VerticalFieldManager fieldContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        fieldContainer.setBackground(BackgroundFactory.createSolidBackground(0x222222));
        fieldContainer.setPadding(10, 10, 10, 10);
        
        phoneField = new BasicEditField("", "", 20, Field.FIELD_HCENTER);
        try { phoneField.setFilter(TextFilter.get(TextFilter.PHONE)); } catch(Throwable t) {}
        try { phoneField.setFont(Font.getDefault().derive(Font.BOLD, 36)); } catch(Exception e){}
        
        fieldContainer.add(phoneField);
        vfm.add(fieldContainer);
        
        VerticalFieldManager spacerMid = new VerticalFieldManager();
        spacerMid.setPadding(40, 0, 0, 0);
        vfm.add(spacerMid);
        
        CallButtonField btnCall = new CallButtonField("Appeler", 0x009900, 0x00FF00, 200, 60);
        btnCall.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                executeCall();
            }
        });
        
        vfm.add(btnCall);
        
        DarkLabelField hintLabel = new DarkLabelField("(Appuyez sur la touche Verte pour appeler)", Field.FIELD_HCENTER, 0x555555);
        try { hintLabel.setFont(Font.getDefault().derive(Font.PLAIN, 14)); } catch(Exception e){}
        VerticalFieldManager hintSpacer = new VerticalFieldManager(Field.FIELD_HCENTER);
        hintSpacer.setPadding(20, 0, 0, 0);
        hintSpacer.add(hintLabel);
        vfm.add(hintSpacer);
        
        add(vfm);
    }
    
    private void executeCall() {
        String number = phoneField.getText();
        if (number != null) {
            number = number.trim();
        }
        
        if (number != null && number.length() > 0) {
            app.getConnectionManager().sendData("CALL_OUTBOUND|" + number + "\n");
            
            Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.inform("Appel lancé vers " + phoneField.getText().trim());
                }
            });
            
            close();
        } else {
            Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.alert("Veuillez saisir un numéro.");
                }
            });
        }
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) {
            executeCall();
            return true;
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) {
            close();
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
            super(FOCUSABLE | Field.FIELD_HCENTER);
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
            
            graphics.setColor(0xFFFFFF); 
            graphics.drawRoundRect(2, 2, getWidth()-4, getHeight()-4, 18, 18);
            
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
