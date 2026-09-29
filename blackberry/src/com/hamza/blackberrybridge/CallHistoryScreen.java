package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.Keypad;

public class CallHistoryScreen extends MainScreen {
    private SmartBridgeApp app;
    private VerticalFieldManager listContainer;

    public CallHistoryScreen(SmartBridgeApp application) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = application;

        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));

        // Top Status Header
        VerticalFieldManager topHeader = new VerticalFieldManager(Field.FIELD_HCENTER);
        topHeader.setPadding(3, 4, 3, 4);

        DarkLabelField title = new DarkLabelField("HISTORIQUE DES APPELS", Field.FIELD_HCENTER, 0x00E5FF);
        try {
            title.setFont(Font.getDefault().derive(Font.BOLD, 14));
        } catch (Throwable e) {}
        topHeader.add(title);

        DarkLabelField sub = new DarkLabelField("Clic Trackpad / Touche Verte pour rappeler", Field.FIELD_HCENTER, 0x64748B);
        try {
            sub.setFont(Font.getDefault().derive(Font.PLAIN, 10));
        } catch (Throwable e) {}
        topHeader.add(sub);

        add(topHeader);

        // List Container
        listContainer = new VerticalFieldManager();
        listContainer.setPadding(2, 4, 4, 4);

        refreshList();
        add(listContainer);
    }

    public void refreshList() {
        listContainer.deleteAll();
        Vector history = app.getCallManager().getCallHistory();

        if (history.size() == 0) {
            DarkLabelField empty = new DarkLabelField("Aucun appel enregistré", Field.FIELD_HCENTER, 0x64748B);
            try { empty.setFont(Font.getDefault().derive(Font.ITALIC, 11)); } catch (Throwable e) {}
            listContainer.add(empty);
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            final CallItem item = (CallItem) history.elementAt(i);
            listContainer.add(createCallRow(item));
        }
    }

    private Field createCallRow(final CallItem item) {
        return new CallRowField(item, new Runnable() {
            public void run() {
                promptCall(item);
            }
        });
    }

    private void promptCall(final CallItem item) {
        if (item.number != null && item.number.length() > 0) {
            app.getCallManager().initiateOutboundCall(item.number, item.name);
            close();
        }
    }

    public boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_ESCAPE || key == Keypad.KEY_END) {
            close();
            return true;
        }
        return super.keyDown(keycode, time);
    }

    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);
        menu.add(new MenuItem("Vider l'historique", 100, 1) {
            public void run() {
                if (Dialog.ask(Dialog.D_YES_NO, "Effacer tout l'historique ?") == Dialog.YES) {
                    app.getCallManager().clearCallHistory();
                    refreshList();
                }
            }
        });
        menu.add(new MenuItem("Composer un numéro", 100, 2) {
            public void run() {
                close();
                app.getUIManager().openDialer();
            }
        });
    }

    // Custom focusable Call Row for Curve 9300
    private static class CallRowField extends Field {
        private CallItem item;
        private Runnable onClick;

        public CallRowField(CallItem item, Runnable onClick) {
            super(FOCUSABLE);
            this.item = item;
            this.onClick = onClick;
        }

        public int getPreferredWidth() { return 312; }
        public int getPreferredHeight() { return 34; }

        protected void layout(int width, int height) {
            setExtent(getPreferredWidth(), getPreferredHeight());
        }

        protected void paint(Graphics graphics) {
            boolean focused = isFocus();
            int w = getWidth();
            int h = getHeight();

            graphics.setColor(focused ? 0x083344 : 0x11161B);
            graphics.fillRoundRect(0, 0, w, h, 6, 6);
            graphics.setColor(focused ? 0x00E5FF : 0x1F2937);
            graphics.drawRoundRect(0, 0, w, h, 6, 6);

            // Direction badge: [IN] in green, [OUT] in cyan, [MISSED] in red
            if (item.isIncoming()) {
                graphics.setColor(0x22C55E);
                graphics.drawText("[IN]", 6, 4);
            } else if (item.isOutgoing()) {
                graphics.setColor(0x00E5FF);
                graphics.drawText("[OUT]", 6, 4);
            } else {
                graphics.setColor(0xEF4444);
                graphics.drawText("[MISSED]", 6, 4);
            }

            // Name in bold white
            graphics.setColor(focused ? Color.WHITE : 0xF1F5F9);
            try {
                graphics.setFont(Font.getDefault().derive(Font.BOLD, 12));
            } catch (Throwable e) {}
            graphics.drawText(item.name, 48, 4);

            // SIM badge
            graphics.setColor(0xFACC15);
            try {
                graphics.setFont(Font.getDefault().derive(Font.PLAIN, 10));
            } catch (Throwable e) {}
            graphics.drawText("[" + item.simName + "]", 200, 4);

            // Time & duration on right
            graphics.setColor(0x94A3B8);
            graphics.drawText(item.time, w - 50, 4);

            // Subline: Number + Duration
            graphics.setColor(0x64748B);
            try {
                graphics.setFont(Font.getDefault().derive(Font.PLAIN, 10));
            } catch (Throwable e) {}
            graphics.drawText(item.number + " • " + item.duration, 48, 18);
        }

        protected boolean navigationClick(int status, int time) {
            if (onClick != null) onClick.run();
            return true;
        }

        protected boolean invokeAction(int action) {
            if (action == ACTION_INVOKE) {
                if (onClick != null) onClick.run();
                return true;
            }
            return super.invokeAction(action);
        }
    }
}
