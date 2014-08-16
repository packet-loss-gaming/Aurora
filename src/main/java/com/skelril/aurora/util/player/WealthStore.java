/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.player;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Author: Turtle9598
 */
public class WealthStore extends GenericWealthStore {

    public WealthStore(String ownerName, ItemStack[] inventoryContents) {

        super(ownerName, inventoryContents);
    }

    public WealthStore(String ownerName, ItemStack[] inventoryContents, ItemStack[] armorContents) {

        super(ownerName, inventoryContents, armorContents);
    }

    public WealthStore(String ownerName, List<ItemStack> itemStacks) {

        super(ownerName, itemStacks);
    }

    public WealthStore(String ownerName, List<ItemStack> itemStacks, int value) {

        super(ownerName, itemStacks, value);
    }

    public WealthStore(String ownerName, int value) {

        super(ownerName, value);
    }
}
