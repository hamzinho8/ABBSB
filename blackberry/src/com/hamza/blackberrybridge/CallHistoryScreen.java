package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.Keypad;

public class CallHistoryScreen extends MainScreen {
    private SmartBridgeApp app;
    private VerticalFieldManager listContainer;

    // Call history item data model
    public static class CallItem {
        public String name;
        public String number;
        public String time;
        public boolean isIncoming;
        public String duration;
        public String simName;

        public CallItem(String name, String number, String time, boolean isIncoming, String duration, String simName) {
            this.name = name;
            this.number = number;
            this.time = time;
            this.isIncoming = isIncoming;
            this.duration = duration;
            this.simName = simName;
        }
    }

    private static final CallItem[] SEEDED_CALLS = new CallItem[] {
        new CallItem("Amina Mansouri", "+212634934134", "14:28", true, "04:12", "inwi"),
        new CallItem("Youssef Bennani", "+212655881230", "12:15", false, "01:45", "Orange"),
        new CallItem("Hamza H.", "+212611223344", "10:04", true, "08:30", "inwi"),
        new CallItem("Service Client inwi", "220", "Hier 18:40", false, "02:10", "inwi"),
        new CallItem("Dr. Karim Lahlou", "+212672409918", "Hier 15:22", true, "00:54", "Orange"),
        new CallItem("Fatima Zahra", "+212698712345", "24 Sep", true, "05:20", "inwi"),
        new CallItem("Orange Recharges", "121", "24 Sep", false, "03:05", "Orange"),
        new CallItem("Sara Alami", "+212644332211", "23 Sep", true, "02:18", "inwi")
    };

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

        for (int i = 0; i < SEEDED_CALLS.length; i++) {
            final CallItem item = SEEDED_CALLS[i];
            listContainer.add(createCallRow(item));
        }

        add(listContainer);
    }

    private Field createCallRow(final CallItem item) {
        return new CallRowField(item, new Runnable() {
            public void run() {
                promptCall(item);
            }
        });
    }

    private void promptCall(final CallItem item) {
        app.getCallManager().initiateOutboundCall(item.number, item.name);
        close();
    }

    public boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_ESCAPE || key == Keypad.KEY_END) {
            close();
            return true;
        }
        return super.keyDown(keycode, time);
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

            // Background & Border
            graphics.setColor(focused ? 0x083344 : 0x11161B);
            graphics.fillRoundRect(0, 0, w, h, 6, 6);

            graphics.setColor(focused ? 0x00E5FF : 0x1F2937);
            graphics.drawRoundRect(0, 0, w, h, 6, 6);

            // Direction badge: [IN] in green, [OUT] in cyan
            if (item.isIncoming) {
                graphics.setColor(0x22C55E);
                graphics.drawText("[IN]", 6, 4);
            } else {
                graphics.setColor(0x00E5FF);
                graphics.drawText("[OUT]", 6, 4);
            }

            // Name in bold white
            graphics.setColor(focused ? Color.WHITE : 0xF1F5F9);
            try {
                graphics.setFont(Font.getDefault().derive(Font.BOLD, 12));
            } catch (Throwable e) {}
            graphics.drawText(item.name, 42, 4);

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
            graphics.drawText(item.number + " • " + item.duration, 42, 18);
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
