/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.items.generic;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractXPArmor extends AbstractItemFeatureImpl {

    public abstract boolean hasArmor(Player player);

    public abstract int modifyXP(int startingAmt);

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onXPPickUp(PlayerExpChangeEvent event) {

        Player player = event.getPlayer();

        int origin = event.getAmount();
        int exp = modifyXP(origin);

        if (hasArmor(player)) {
            ItemStack[] armor = player.getInventory().getArmorContents();
            do {
                double ratio = 0;
                ItemStack is = null;
                for (ItemStack armorPiece : armor) {
                    double cRatio = (double) armorPiece.getDurability() / armorPiece.getType().getMaxDurability();
                    if (cRatio > ratio) {
                        ratio = cRatio;
                        is = armorPiece;
                    }
                }
                if (is == null) break;
                if (exp > is.getDurability()) {
                    exp -= is.getDurability();
                    is.setDurability((short) 0);
                } else {
                    is.setDurability((short) (is.getDurability() - exp));
                    exp = 0;
                }
            } while (exp > 0);
            player.getInventory().setArmorContents(armor);
            event.setAmount(Math.min(exp, origin));
        }
    }
}
