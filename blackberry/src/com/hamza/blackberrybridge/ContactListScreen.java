package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;

public class ContactListScreen extends MainScreen {
    private ContactManager contactManager;
    private SmartBridgeApp app;
    private BasicEditField searchField;
    private DarkLabelField titleLabel;
    private DarkLabelField statusLabel;
    private ObjectListField contactList;
    private Vector currentDisplayedContacts;
    
    public ContactListScreen(ContactManager manager) {
        this(manager, null);
    }
    
    public ContactListScreen(ContactManager manager, SmartBridgeApp app) {
        super(MainScreen.DEFAULT_MENU | MainScreen.DEFAULT_CLOSE);
        this.contactManager = manager;
        this.app = app;
        this.contactManager.setActiveScreen(this);
        this.currentDisplayedContacts = new Vector();
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER);
        vfm.setPadding(2, 4, 2, 4);
        
        // Header
        HorizontalFieldManager header = new HorizontalFieldManager(Field.FIELD_HCENTER);
        titleLabel = new DarkLabelField("CONTACTS VIP", Field.FIELD_HCENTER, 0x00E5FF);
        try { titleLabel.setFont(Font.getDefault().derive(Font.BOLD, 15)); } catch(Exception ignored){}
        header.add(titleLabel);
        vfm.add(header);
        
        // Status bar (Sync info / progress)
        statusLabel = new DarkLabelField("", Field.FIELD_HCENTER, 0xAAAAAA);
        try { statusLabel.setFont(Font.getDefault().derive(Font.PLAIN, 12)); } catch(Exception ignored){}
        vfm.add(statusLabel);
        vfm.add(new SeparatorField());
        
        // Search bar
        HorizontalFieldManager searchContainer = new HorizontalFieldManager(Field.FIELD_HCENTER);
        searchContainer.setPadding(2, 0, 2, 0);
        
