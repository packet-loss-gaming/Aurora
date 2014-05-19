/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.area.areas.DropParty;

import com.sk89q.worldedit.regions.CuboidRegion;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.checker.RegionChecker;
import com.skelril.aurora.util.timer.IntegratedRunnable;
import com.skelril.aurora.util.timer.TimedRunnable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.Iterator;
import java.util.List;

public class DropPartyTask {

    private TimedRunnable runnable;
    private World world;
    private CuboidRegion rg;
    private List<ItemStack> items;
    private RegionChecker checker;
    private int xpAmt = 0;
    private int xpSize = 0;

    public DropPartyTask(World world, CuboidRegion rg, List<ItemStack> items, RegionChecker checker) {
        this.world = world;
        this.rg = rg;
        this.items = items;
        this.checker = checker;
        this.runnable = new TimedRunnable(create(), (int) (items.size() * .15) + 1);
    }

    public void start(Plugin plugin, BukkitScheduler scheduler) {
        start(plugin, scheduler, 0, 20);
    }

    public void start(Plugin plugin, BukkitScheduler scheduler, long delay, long interval) {
        runnable.setTask(scheduler.runTaskTimer(plugin, runnable, delay, interval));
    }

    public World getWorld() {
        return world;
    }

    public void setXPChance(int amt) {
        xpAmt = amt;
    }

    public void setXPSize(int size) {
        xpSize = size;
    }

    private IntegratedRunnable create() {
        return new IntegratedRunnable() {
            @Override
            public boolean run(int times) {
                Iterator<ItemStack> it = items.iterator();

                for (int k = 10; it.hasNext() && k > 0; k--) {

                    // Pick a random Location
                    Location l = LocationUtil.pickLocation(world, rg.getMaximumY(), checker);
                    if (!world.getChunkAt(l).isLoaded()) world.getChunkAt(l).load(true);
                    world.dropItem(l, it.next());

                    // Remove the drop
                    it.remove();

                    // Drop the xp
                    if (xpAmt > 0) {
                        // Throw in some xp cause why not
                        for (short s = (short) ChanceUtil.getRandom(xpAmt); s > 0; s--) {
                            ExperienceOrb e = world.spawn(l, ExperienceOrb.class);
                            e.setExperience(xpSize);
                        }
                    }
                }

                // Cancel if we've ran out of drop party pulses or if there is nothing more to drop
                if (items.isEmpty()) {
                    runnable.cancel();
                }
                return true;
            }

            @Override
            public void end() {

            }
        };
    }
}
