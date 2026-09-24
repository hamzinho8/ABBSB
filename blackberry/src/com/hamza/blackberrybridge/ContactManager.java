package com.hamza.blackberrybridge;

import java.util.Vector;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;

public class ContactManager {
    private static final String RMS_STORE_NAME = "BBSB_VIPContacts";
    private SmartBridgeApp app;
    private Vector vipContacts;
    private Vector incomingBatch;
    private ContactListScreen activeScreen;
    private boolean isReceivingBatch = false;
    private int expectedBatchCount = 0;
    
    public ContactManager(SmartBridgeApp app) {
        this.app = app;
        this.vipContacts = new Vector();
        this.incomingBatch = new Vector();
        loadVipContactsFromRMS();
    }
    
    public void setActiveScreen(ContactListScreen screen) {
        this.activeScreen = screen;
    }

    public ContactListScreen getActiveScreen() {
        return activeScreen;
    }
    
    // =========================================================================
    // RMS Persistence for VIP Contacts
    // =========================================================================
    public synchronized void loadVipContactsFromRMS() {
        RecordStore rs = null;
        RecordEnumeration re = null;
        try {
            rs = RecordStore.openRecordStore(RMS_STORE_NAME, true);
            re = rs.enumerateRecords(null, null, false);
            vipContacts.removeAllElements();
            while (re.hasNextElement()) {
                byte[] data = re.nextRecord();
                if (data != null && data.length > 0) {
                    String line = new String(data);
                    Contact c = Contact.fromRecordString(line);
                    if (c != null) {
                        vipContacts.addElement(c);
                    }
                }
            }
            LogManager.log("CONTACTS", "Loaded " + vipContacts.size() + " VIP contacts from RMS");
        } catch (Exception e) {
            LogManager.error("CONTACTS", "RMS load error: " + e.getMessage());
        } finally {
            if (re != null) {
                try { re.destroy(); } catch (Exception ignored) {}
            }
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
    }
    
    public synchronized void saveVipContactsToRMS() {
        RecordStore rs = null;
        try {
            try {
                RecordStore.deleteRecordStore(RMS_STORE_NAME);
            } catch (Exception ignored) {}
            
            rs = RecordStore.openRecordStore(RMS_STORE_NAME, true);
            for (int i = 0; i < vipContacts.size(); i++) {
                Contact c = (Contact) vipContacts.elementAt(i);
                byte[] bytes = c.toRecordString().getBytes();
                rs.addRecord(bytes, 0, bytes.length);
            }
            LogManager.log("CONTACTS", "Saved " + vipContacts.size() + " VIP contacts to RMS");
        } catch (Exception e) {
            LogManager.error("CONTACTS", "RMS save error: " + e.getMessage());
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (Exception ignored) {}
            }
        }
    }
    
    // =========================================================================
    // Protocol Handlers
    // =========================================================================

