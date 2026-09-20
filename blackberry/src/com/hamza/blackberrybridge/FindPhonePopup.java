package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.system.Display;

public class FindPhonePopup extends PopupScreen {
    
    public FindPhonePopup() {
        super(new VerticalFieldManager(Field.FIELD_HCENTER | Field.FIELD_VCENTER));
        
        DarkLabelField title = new DarkLabelField("\uD83D\uDD0D Localisation...", Field.FIELD_HCENTER, 0xFF0000);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 20)); } catch(Exception e){}
        add(title);
        add(new SeparatorField());
        
        DarkLabelField desc = new DarkLabelField("Votre téléphone Android vous cherche.", Field.FIELD_HCENTER, Color.WHITE);
        add(desc);
        
        VerticalFieldManager spacer = new VerticalFieldManager();
        spacer.setPadding(20, 0, 20, 0);
        add(spacer);
        
        ButtonField btnOk = new ButtonField("OK", ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
        btnOk.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                HardwareManager.stopFindPhoneAlert();
                close();
            }
        });
        
        add(btnOk);
    }
    
    protected void sublayout(int width, int height) {
        int displayWidth = Display.getWidth();
        int displayHeight = Display.getHeight();
        int popupWidth = (int)(displayWidth * 0.85);
        int popupHeight = Math.min(super.getPreferredHeight(), (int)(displayHeight * 0.50));
        
        super.sublayout(popupWidth, popupHeight);
        setExtent(popupWidth, popupHeight);
    }
}
