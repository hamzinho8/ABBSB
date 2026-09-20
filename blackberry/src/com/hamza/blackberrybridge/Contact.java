package com.hamza.blackberrybridge;

public class Contact {
    public String id;
    public String name;
    public String number;
    
    public Contact(String id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }
    
    public String toString() {
        return name + " (" + number + ")";
    }
}
