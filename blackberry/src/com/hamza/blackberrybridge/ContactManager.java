package com.hamza.blackberrybridge;

import java.util.Vector;
import net.rim.device.api.system.Application;

public class ContactManager {
    private SmartBridgeApp app;
    private Vector contacts;
    private ContactListScreen activeScreen;
    
    public ContactManager(SmartBridgeApp app) {
        this.app = app;
        this.contacts = new Vector();
    }
    
    public void setActiveScreen(ContactListScreen screen) {
        this.activeScreen = screen;
    }
    
    public void searchContacts(String query) {
        contacts.removeAllElements(); // Clear before new search
        if (activeScreen != null) {
            activeScreen.refreshList(); // Clear UI
        }
        app.getConnectionManager().sendData("CONTACT_SEARCH|" + query + "\n");
    }
    
    public void handleContact(String id, String name, String number) {
        final Contact c = new Contact(id, name, number);
        contacts.addElement(c);
        
        if (activeScreen != null) {
            Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    activeScreen.addContactToUI(c);
                }
            });
        }
    }
    
    public void handleContactsEnd(String countStr) {
        if (activeScreen != null) {
            Application.getApplication().invokeLater(new Runnable() {
                public void run() {
                    activeScreen.refreshList();
                }
            });
        }
    }
    
    public void callContact(String number) {
        if (app.getConnectionManager() != null) {
            app.getConnectionManager().sendData("CALL_OUTBOUND|" + number + "\n");
        }
    }
    
    public Vector getContacts() {
        return contacts;
    }
}
