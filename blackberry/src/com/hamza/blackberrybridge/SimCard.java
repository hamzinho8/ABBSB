package com.hamza.blackberrybridge;

/**
 * Représente une carte SIM détectée sur le smartphone Android distant.
 * Utilisé pour la gestion Double SIM et la sélection lors des appels sortants.
 */
public class SimCard {
    private String name;
    private int slot;

    public SimCard(String name, int slot) {
        this.name = (name != null && name.trim().length() > 0) ? name.trim() : "SIM " + (slot + 1);
        this.slot = slot;
    }

    public String getName() {
        return name;
    }

    public int getSlot() {
        return slot;
    }

    public String toString() {
        return name + " (Slot " + slot + ")";
    }
}
