package com.hamza.blackberrybridge;

public class Contact {
    public String id;
    public String name;
    public String number;
    public boolean isVip;
    
    public Contact(String id, String name, String number) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.number = number != null ? number : "";
        this.isVip = true;
    }

    public Contact(String id, String name, String number, boolean isVip) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.number = number != null ? number : "";
        this.isVip = isVip;
    }
    
    public String toRecordString() {
        return id + "|" + name + "|" + number + "|" + (isVip ? "1" : "0");
    }

    public static Contact fromRecordString(String line) {
        if (line == null || line.length() == 0) return null;
        int p1 = line.indexOf('|');
        if (p1 < 0) return null;
        int p2 = line.indexOf('|', p1 + 1);
        if (p2 < 0) return null;
        int p3 = line.indexOf('|', p2 + 1);
        
        String id = line.substring(0, p1);
        String name = line.substring(p1 + 1, p2);
        String number = (p3 >= 0) ? line.substring(p2 + 1, p3) : line.substring(p2 + 1);
        boolean vip = true;
        if (p3 >= 0) {
            String vipStr = line.substring(p3 + 1);
            vip = vipStr.equals("1") || vipStr.equalsIgnoreCase("true");
        }
        return new Contact(id, name, number, vip);
    }
    
    public String toString() {
        return name + " (" + number + ")";
    }
}
