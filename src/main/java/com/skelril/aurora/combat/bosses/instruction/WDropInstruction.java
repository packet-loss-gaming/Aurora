/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.combat.bosses.instruction;

import com.skelril.aurora.modifier.ModifierType;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

import static com.skelril.aurora.modifier.ModifierComponent.getModifierManager;

public abstract class WDropInstruction extends DropInstruction {
    @Override
    public void dropItems(Location target, List<ItemStack> items) {
        if (getModifierManager().isActive(ModifierType.DOUBLE_WILD_DROPS)) {
            items.addAll(items.stream().map(ItemStack::clone).collect(Collectors.toList()));
        }
        super.dropItems(target, items);
    }
}
