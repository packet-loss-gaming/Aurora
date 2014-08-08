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
import com.skelril.aurora.events.PlayerInstanceDeathEvent;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.KeepAction;
import com.skelril.aurora.util.database.IOUtil;
import com.skelril.aurora.util.player.PlayerRespawnProfile_1_7_10;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.bukkit.BukkitComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import static com.sk89q.commandbook.CommandBook.*;
import static com.zachsthings.libcomponents.bukkit.BasePlugin.callEvent;
import static com.zachsthings.libcomponents.bukkit.BasePlugin.server;

@ComponentInformation(friendlyName = "Shard Instance Manager", desc = "Shard Instancing")
public class ShardManagerComponent extends BukkitComponent implements Listener {

    private HashMap<UUID, PlayerRespawnProfile_1_7_10> playerState = new HashMap<>();

    private WorldGuardPlugin WG = WGBukkit.getPlugin();
    private ShardManager manager;
    private BukkitWorld shardWorld;

    @Override
    public void enable() {
        server().getScheduler().runTaskLater(inst(), () -> {
            shardWorld = new BukkitWorld(Bukkit.getWorld("Exemplar"));
            manager = new ShardManager(shardWorld, WG.getRegionManager(shardWorld.getWorld()));
        }, 1);
        reloadData();
        server().getScheduler().runTaskTimer(inst(), this::writeData, 5, 5 * 20);
        registerCommands(Commands.class);
        registerEvents(this);
    }

    @Override
    public void disable() {
        super.disable();
        writeData();
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

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        World world = getShardWorld();
        if (!event.getPlayer().getWorld().equals(world)) return;
        Player player = event.getPlayer();
        if (WG.getRegionManager(world).getApplicableRegions(player.getLocation()).size() > 0) {
            leaveInstance(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = getShardWorld();
        if (!event.getEntity().getWorld().equals(world)) return;

        PlayerInstanceDeathEvent deathEvent = new PlayerInstanceDeathEvent(
                player,
                new PlayerRespawnProfile_1_7_10(
                        player,
                        event.getDroppedExp(),
                        KeepAction.KEEP,
                        KeepAction.KEEP,
                        KeepAction.KEEP,
                        KeepAction.KEEP
                )
        );

        callEvent(deathEvent);
        event.getDrops().clear();

        PlayerRespawnProfile_1_7_10 profile = deathEvent.getProfile();
        switch (profile.getArmorAction()) {
            case DROP:
                Collections.addAll(event.getDrops(), profile.getArmourContents());
                break;
        }

        switch (profile.getInvAction()) {
            case DROP:
                Collections.addAll(event.getDrops(), profile.getInventoryContents());
                break;
        }

        event.setDroppedExp((int) deathEvent.getProfile().getDroppedExp());

        playerState.put(player.getUniqueId(), deathEvent.getProfile());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getUniqueId())) {
            try {
                PlayerRespawnProfile_1_7_10 identity = playerState.get(player.getUniqueId());
                // Restore the contents
                if (identity.getInvAction() == KeepAction.KEEP) {
                    player.getInventory().setContents(identity.getInventoryContents());
                }
                if (identity.getArmorAction() == KeepAction.KEEP) {
                    player.getInventory().setArmorContents(identity.getArmourContents());
                }
                if (identity.getLevelAction() == KeepAction.KEEP) {
                    player.setLevel(identity.getLevel());
                }
                if (identity.getExperienceAction() == KeepAction.KEEP) {
                    player.setExp(identity.getExperience());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getUniqueId());
            }
        }
    }

    private File getWorkingDir() {
        return new File(inst().getDataFolder().getPath() + "/shards/");
    }

    public synchronized void writeData() {
        File playerStateFile = new File(getWorkingDir(), "/respawns.dat");
        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
            if (playerState.equals(playerStateFileO)) {
                return;
            }
        }
        IOUtil.toBinaryFile(getWorkingDir(), "respawns", playerState);
    }

    public synchronized void reloadData() {
        File playerStateFile = new File(getWorkingDir(), "/respawns.dat");
        if (playerStateFile.exists()) {
            Object playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
            if (playerStateFileO instanceof HashMap) {
                //noinspection unchecked
                playerState = (HashMap<UUID, PlayerRespawnProfile_1_7_10>) playerStateFileO;
                logger().info("Loaded: " + playerState.size() + " shard respawn records.");
            } else {
                logger().warning("Invalid respawn record file encountered: " + playerStateFile.getName() + "!");
                logger().warning("Attempting to use backup file...");
                playerStateFile = new File(getWorkingDir().getPath() + "/old-" + playerStateFile.getName());
                if (playerStateFile.exists()) {
                    playerStateFileO = IOUtil.readBinaryFile(playerStateFile);
                    if (playerStateFileO instanceof HashMap) {
                        //noinspection unchecked
                        playerState = (HashMap<UUID, PlayerRespawnProfile_1_7_10>) playerStateFileO;
                        logger().info("Backup file loaded successfully!");
                        logger().info("Loaded: " + playerState.size() + " shard respawn records.");
                    } else {
                        logger().warning("Backup file failed to load!");
                    }
                }
            }
        }
    }

    public void leaveInstance(Player player) {
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
    }

    public class Commands {

        @Command(aliases = {"leave"},
                usage = "", desc = "Leave an instance",
                flags = "", min = 0, max = 0)
        public void leaveCmd(CommandContext args, CommandSender sender) throws CommandException {
            Player player = PlayerUtil.checkPlayer(sender);
            if (!player.getWorld().equals(getShardWorld())) {
                throw new CommandException("You must be in an instance to use this command.");
            }
            leaveInstance(player);
            ChatUtil.sendNotice(player, "You've left the instance.");
        }

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
