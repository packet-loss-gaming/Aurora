/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.util.player;

import com.google.common.collect.Lists;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Author: Turtle9598
 */
public class GeneralPlayerUtil {

    /**
     * Make a player state
     */
    public static PlayerState makeComplexState(Player player) {

        return new PlayerState(player.getUniqueId().toString(),
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getExhaustion(),
                player.getLevel(),
                player.getExp());
    }

    public static void findSafeSpot(Player player) {

        Location toBlock = LocationUtil.findFreePosition(player.getLocation());

        if (toBlock != null && player.teleport(toBlock)) {
            return;
        } else {
            toBlock = player.getLocation();
        }

        Location working = toBlock.clone();

        List<BlockFace> nearbyBlockFaces = Lists.newArrayList(EnvironmentUtil.getNearbyBlockFaces());
        nearbyBlockFaces.remove(BlockFace.SELF);
        Collections.shuffle(nearbyBlockFaces);

        boolean done = false;

        for (int i = 1; i < 10 && !done; i++) {
            for (BlockFace face : nearbyBlockFaces) {
                working = LocationUtil.findFreePosition(toBlock.getBlock().getRelative(face, i).getLocation(working));

                if (working == null) {
                    working = toBlock.clone();
                    continue;
                }

                done = player.teleport(working);
            }
        }

        if (!done) {
            player.teleport(player.getWorld().getSpawnLocation());
            ChatUtil.sendError(player, "Failed to locate a safe location, teleporting to spawn!");
        }
    }

    /**
     * This method is used to hide a player
     *
     * @param player - The player to hide
     * @param to     - The player who can no longer see the player
     * @return - true if change occurred
     */
    public static boolean hide(Player player, Player to) {

        if (to.canSee(player)) {
            to.hidePlayer(player);
            return true;
        }
        return false;
    }

    /**
     * This method is used to show a player
     *
     * @param player - The player to show
     * @param to     - The player who can now see the player
     * @return - true if change occurred
     */
    public static boolean show(Player player, Player to) {

        if (!to.canSee(player)) {
            to.showPlayer(player);
            return true;
        }
        return false;
    }
}
