/*
 * Copyright (c) 2014 Wyatt Childers.
 *
 * All Rights Reserved
 */

package com.skelril.aurora.shard;

import com.sk89q.commandbook.util.entity.player.PlayerUtil;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.skelril.aurora.util.ChatUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static com.sk89q.commandbook.CommandBook.inst;
import static com.zachsthings.libcomponents.bukkit.BasePlugin.server;

@ComponentInformation(friendlyName = "Shard Instance Manager", desc = "Shard Instancing")
public class ShardManagerComponent extends BukkitComponent {

    private WorldGuardPlugin WG = WGBukkit.getPlugin();
    private ShardManager manager;
    private BukkitWorld shardWorld;

    @Override
    public void enable() {
        server().getScheduler().runTaskLater(inst(), () -> {
            shardWorld = new BukkitWorld(Bukkit.getWorld("Exemplar"));
            manager = new ShardManager(shardWorld, WG.getRegionManager(shardWorld.getWorld()));
        }, 1);
        registerCommands(Commands.class);
    }

    public ShardManager getManager() {
        return manager;
    }

    public World getShardWorld() {
        return shardWorld.getWorld();
    }

    public com.sk89q.worldedit.world.World getShardWEWorld() {
        return shardWorld;
    }

    public class Commands {

        private WorldEditPlugin getWE() {
            return (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
        }

        @Command(aliases = {"/relpos"},
                usage = "", desc = "Get your relative position to the minimum point of your selection",
                flags = "", min = 0, max = 0)
        public void relposCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            Selection selection = getWE().getSelection(player);
            if (selection == null) {
                throw new CommandException("You must first make a selection!");
            }
            Location offset = player.getLocation().subtract(selection.getMinimumPoint());
            ChatUtil.sendNotice(sender, "X: " + offset.getBlockX() + ", Y: " + offset.getBlockY() + ", Z: " + offset.getBlockZ());
        }
    }
}
