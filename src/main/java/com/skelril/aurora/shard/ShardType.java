/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.shard;

import org.bukkit.ChatColor;

public enum ShardType {

    // Bosses
    SHNUGGLES_PRIME(ChatColor.BLUE, "Shnuggles Prime"),
    PATIENT_X(ChatColor.DARK_RED, "Patient X");

    private ChatColor color;
    private String name;

    private ShardType(ChatColor color, String name) {

        this.color = color;
        this.name = name;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public String getColoredName() {
        return color + name;
    }

    @Override
    public String toString() {
        return color + name;
    }

    public static ShardType matchFrom(String string) {
        for (ShardType type : values()) {
            if (type.getColoredName().equals(string)) {
                return type;
            }
        }
        return null;
    }
}