        searchField = new BasicEditField("Recherche: ", "", 30, BasicEditField.FILTER_DEFAULT) {
            protected boolean keyChar(char key, int status, int time) {
                boolean result = super.keyChar(key, status, time);
                // Instantaneous filtering on typing
                filterContacts(getText());
                return result;
            }
        };
        searchField.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                filterContacts(searchField.getText());
            }
        });
        
        ButtonField btnSearch = new ButtonField("OK", ButtonField.CONSUME_CLICK);
        btnSearch.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                filterContacts(searchField.getText());
            }
        });
        
        searchContainer.add(searchField);
        searchContainer.add(btnSearch);
        vfm.add(searchContainer);
        
        // Action buttons bar
        HorizontalFieldManager actionsBar = new HorizontalFieldManager(Field.FIELD_HCENTER);
        actionsBar.setPadding(2, 0, 4, 0);
        
        ButtonField btnSync = new ButtonField("Demander VIP", ButtonField.CONSUME_CLICK);
        btnSync.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                contactManager.requestVipContacts();
            }
        });
        
        ButtonField btnClear = new ButtonField("Vider", ButtonField.CONSUME_CLICK);
        btnClear.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (Dialog.ask(Dialog.D_YES_NO, "Effacer les contacts VIP ?") == Dialog.YES) {
                    contactManager.clearContacts();
                }
            }
        });
        
        actionsBar.add(btnSync);
        actionsBar.add(btnClear);
        vfm.add(actionsBar);
        vfm.add(new SeparatorField());
        
        // Enhanced VIP Contact list (2-line custom rendering)
        contactList = new ObjectListField() {
            public int getRowHeight() {
                return 34;
            }
            
            public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
                if (index < currentDisplayedContacts.size()) {
                    Contact c = (Contact) currentDisplayedContacts.elementAt(index);
                    
                    if (graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS)) {
                        graphics.setColor(0x005588);
                        graphics.fillRect(0, y, width, getRowHeight());
                        graphics.setColor(0x00E5FF);
                        graphics.drawRect(0, y, width, getRowHeight());
                        
                        // Line 1: [VIP] Name
                        graphics.setColor(Color.WHITE);
                        graphics.drawText("[VIP] " + c.name, 4, y + 2);
                        // Line 2: Number
                        graphics.setColor(0x80D8FF);
                        graphics.drawText("Tél: " + c.number, 14, y + 18);
                    } else {
                        // Alternate row styling
                        if (index % 2 == 1) {
                            graphics.setColor(0x161616);
                            graphics.fillRect(0, y, width, getRowHeight());
                        }
                        // Line 1: VIP badge + Name
                        graphics.setColor(0xFFD700); // Gold
                        graphics.drawText("★ ", 4, y + 2);
                        graphics.setColor(Color.WHITE);
                        graphics.drawText(c.name, 18, y + 2);
                        // Line 2: Number
                        graphics.setColor(0x888888);
                        graphics.drawText(c.number, 18, y + 18);
                    }
                }
            }
            
            protected boolean keyChar(char key, int status, int time) {
                if (key == '\n' || key == '\r' || key == 10 || key == 13) {
                    executeCall();
                    return true;
                }
                return super.keyChar(key, status, time);
            }
            
            protected boolean trackwheelClick(int status, int time) {
                executeCall();
                return true;
            }
            
            protected boolean navigationClick(int status, int time) {
                executeCall();
                return true;
            }
        };
        
        VerticalFieldManager listContainer = new VerticalFieldManager(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR);
        listContainer.add(contactList);
        vfm.add(listContainer);
        add(vfm);
        
        // Initial populate from local RMS
        refreshList();
    }
    
    public void setStatusMessage(final String message) {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (statusLabel != null) {
                    statusLabel.setText(message);
                }
            }
        });
    }
    
    public void filterContacts(String query) {
        Vector results = contactManager.searchContactsLocally(query);
        currentDisplayedContacts.removeAllElements();
        for (int i = 0; i < results.size(); i++) {
            currentDisplayedContacts.addElement(results.elementAt(i));
        }
        updateListField();
    }
    
    public void addContactToUI(Contact c) {
        currentDisplayedContacts.addElement(c);
        updateListField();
    }
    
    public void refreshList() {
        currentDisplayedContacts.removeAllElements();
        Vector contacts = contactManager.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            currentDisplayedContacts.addElement(contacts.elementAt(i));
        }
        updateListField();
        
        int count = contacts.size();
        if (titleLabel != null) {
            titleLabel.setText("CONTACTS VIP (" + count + ")");
        }
        if (statusLabel != null && (statusLabel.getText() == null || statusLabel.getText().length() == 0)) {
            statusLabel.setText(count + " contacts VIP enregistrés");
        }
    }
    
    private void updateListField() {
        int size = currentDisplayedContacts.size();
        Object[] arr = new Object[size];
        for (int i = 0; i < size; i++) {
            Contact c = (Contact) currentDisplayedContacts.elementAt(i);
            arr[i] = c.name + " (" + c.number + ")";
        }
        contactList.set(arr);
        contactList.invalidate();
    }
    
    private void executeCall() {
        int selectedIndex = contactList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < currentDisplayedContacts.size()) {
            final Contact c = (Contact) currentDisplayedContacts.elementAt(selectedIndex);
            contactManager.callContact(c.number);
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.inform("Appel en cours vers " + c.name);
                }
            });
            close();
        }
    }
    
    private void saveSelectedToNativeAddressBook() {
        int selectedIndex = contactList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < currentDisplayedContacts.size()) {
            final Contact c = (Contact) currentDisplayedContacts.elementAt(selectedIndex);
            boolean ok = contactManager.saveContactToBlackBerryAddressBook(c);
            if (ok) {
                Dialog.inform("Contact '" + c.name + "' sauvegardé dans le carnet BlackBerry !");
            } else {
                Dialog.alert("Impossible d'enregistrer le contact dans le carnet natif.");
            }
        }
    }
    
    protected void makeMenu(Menu menu, int instance) {
        super.makeMenu(menu, instance);
        
        MenuItem callItem = new MenuItem("Appeler via Android", 100, 10) {
            public void run() {
                executeCall();
            }
        };
        menu.add(callItem);
        
        MenuItem syncItem = new MenuItem("Demander les contacts VIP", 101, 20) {
            public void run() {
                contactManager.requestVipContacts();
            }
        };
        menu.add(syncItem);
        
        MenuItem saveNativeItem = new MenuItem("Sauvegarder dans carnet BB", 102, 30) {
            public void run() {
                saveSelectedToNativeAddressBook();
            }
        };
        menu.add(saveNativeItem);
        
        MenuItem clearItem = new MenuItem("Vider les contacts VIP", 103, 40) {
            public void run() {
                if (Dialog.ask(Dialog.D_YES_NO, "Effacer les contacts VIP ?") == Dialog.YES) {
                    contactManager.clearContacts();
                }
            }
        };
        menu.add(clearItem);
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
    
    public void close() {
        contactManager.setActiveScreen(null);
        super.close();
    }
}
