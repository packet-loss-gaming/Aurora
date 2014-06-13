/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.city.engine.arena.factory;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.util.ChatUtil;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FactorySmelter extends FactoryMech {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private LavaSupply lavaSupply;

    private static final List<Integer> wanted = new ArrayList<>();

    static {
        wanted.add(BlockID.IRON_ORE);
        wanted.add(BlockID.GOLD_ORE);
    }

    public FactorySmelter(World world, ProtectedRegion region, ProtectedRegion lavaSupply, ProtectedRegion lavaZone) {
        super(world, region);
        this.lavaSupply = new LavaSupply(world, lavaSupply, lavaZone);
    }

    @Override
    public List<ItemStack> process() {
        Player[] playerList = getContainedPlayers(1);

        Entity[] lavaContained = lavaSupply.getContainedEntities();
        if (lavaContained.length > 0) ChatUtil.sendNotice(playerList, "Adding lava...");
        int totalLava = items.containsKey(ItemID.LAVA_BUCKET) ? items.get(ItemID.LAVA_BUCKET) : 0;
        for (Entity e : lavaContained) {
            // Kill contained living entities
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).setHealth(0);
                continue;
            }

            // Find items and destroy those unwanted
            if (e instanceof Item) {

                ItemStack workingStack = ((Item) e).getItemStack();

                // Add the item to the list
                if (workingStack.getType().equals(Material.LAVA_BUCKET)) {
                    int total = workingStack.getAmount();
                    if (items.containsKey(workingStack.getTypeId())) {
                        total += items.get(workingStack.getTypeId());
                    }
                    items.put(workingStack.getTypeId(), totalLava = total);
                }
            }
            e.remove();
        }
        items.put(ItemID.LAVA_BUCKET, lavaSupply.addLava(totalLava));

        Entity[] contained = getContainedEntities();
        if (contained.length > 0) ChatUtil.sendNotice(playerList, "Processing...");

        for (Entity e : contained) {

            // Kill contained living entities
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).setHealth(0);
                continue;
            }

            // Find items and destroy those unwanted
            if (e instanceof Item) {

                ItemStack workingStack = ((Item) e).getItemStack();

                // Add the item to the list
                if (wanted.contains(workingStack.getTypeId())) {
                    int total = workingStack.getAmount();
                    ChatUtil.sendNotice(playerList, "Found: " + total + " " + workingStack.getType().toString() + ".");
                    if (items.containsKey(workingStack.getTypeId())) {
                        total += items.get(workingStack.getTypeId());
                    }
                    items.put(workingStack.getTypeId(), total);
                }
            }
            e.remove();
        }

        int maxIron = items.containsKey(BlockID.IRON_ORE) ? items.get(BlockID.IRON_ORE) : 0;
        int maxGold = items.containsKey(BlockID.GOLD_ORE) ? items.get(BlockID.GOLD_ORE) : 0;

        if (maxGold + maxIron < 1) return new ArrayList<>();

        int requestedLava = Math.max(1, Math.max(maxIron, maxGold) / 8);
        int availableLava = lavaSupply.removeLava(requestedLava);

        int ironRemainder = maxIron - (availableLava * 8);
        int goldRemainder = maxGold - (availableLava * 8);

        if (ironRemainder < 1) {
            items.remove(BlockID.IRON_ORE);
        } else {
            items.put(BlockID.IRON_ORE, ironRemainder);
        }
        if (goldRemainder < 1) {
            items.remove(BlockID.GOLD_ORE);
        } else {
            items.put(BlockID.GOLD_ORE, goldRemainder);
        }

        if (availableLava < requestedLava) {
            if (maxIron > 0) maxIron = maxIron - ironRemainder;
            if (maxGold > 0) maxGold = maxGold - goldRemainder;
        }

        maxIron *= 8;
        maxGold *= 8;

        // Tell the player what we are making
        if (maxIron > 0) {
            ChatUtil.sendNotice(playerList, "Smelting: "  + maxIron + " iron ingots.");
        }
        if (maxGold > 0) {
            ChatUtil.sendNotice(playerList, "Smelting: " + maxGold + " gold ingots.");
        }
        // Return the product for the que
        List<ItemStack> product = new ArrayList<>();
        for (int i = maxIron; i > 0; --i) product.add(new ItemStack(ItemID.IRON_BAR));
        for (int i = maxGold; i > 0; --i) product.add(new ItemStack(ItemID.GOLD_BAR));
        return product;
    }
}