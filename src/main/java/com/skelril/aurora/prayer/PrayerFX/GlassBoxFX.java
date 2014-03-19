/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.prayer.PrayerFX;

import com.sk89q.worldedit.blocks.BlockID;
import com.skelril.aurora.prayer.PrayerType;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GlassBoxFX extends AbstractEffect {

    @Override
    public PrayerType getType() {

        return PrayerType.GLASSBOX;
    }

    @Override
    public void add(Player player) {

        LocationUtil.toGround(player);

        List<Location> locationList = new ArrayList<>();
        List<Location> queList = new ArrayList<>();
        final Location loc1, loc2;
        loc1 = player.getLocation();
        loc2 = player.getEyeLocation();
        locationList.add(loc1);
        locationList.add(loc2);
        for (Location loc : locationList) {
            for (BlockFace face : EnvironmentUtil.getNearbyBlockFaces()) {
                queList.add(loc.getBlock().getRelative(face).getLocation());
            }
        }
        locationList.addAll(queList.stream().map(loc -> loc).collect(Collectors.toList()));
        for (Location loc : locationList) {
            if (loc.getBlock().equals(loc1.getBlock()) || loc.getBlock().equals(loc2.getBlock())) continue;
            player.sendBlockChange(loc, BlockID.GLASS, (byte) 0);
        }
    }

    @Override
    public void clean(Player player) {

        // Nothing to do here
    }
}
