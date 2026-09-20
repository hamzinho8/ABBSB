package com.hamza.blackberrybridge;

import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.decor.*;
import net.rim.device.api.system.Characters;
import java.util.Vector;

public class ContactListScreen extends MainScreen {
    private ContactManager contactManager;
    private BasicEditField searchField;
    private ObjectListField contactList;
    private Vector currentDisplayedContacts;
    
    public ContactListScreen(ContactManager manager) {
        super(MainScreen.NO_VERTICAL_SCROLL | MainScreen.NO_HORIZONTAL_SCROLL);
        this.contactManager = manager;
        this.contactManager.setActiveScreen(this);
        this.currentDisplayedContacts = new Vector();
        
        getMainManager().setBackground(BackgroundFactory.createSolidBackground(Color.BLACK));
        
        VerticalFieldManager vfm = new VerticalFieldManager(Field.FIELD_HCENTER | Field.USE_ALL_HEIGHT);
        
        DarkLabelField title = new DarkLabelField("CARNET D'ADRESSES", Field.FIELD_HCENTER, 0x00A2E8);
        try { title.setFont(Font.getDefault().derive(Font.BOLD, 22)); } catch(Exception e){}
        vfm.add(title);
        vfm.add(new SeparatorField());
        
        HorizontalFieldManager searchContainer = new HorizontalFieldManager(Field.FIELD_HCENTER);
        searchContainer.setPadding(10, 5, 10, 5);
        
        searchField = new BasicEditField("Rechercher : ", "", 50, BasicEditField.FILTER_DEFAULT);
        
        ButtonField btnSearch = new ButtonField("Chercher", ButtonField.CONSUME_CLICK);
        btnSearch.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                String query = searchField.getText();
                contactManager.searchContacts(query != null ? query : "");
            }
        });
        
        searchContainer.add(searchField);
        searchContainer.add(btnSearch);
        vfm.add(searchContainer);
        vfm.add(new SeparatorField());
        
        contactList = new ObjectListField() {
            public void drawListRow(ListField listField, Graphics graphics, int index, int y, int width) {
                if (index < currentDisplayedContacts.size()) {
                    Contact c = (Contact) currentDisplayedContacts.elementAt(index);
                    String text = c.name + " (" + c.number + ")";
                    
                    if (graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS)) {
                        graphics.setColor(0x00A2E8);
                        graphics.fillRect(0, y, width, getRowHeight());
                        graphics.setColor(Color.WHITE);
                    } else {
                        graphics.setColor(Color.WHITE);
                    }
                    
                    graphics.drawText(text, 5, y);
                }
            }
            protected boolean keyChar(char key, int status, int time) {
                if (key == Characters.ENTER) {
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
        
        contactList.set(new Object[0]);
        
        VerticalFieldManager listContainer = new VerticalFieldManager(Manager.VERTICAL_SCROLL | Manager.VERTICAL_SCROLLBAR);
        listContainer.add(contactList);
        
        vfm.add(listContainer);
        add(vfm);
        
        // Load initial default list (50 first contacts)
        contactManager.searchContacts("");
    }
    
    private void executeCall() {
        int selectedIndex = contactList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < currentDisplayedContacts.size()) {
            final Contact c = (Contact) currentDisplayedContacts.elementAt(selectedIndex);
            
            contactManager.callContact(c.number);
            
            net.rim.device.api.system.Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    Dialog.inform("Appel en cours vers " + c.name);
                }
            });
            
            close();
        }
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
    
    protected boolean keyDown(int keycode, int time) {
        int key = Keypad.key(keycode);
        if (key == Keypad.KEY_SEND) {
            executeCall();
            return true;
        } else if (key == Keypad.KEY_END || key == Keypad.KEY_ESCAPE) { 
            contactManager.setActiveScreen(null);
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
