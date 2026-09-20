package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.Display;

public class MessagePopupScreen extends PopupScreen {
    
    private SmartBridgeApp app;
    private String notificationId;
    private AutoTextEditField replyField;
    
    public MessagePopupScreen(SmartBridgeApp app, String id, String sender, String body) {
        super(new VerticalFieldManager(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR));
        this.app = app;
        this.notificationId = id;
        
        DarkLabelField title = new DarkLabelField("\uD83D\uDCAC Message de " + sender, Field.FIELD_HCENTER, 0x00A2E8); // Blue Title
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 18)); } catch(Exception e){}
        
        add(title);
        add(new SeparatorField());
        
        ActiveRichTextField bodyField = new ActiveRichTextField(body, Field.FIELD_HCENTER | Field.FOCUSABLE);
        try { bodyField.setFont(Font.getDefault().derive(Font.PLAIN, 16)); } catch(Exception e){}
        
        VerticalFieldManager bodyContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        bodyContainer.setPadding(10, 10, 10, 10);
        bodyContainer.add(bodyField);
        
        add(bodyContainer);
        add(new SeparatorField());
        
        replyField = new AutoTextEditField("Votre réponse : ", "");
        VerticalFieldManager replyContainer = new VerticalFieldManager();
        replyContainer.setPadding(5, 10, 10, 10);
        replyContainer.add(replyField);
        
        add(replyContainer);
        
        ButtonField btnReply = new ButtonField("Répondre", ButtonField.CONSUME_CLICK);
        btnReply.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                sendReply();
            }
        });
        
        ButtonField btnClose = new ButtonField("Ignorer", ButtonField.CONSUME_CLICK);
        btnClose.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                HardwareManager.stopMessageAlert();
                close();
            }
        });
        
        HorizontalFieldManager btnContainer = new HorizontalFieldManager(Field.FIELD_HCENTER);
        btnContainer.setPadding(10, 0, 5, 0);
        btnContainer.add(btnReply);
        
        HorizontalFieldManager spacer = new HorizontalFieldManager();
        spacer.setPadding(0, 5, 0, 5);
        btnContainer.add(spacer);
        
        btnContainer.add(btnClose);
        
        add(btnContainer);
    }
    
    private void sendReply() {
        String text = replyField.getText();
        if (text != null && text.length() > 0) {
            app.getConnectionManager().sendData("REPLY_MSG|" + notificationId + "|" + text + "\n");
            replyField.setText("");
            HardwareManager.stopMessageAlert();
            close();
            
            net.rim.device.api.system.Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.inform("Message envoyé !");
                }
            });
        } else {
            Dialog.alert("Veuillez saisir une réponse.");
        }
    }
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_ENTER) {
            sendReply();
            return true;
        }
        return super.keyDown(keycode, time);
    }
    
    protected void sublayout(int width, int height) {
        int displayWidth = Display.getWidth();
        int displayHeight = Display.getHeight();
        int popupWidth = (int)(displayWidth * 0.90);
        int popupHeight = Math.min(super.getPreferredHeight(), (int)(displayHeight * 0.90));
        
        super.sublayout(popupWidth, popupHeight);
        setExtent(popupWidth, popupHeight);
    }
}