    /**
     * Commande CONTACTS_CLEAR : Vide la liste temporaire et le RecordStore
     */
    public synchronized void clearContacts() {
        vipContacts.removeAllElements();
        incomingBatch.removeAllElements();
        saveVipContactsToRMS();
        LogManager.log("CONTACTS", "Contacts VIP effacés");
        
        if (activeScreen != null) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (activeScreen != null) {
                        activeScreen.setStatusMessage("Contacts VIP effacés.");
                        activeScreen.refreshList();
                    }
                }
            });
        }
    }
    
    /**
     * Commande CONTACTS_START|<nombre>|VIP : Initialisation de la réception
     */
    public synchronized void startContactsBatch(int count, String type) {
        this.expectedBatchCount = count;
        this.isReceivingBatch = true;
        this.incomingBatch.removeAllElements();
        
        final String msg = "Réception de " + count + " contacts VIP...";
        LogManager.log("CONTACTS", msg);
        
        if (activeScreen != null) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (activeScreen != null) {
                        activeScreen.setStatusMessage(msg);
                    }
                }
            });
        }
    }
    
    /**
     * Commande CONTACT|<id>|<nom>|<numero> : Ajout d'un contact au lot
     */
    public synchronized void handleContact(String id, String name, String number) {
        final Contact c = new Contact(id, name, number, true);
        if (isReceivingBatch) {
            incomingBatch.addElement(c);
        } else {
            // Direct contact push
            vipContacts.addElement(c);
            saveVipContactsToRMS();
        }
        
        if (activeScreen != null) {
            UiApplication.getUiApplication().invokeLater(new Runnable() {
                public void run() {
                    if (activeScreen != null) {
                        if (isReceivingBatch) {
                            activeScreen.setStatusMessage("Reçu: " + incomingBatch.size() + "/" + expectedBatchCount + " (" + c.name + ")");
                        } else {
                            activeScreen.addContactToUI(c);
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Commande CONTACTS_END|<nombre> : Finalisation et notification
     */
    public synchronized void handleContactsEnd(String countStr) {
        int count = expectedBatchCount;
        try {
            if (countStr != null && countStr.length() > 0) {
                count = Integer.parseInt(countStr.trim());
            }
        } catch (Exception ex) {
            count = incomingBatch.size();
        }
        
        if (incomingBatch.size() > 0) {
            vipContacts.removeAllElements();
            for (int i = 0; i < incomingBatch.size(); i++) {
                vipContacts.addElement(incomingBatch.elementAt(i));
            }
            incomingBatch.removeAllElements();
            saveVipContactsToRMS();
        }
        
        isReceivingBatch = false;
        final int finalCount = count;
        final String informText = finalCount + " contacts VIP synchronisés avec succès !";
        LogManager.log("CONTACTS", informText);
        
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() {
                if (activeScreen != null) {
                    activeScreen.setStatusMessage(finalCount + " contacts VIP à jour.");
                    activeScreen.refreshList();
                }
                Dialog.inform(informText);
            }
        });
    }
    
    /**
     * Demande manuelle depuis le BlackBerry vers l'Android
     */
    public void requestVipContacts() {
        if (app.getConnectionManager() != null) {
            LogManager.log("CONTACTS", "Envoi GET_CONTACTS & SYNC_CONTACTS à Android");
            app.getConnectionManager().sendData("GET_CONTACTS\n");
            app.getConnectionManager().sendData("SYNC_CONTACTS\n");
            if (activeScreen != null) {
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        if (activeScreen != null) {
                            activeScreen.setStatusMessage("Demande des contacts VIP envoyée...");
                        }
                    }
                });
            }
        }
    }

    /**
     * Filtrage local rapide lors de la frappe dans la recherche
     */
    public Vector searchContactsLocally(String query) {
        if (query == null || query.trim().length() == 0) {
            Vector all = new Vector();
            for (int i = 0; i < vipContacts.size(); i++) {
                all.addElement(vipContacts.elementAt(i));
            }
            return all;
        }
        
        String lowerQuery = query.trim().toLowerCase();
        Vector results = new Vector();
        for (int i = 0; i < vipContacts.size(); i++) {
            Contact c = (Contact) vipContacts.elementAt(i);
            String name = c.name != null ? c.name.toLowerCase() : "";
            String number = c.number != null ? c.number : "";
            if (name.indexOf(lowerQuery) >= 0 || number.indexOf(lowerQuery) >= 0) {
                results.addElement(c);
            }
        }
        return results;
    }
    
    /**
     * Enregistre un contact dans le carnet d'adresses natif de BlackBerry via PIM (JSR 75)
     */
    public boolean saveContactToBlackBerryAddressBook(Contact c) {
        if (c == null) return false;
        try {
            javax.microedition.pim.PIM pim = javax.microedition.pim.PIM.getInstance();
            javax.microedition.pim.ContactList list = (javax.microedition.pim.ContactList) pim.openPIMList(javax.microedition.pim.PIM.CONTACT_LIST, javax.microedition.pim.PIM.READ_WRITE);
            javax.microedition.pim.Contact pimContact = list.createContact();
            
            if (c.name != null && c.name.length() > 0) {
                String[] nameArray = new String[list.stringArraySize(javax.microedition.pim.Contact.NAME)];
                nameArray[javax.microedition.pim.Contact.NAME_GIVEN] = c.name;
                pimContact.addStringArray(javax.microedition.pim.Contact.NAME, javax.microedition.pim.Contact.ATTR_NONE, nameArray);
            }
            if (c.number != null && c.number.length() > 0) {
                pimContact.addString(javax.microedition.pim.Contact.TEL, javax.microedition.pim.Contact.ATTR_MOBILE, c.number);
            }
            pimContact.commit();
            list.close();
            LogManager.log("CONTACTS", "Contact enregistré dans carnet natif: " + c.name);
            return true;
        } catch (Throwable t) {
            LogManager.error("CONTACTS", "Erreur PIM: " + t.getMessage());
            return false;
        }
    }
    
    public void searchContacts(String query) {
        // Envoi optionnel à l'Android pour recherche étendue si besoin
        if (app.getConnectionManager() != null) {
            app.getConnectionManager().sendData("CONTACT_SEARCH|" + (query != null ? query : "") + "\n");
        }
    }
    
    public void callContact(String number) {
        if (app.getConnectionManager() != null && number != null) {
            app.getConnectionManager().sendData("CALL_OUTBOUND|" + number + "\n");
        }
    }
    
    public Vector getContacts() {
        return vipContacts;
    }
}
