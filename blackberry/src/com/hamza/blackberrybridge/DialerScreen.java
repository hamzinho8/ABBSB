package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

/**
 * Écran unifié d'Appels et de Journal d'Historique pour BlackBerry Curve 9300.
 * Intègre la numérotation rapide et la liste réelle des appels émis / reçus / manqués.
 */
public class DialerScreen extends MainScreen {
    private BasicEditField phoneField;
    private SmartBridgeApp app;
    private VerticalFieldManager historyContainer;

    public DialerScreen(final SmartBridgeApp app, final CallManager callManager) {
        super(MainScreen.VERTICAL_SCROLL | MainScreen.VERTICAL_SCROLLBAR);
        this.app = app;

        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));

        VerticalFieldManager topContainer = new VerticalFieldManager(Field.FIELD_HCENTER);
        topContainer.setPadding(4, 6, 2, 6);

        // Header Title
        DarkLabelField title = new DarkLabelField("APPELS & COMPOSITEUR", Field.FIELD_HCENTER, 0x00E5FF);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 14)); } catch (Exception e) {}
        topContainer.add(title);

        // Number input and call button bar
        HorizontalFieldManager dialBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        dialBar.setPadding(4, 0, 4, 0);

        VerticalFieldManager inputWrapper = new VerticalFieldManager();
        inputWrapper.setBackground(BackgroundFactory.createSolidBackground(0x1E293B));
        inputWrapper.setPadding(4, 6, 4, 6);

        phoneField = new BasicEditField("", "", 25, BasicEditField.FILTER_PHONE);
        try { phoneField.setFont(Font.getDefault().derive(Font.BOLD, 16)); } catch (Exception e) {}
        inputWrapper.add(phoneField);

        CallButtonField btnCall = new CallButtonField("Appeler", 0x15803D, 0x22C55E, 84, 28);
        btnCall.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                executeCall();
            }
        });

        dialBar.add(inputWrapper);
        dialBar.add(btnCall);
        topContainer.add(dialBar);

        // Separator with Call History Header
        DarkLabelField historyHeader = new DarkLabelField("JOURNAL DES APPELS ÉMIS / REÇUS", Field.FIELD_HCENTER, 0xFACC15);
        try { historyHeader.setFont(Font.getDefault().derive(Font.BOLD, 11)); } catch (Exception e) {}
        topContainer.add(historyHeader);

        DarkLabelField hint = new DarkLabelField("Clic sur un appel pour rappeler | Touche Verte pour composer", Field.FIELD_HCENTER, 0x64748B);
        try { hint.setFont(Font.getDefault().derive(Font.PLAIN, 9)); } catch (Exception e) {}
        topContainer.add(hint);

        add(topContainer);

        // History items container
        historyContainer = new VerticalFieldManager();
        historyContainer.setPadding(2, 4, 6, 4);

        refreshHistoryList();
        add(historyContainer);
    }

    private void refreshHistoryList() {
        historyContainer.deleteAll();
        Vector history = app.getCallManager().getCallHistory();

        if (history.size() == 0) {
            DarkLabelField emptyLabel = new DarkLabelField("Aucun appel dans le journal", Field.FIELD_HCENTER, 0x64748B);
            try { emptyLabel.setFont(Font.getDefault().derive(Font.ITALIC, 11)); } catch (Exception e) {}
            historyContainer.add(emptyLabel);
            return;
        }

        for (int i = 0; i < history.size(); i++) {
            final CallItem item = (CallItem) history.elementAt(i);
            historyContainer.add(new CallHistoryRowField(item, new Runnable() {
                public void run() {
                    promptRecall(item);
                }
            }));
        }
    }

    private void promptRecall(final CallItem item) {
        if (item.number != null && item.number.length() > 0) {
            close();
            app.getCallManager().initiateOutboundCall(item.number, item.name);
        }
    }

    private void executeCall() {
        String number = phoneField.getText();
        if (number != null) {
            number = number.trim();
        }

        if (number != null && number.length() > 0) {
            close();
            app.getCallManager().initiateOutboundCall(number, null);
        } else {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.alert("Veuillez saisir un numéro à composer.");
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

    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);

        menu.add(new MenuItem("Composer le Numéro", 100, 1) {
            public void run() {
                executeCall();
            }
        });

        menu.add(new MenuItem("Carnet de Contacts", 100, 2) {
            public void run() {
                close();
                app.getUIManager().openContacts();
            }
        });

        menu.add(new MenuItem("Vider le Journal", 100, 3) {
            public void run() {
                if (Dialog.ask(Dialog.D_YES_NO, "Effacer tout le journal d'appels ?") == Dialog.YES) {
                    app.getCallManager().clearCallHistory();
                    refreshHistoryList();
                }
            }
        });
    }

    // Custom interactive call history row widget for BlackBerry Curve 9300
    private static class CallHistoryRowField extends Field {
        private CallItem item;
        private Runnable onClick;

        public CallHistoryRowField(CallItem item, Runnable onClick) {
            super(FOCUSABLE);
            this.item = item;
            this.onClick = onClick;
        }

        public int getPreferredWidth() { return 312; }
        public int getPreferredHeight() { return 32; }

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

            // Badge direction: [IN] vert, [OUT] cyan, [MISSED] rouge
            if (item.isIncoming()) {
                graphics.setColor(0x22C55E); // Green
                graphics.drawText("[IN]", 6, 3);
            } else if (item.isOutgoing()) {
                graphics.setColor(0x00E5FF); // Cyan
                graphics.drawText("[OUT]", 6, 3);
            } else {
                graphics.setColor(0xEF4444); // Red
                graphics.drawText("[MISSED]", 6, 3);
            }

            // Name / Number
            graphics.setColor(focused ? Color.WHITE : 0xF1F5F9);
            try {
                graphics.setFont(Font.getDefault().derive(Font.BOLD, 11));
            } catch (Throwable e) {}
            graphics.drawText(item.name, 56, 3);

            // SIM badge
            graphics.setColor(0xFACC15);
            try {
                graphics.setFont(Font.getDefault().derive(Font.PLAIN, 9));
            } catch (Throwable e) {}
            graphics.drawText("[" + item.simName + "]", 195, 3);

            // Time
            graphics.setColor(0x94A3B8);
            graphics.drawText(item.time, w - 48, 3);

            // Subline: Number + Duration
            graphics.setColor(0x64748B);
            try {
                graphics.setFont(Font.getDefault().derive(Font.PLAIN, 9));
            } catch (Throwable e) {}
            graphics.drawText(item.number + " • " + item.duration, 56, 17);
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

    private static class CallButtonField extends Field {
        private String label;
        private int bgColor;
        private int focusColor;
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
            graphics.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            graphics.setColor(focused ? Color.BLACK : Color.WHITE);
            try {
                graphics.setFont(Font.getDefault().derive(Font.BOLD, 12));
            } catch (Exception e) {}
            Font f = graphics.getFont();
            int tx = (getWidth() - f.getAdvance(label)) / 2;
            int ty = (getHeight() - f.getHeight()) / 2;
            graphics.drawText(label, tx, ty);
        }

        protected boolean navigationClick(int status, int time) {
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
