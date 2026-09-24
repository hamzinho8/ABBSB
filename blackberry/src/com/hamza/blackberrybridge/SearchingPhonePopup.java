package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;

/**
 * Popup displayed on BlackBerry while searching for Android smartphone.
 * Allows cancelling the alarm, and is automatically dismissed when Android stops it.
 */
public class SearchingPhonePopup extends PopupScreen {
    private SmartBridgeApp app;

    public SearchingPhonePopup(final SmartBridgeApp app) {
        super(new VerticalFieldManager(Field.FIELD_HCENTER | Field.FIELD_VCENTER));
        this.app = app;

        DarkLabelField title = new DarkLabelField("\uD83D\uDD0D Recherche en cours...", Field.FIELD_HCENTER, 0x00FF88);
        try {
            title.setFont(Font.getDefault().derive(Font.BOLD, 18));
        } catch (Exception e) {}
        add(title);
        add(new SeparatorField());

        DarkLabelField desc = new DarkLabelField("Alarme activée sur votre smartphone.", Field.FIELD_HCENTER, Color.WHITE);
        add(desc);

        VerticalFieldManager spacer = new VerticalFieldManager();
        spacer.setPadding(8, 0, 8, 0);
        add(spacer);

        ButtonField btnStop = new ButtonField("Arrêter la sonnerie", ButtonField.CONSUME_CLICK | Field.FIELD_HCENTER);
        btnStop.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (app != null && app.getConnectionManager() != null) {
                    app.getConnectionManager().sendData("FIND_PHONE_STOP\n");
                }
                close();
            }
        });
        add(btnStop);
    }

    protected void sublayout(int width, int height) {
        int popupWidth = (int) (width * 0.90);
        int popupHeight = Math.min(super.getPreferredHeight(), (int) (height * 0.60));
        super.sublayout(popupWidth, popupHeight);
        setExtent(popupWidth, popupHeight);
    }
}
