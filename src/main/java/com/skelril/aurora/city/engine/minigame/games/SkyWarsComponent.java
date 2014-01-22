package com.skelril.aurora.city.engine.minigame.games;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.commandbook.session.PersistentSession;
import com.sk89q.commandbook.session.SessionComponent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.NestedCommand;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.patterns.SingleBlockPattern;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.anticheat.AntiCheatCompatibilityComponent;
import com.skelril.aurora.city.engine.minigame.MinigameComponent;
import com.skelril.aurora.city.engine.minigame.PlayerGameState;
import com.skelril.aurora.events.ServerShutdownEvent;
import com.skelril.aurora.events.anticheat.ThrowPlayerEvent;
import com.skelril.aurora.events.apocalypse.ApocalypseLocalSpawnEvent;
import com.skelril.aurora.exceptions.UnknownPluginException;
import com.skelril.aurora.prayer.PrayerComponent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.ChatUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.LocationUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.zachsthings.libcomponents.ComponentInformation;
import com.zachsthings.libcomponents.Depend;
import com.zachsthings.libcomponents.InjectComponent;
import com.zachsthings.libcomponents.config.ConfigurationBase;
import com.zachsthings.libcomponents.config.Setting;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Turtle9598
 */
@ComponentInformation(friendlyName = "Sky Wars", desc = "Sky warfare at it's best!")
@Depend(components = {AdminComponent.class, PrayerComponent.class}, plugins = {"WorldEdit", "WorldGuard"})
public class SkyWarsComponent extends MinigameComponent {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = CommandBook.logger();
    private final Server server = CommandBook.server();

    private ProtectedRegion region;
    private World world;
    private LocalConfiguration config;
    private short attempts = 0;

    @InjectComponent
    AdminComponent adminComponent;
    @InjectComponent
    AntiCheatCompatibilityComponent antiCheat;
    @InjectComponent
    SessionComponent sessions;

    public SkyWarsComponent() {
        super("Sky War", "sw");
    }

    @Override
    public void initialize(Set<Character> flags) {
        super.initialize(flags);

        Player[] players = getContainedPlayers();

        ChatUtil.sendNotice(players, "Get ready...");
    }

    @Override
    public void start() {
        super.start();

        Player[] players = getContainedPlayers();

        for (Player player : players) {

            server.getPluginManager().callEvent(new ThrowPlayerEvent(player));

            player.setVelocity(new Vector(0, 3.5, 0));

            sessions.getSession(SkyWarSession.class, player).stopPushBack();
        }

        editStartingPad(0, 0);

        ChatUtil.sendNotice(players, "Fight!");
    }

    // Player Management
    @Override
    public boolean addToTeam(Player player, int teamNumber, Set<Character> flags) {

        if (adminComponent.isAdmin(player) && !adminComponent.isSysop(player)) return false;

        super.addToTeam(player, teamNumber, flags);

        PlayerInventory playerInventory = player.getInventory();
        playerInventory.clear();

        List<ItemStack> gear = new ArrayList<>();

        gear.add(makeSkyFeather(-1, 2, 4));

        player.getInventory().addItem(gear.toArray(new ItemStack[gear.size()]));

        ItemStack[] leatherArmour = ItemUtil.leatherArmour;
        Color color = Color.WHITE;
        if (teamNumber == 2) color = Color.RED;
        else if (teamNumber == 1) color = Color.BLUE;

        LeatherArmorMeta helmMeta = (LeatherArmorMeta) leatherArmour[3].getItemMeta();
        helmMeta.setDisplayName(ChatColor.WHITE + "Sky Hood");
        helmMeta.setColor(color);
        leatherArmour[3].setItemMeta(helmMeta);

        LeatherArmorMeta chestMeta = (LeatherArmorMeta) leatherArmour[2].getItemMeta();
        chestMeta.setDisplayName(ChatColor.WHITE + "Sky Plate");
        chestMeta.setColor(color);
        leatherArmour[2].setItemMeta(chestMeta);

        LeatherArmorMeta legMeta = (LeatherArmorMeta) leatherArmour[1].getItemMeta();
        legMeta.setDisplayName(ChatColor.WHITE + "Sky Leggings");
        legMeta.setColor(color);
        leatherArmour[1].setItemMeta(legMeta);

        LeatherArmorMeta bootMeta = (LeatherArmorMeta) leatherArmour[0].getItemMeta();
        bootMeta.setDisplayName(ChatColor.WHITE + "Sky Boots");
        bootMeta.setColor(color);
        leatherArmour[0].setItemMeta(bootMeta);

        playerInventory.setArmorContents(leatherArmour);

        Location battleLoc = new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z);

        if (battleLoc.getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR)) {
            edit(BlockID.STAINED_GLASS, 15, battleLoc);
        }

        if (player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        player.sendMessage(ChatColor.YELLOW + "You have joined the Sky War.");
        return player.teleport(battleLoc);
    }

    private void editStartingPad(int toType, int toData) {

        edit(toType, toData, new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z));
    }

    private void edit(int toType, int toData, Location battleLoc) {

        battleLoc = battleLoc.clone().add(0, -1, 0);

        EditSession editor = new EditSession(new BukkitWorld(battleLoc.getWorld()), -1);
        com.sk89q.worldedit.Vector origin = new com.sk89q.worldedit.Vector(
                battleLoc.getX(), battleLoc.getY(), battleLoc.getZ()
        );
        Pattern pattern = new SingleBlockPattern(new BaseBlock(toType, toData));
        try {

            editor.makeCylinder(origin, pattern, 12, 1, true);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
    }

    private void awardPowerup(Player player) {

        ItemStack feather = makeSkyFeather(ChanceUtil.getRandom(5), ChanceUtil.getRandom(12), ChanceUtil.getRandom(12));
        player.getInventory().addItem(feather);
        //noinspection deprecation
        player.updateInventory();

        // Display name doesn't need checked as all sky feathers have one assigned
        ChatUtil.sendNotice(player, "You obtain a power-up: "
                + feather.getItemMeta().getDisplayName() + ChatColor.YELLOW + "!");
    }

    private void decrementUses(final Player player, ItemStack itemStack, int uses, double flight, double pushBack) {

        if (uses == -1) return;

        uses--;

        final ItemStack remainder;
        if (itemStack.getAmount() > 1) {
            remainder = itemStack.clone();
            remainder.setAmount(remainder.getAmount() - 1);
        } else {
            remainder = null;
        }

        final ItemStack newSkyFeather;
        if (uses < 1) {
            newSkyFeather = null;
        } else {
            newSkyFeather = modifySkyFeather(itemStack, uses, flight, pushBack);
            newSkyFeather.setAmount(1);
        }

        server.getScheduler().runTaskLater(inst, new Runnable() {
            @Override
            public void run() {
                if (newSkyFeather == null) {
                    player.getInventory().setItemInHand(null);
                }
                if (remainder != null) {
                    player.getInventory().addItem(remainder);
                }
                //noinspection deprecation
                player.updateInventory();
            }
        }, 1);
    }

    private ItemStack makeSkyFeather(int uses, double flight, double pushBack) {

        return modifySkyFeather(new ItemStack(ItemID.FEATHER), uses, flight, pushBack);
    }

    private ItemStack modifySkyFeather(ItemStack skyFeather, int uses, double flight, double pushBack) {
        ItemMeta skyMeta = skyFeather.getItemMeta();

        String suffix;

        if (uses == -1) {
            suffix = "Infinite";
        } else {
            if (flight == pushBack) {
                suffix = "Balance";
            } else if (flight > pushBack) {
                suffix = "Flight";
            } else {
                suffix = "Push Back";
            }
        }

        skyMeta.setDisplayName(ChatColor.AQUA + "Sky Feather [" + suffix + "]");
        skyMeta.setLore(Arrays.asList(
                ChatColor.GOLD + "Uses: " + (uses != -1 ? uses : "Infinite"),
                ChatColor.GOLD + "Flight: " + flight,
                ChatColor.GOLD + "Push Back: " + pushBack
        ));
        skyFeather.setItemMeta(skyMeta);
        return skyFeather;
    }

    @Override
    public void checkTeam(int teamNumber) throws CommandException {

        if (teamNumber != 0) {
            throw new CommandException("You can only join team 0.");
        }
    }

    @Override
    public void printFlags() {

        Player[] players = getContainedPlayers();

        ChatUtil.sendNotice(players, ChatColor.GREEN + "The following flags are enabled: ");

        if (gameFlags.contains('q')) ChatUtil.sendNotice(players, ChatColor.GOLD, "Quick start");
    }

    @Override
    public void restore(Player player, PlayerGameState state) {

        super.restore(player, state);
        player.setFallDistance(0);
    }

    @Override
    public Player[] getContainedPlayers() {

        return getContainedPlayers(0);
    }

    public Player[] getContainedPlayers(int parentsUp) {

        List<Player> returnedList = new ArrayList<>();
        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        for (Player player : server.getOnlinePlayers()) {

            if (LocationUtil.isInRegion(world, r, player)) returnedList.add(player);
        }
        return returnedList.toArray(new Player[returnedList.size()]);
    }

    public Entity[] getContainedEntities(Class<?>... classes) {

        return getContainedEntities(0, classes);
    }

    public Entity[] getContainedEntities(int parentsUp, Class<?>... classes) {

        List<Entity> returnedList = new ArrayList<>();

        ProtectedRegion r = region;
        for (int i = parentsUp; i > 0; i--) r = r.getParent();

        for (Entity entity : world.getEntitiesByClasses(classes)) {

            if (entity.isValid() && LocationUtil.isInRegion(r, entity)) returnedList.add(entity);
        }
        return returnedList.toArray(new Entity[returnedList.size()]);
    }

    public boolean contains(Location location) {

        return LocationUtil.isInRegion(world, region, location);
    }

    @Override
    public boolean probe() {

        world = Bukkit.getWorld(config.worldName);
        try {
            region = getWorldGuard().getGlobalRegionManager().get(world).getRegion(config.region);
        } catch (UnknownPluginException | NullPointerException e) {
            if (attempts > 10) {
                e.printStackTrace();
                return false;
            }
            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {

                    attempts++;
                    probe();
                }
            }, 2);
        }

        return world != null && region != null;
    }

    @Override
    public void enable() {

        super.enable();

        config = configure(new LocalConfiguration());

        probe();
        //noinspection AccessStaticViaInstance
        inst.registerEvents(new SkyWarsListener());
        registerCommands(Commands.class);
        server.getScheduler().scheduleSyncRepeatingTask(inst, this, 20 * 2, 10);
    }

    @Override
    public void reload() {

        super.reload();
        configure(config);
        probe();
    }

    @Override
    public void run() {

        try {
            if (playerState.size() == 0 && !isGameInitialised()) return;

            // Damage players & kill missing players
            for (PlayerGameState entry : playerState.values()) {
                try {
                    Player player = Bukkit.getPlayerExact(entry.getOwnerName());
                    if (player == null || !player.isValid()) {
                        continue;
                    }
                    Location pLoc = player.getLocation();
                    if (contains(pLoc)) {
                        player.setFoodLevel(20);
                        player.setSaturation(5F);
                        if (EnvironmentUtil.isWater(pLoc.getBlock())) {
                            if (isGameActive()) {
                                player.damage(ChanceUtil.getRandom(3));
                            } else {
                                player.teleport(new Location(Bukkit.getWorld(config.worldName), config.x, config.y, config.z));
                            }
                        }
                        continue;
                    }
                    player.setHealth(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!isGameInitialised()) return;

            for (int i = 0; i < playerState.size(); i++) {

                if (!ChanceUtil.getChance(10)) continue;

                Chicken c = (Chicken) world.spawnEntity(
                        LocationUtil.pickLocation(world, region.getMaximumPoint().getY() - 10, region), EntityType.CHICKEN);
                c.setRemoveWhenFarAway(true);
            }

            // Security
            for (Player player : getContainedPlayers()) {

                if (!player.isValid()) continue;

                if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
                    if (player.isFlying()) {
                        player.setAllowFlight(true);
                        player.setFlying(true);
                        player.setGameMode(GameMode.SURVIVAL);
                    } else player.setGameMode(GameMode.SURVIVAL);
                }
            }

            if (!isGameActive()) return;

            // Team Counter
            int teamZero = 0;
            int teamOne = 0;
            int teamTwo = 0;
            for (PlayerGameState entry : playerState.values()) {
                try {
                    Player teamPlayer = Bukkit.getPlayerExact(entry.getOwnerName());

                    adminComponent.standardizePlayer(teamPlayer);
                    switch (entry.getTeamNumber()) {
                        case 0:
                            teamZero++;
                            break;
                        case 1:
                            teamOne++;
                            break;
                        case 2:
                            teamTwo++;
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Win Machine
            if (teamOne > 0 || teamTwo > 0 || teamZero > 0) {
                String winner;
                if (teamOne >= 1) {
                    if (teamTwo >= 1 || teamZero >= 1) return;
                    else winner = "Team one";
                } else if (teamTwo >= 1) {
                    if (teamOne >= 1 || teamZero >= 1) return;
                    else winner = "Team two";
                } else {
                    if (teamZero > 1) return;
                    else winner = Lists.newArrayList(playerState.values()).get(0).getOwnerName();
                }
                Bukkit.broadcastMessage(ChatColor.GOLD + winner + " has won!");
            } else {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Tie game!");
            }

            end();
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.broadcastMessage(ChatColor.RED + "[WARNING] Sky Wars logic failed to process.");
        }
    }

    private static class LocalConfiguration extends ConfigurationBase {

        @Setting("sky-wars-start-World")
        public String worldName = "City";
        @Setting("sky-wars--start-X")
        public int x = 631;
        @Setting("sky-wars-start-Y")
        public int y = 81;
        @Setting("sky-wars-start-Z")
        public int z = 205;
        @Setting("sky-wars-region")
        public String region = "vineam-district-sky-wars";

    }

    private class SkyWarsListener implements Listener {

        private final String[] cmdWhiteList = new String[]{
                "skywar", "sw", "stopweather", "me", "say", "pm", "msg", "message", "whisper", "tell",
                "reply", "r", "mute", "unmute", "debug", "dropclear", "dc", "auth", "toggleeditwand"
        };

        @EventHandler(ignoreCancelled = true)
        public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {

            Player player = event.getPlayer();
            if (getTeam(player) != -1) {
                String command = event.getMessage();
                boolean allowed = false;
                for (String cmd : cmdWhiteList) {
                    if (command.toLowerCase().startsWith("/" + cmd)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    ChatUtil.sendError(player, "Command blocked.");
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void onShutDownEvent(ServerShutdownEvent event) {

            end();
        }

        @EventHandler(ignoreCancelled = true)
        public void onItemDrop(PlayerDropItemEvent event) {

            if (getTeam(event.getPlayer()) != -1 && !isGameActive()) event.setCancelled(true);
        }

        @EventHandler
        public void onClick(PlayerInteractEvent event) {

            final Player player = event.getPlayer();
            ItemStack stack = player.getItemInHand();

            if (!isGameActive()) return;

            if (getTeam(player) != -1 && ItemUtil.matchesFilter(stack, ChatColor.AQUA + "Sky Feather")) {
                SkyWarSession session = sessions.getSession(SkyWarSession.class, player);
                Vector vel = player.getLocation().getDirection();

                int uses = -1;
                double flight = 2;
                double pushBack = 4;

                if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                    for (String line : stack.getItemMeta().getLore()) {
                        String[] args = line.split(":");
                        if (args.length < 2) continue;

                        for (int i = 0; i < args.length; i++) {
                            args[i] = args[i].trim();
                        }
                        if (args[0].endsWith("Uses")) {
                            try {
                                uses = Integer.parseInt(args[args.length - 1]);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        if (args[0].endsWith("Flight")) {
                            try {
                                flight = Double.parseDouble(args[args.length - 1]);
                            } catch (NumberFormatException ignored) {
                            }
                        }

                        if (args[0].endsWith("Push Back")) {
                            try {
                                pushBack = Double.parseDouble(args[args.length - 1]);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                switch (event.getAction()) {
                    case LEFT_CLICK_AIR:

                        if (!session.canFly()) break;

                        vel.multiply(flight);

                        server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                        player.setVelocity(vel);

                        session.stopFlight(250);

                        decrementUses(player, stack, uses, flight, pushBack);
                        break;
                    case RIGHT_CLICK_AIR:

                        if (!session.canPushBack()) break;

                        vel.multiply(pushBack);

                        BlockIterator it = new BlockIterator(player, 50);
                        Location k = new Location(null, 0, 0, 0);

                        Entity[] targets = getContainedEntities(Chicken.class, Player.class);

                        while (it.hasNext()) {
                            Block block = it.next();

                            block.getWorld().playEffect(block.getLocation(k), Effect.MOBSPAWNER_FLAMES, 0);

                            for (Entity aEntity : targets) {
                                innerLoop:
                                {
                                    if (!aEntity.isValid() || aEntity.equals(player)) break innerLoop;

                                    if (aEntity.getLocation().distanceSquared(block.getLocation()) <= 12) {
                                        if (aEntity instanceof Player) {
                                            Player aPlayer = (Player) aEntity;

                                            if (isFriendlyFire(player, aPlayer)) break innerLoop;

                                            // Handle Sender
                                            session.stopPushBack(250);
                                            ChatUtil.sendNotice(player, "You push back: " + aPlayer.getName() + "!");

                                            // Handle Target
                                            server.getPluginManager().callEvent(new ThrowPlayerEvent(aPlayer));
                                            aPlayer.setVelocity(vel);

                                            sessions.getSession(SkyWarSession.class, aPlayer).stopFlight();
                                        } else {
                                            awardPowerup(player);
                                            aEntity.remove();
                                        }
                                    }
                                }
                            }
                        }
                        decrementUses(player, stack, uses, flight, pushBack);
                        break;
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onEntityDamageEvent(EntityDamageEvent event) {

            Entity e = event.getEntity();

            if (!(e instanceof Player)) return;

            Player player = (Player) e;

            if (getTeam(player) != -1) {

                if (!isGameActive()) {
                    event.setCancelled(true);

                    if (event instanceof EntityDamageByEntityEvent) {
                        Entity attacker = ((EntityDamageByEntityEvent) event).getDamager();
                        if (attacker instanceof Projectile) {
                            attacker = ((Projectile) attacker).getShooter();
                        }
                        if (!(attacker instanceof Player)) return;
                        ChatUtil.sendError((Player) attacker, "The game has not yet started!");
                    }
                } else if (event.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                    event.setCancelled(true);
                }
            } else if (contains(player.getLocation())) {
                player.teleport(player.getWorld().getSpawnLocation());
                event.setCancelled(true);
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

            Entity attackingEntity = event.getDamager();
            Entity defendingEntity = event.getEntity();

            if (!(defendingEntity instanceof Player)) return;
            Player defendingPlayer = (Player) defendingEntity;

            Player attackingPlayer;
            if (attackingEntity instanceof Player) {
                attackingPlayer = (Player) attackingEntity;
            } else if (attackingEntity instanceof Arrow) {
                if (!(((Arrow) attackingEntity).getShooter() instanceof Player)) return;
                attackingPlayer = (Player) ((Arrow) attackingEntity).getShooter();
            } else {
                return;
            }

            if (getTeam(attackingPlayer) == -1 && getTeam(defendingPlayer) != -1) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't attack participants.");
                return;
            }

            if (getTeam(attackingPlayer) == -1) return;
            if (getTeam(defendingPlayer) == -1) {
                ChatUtil.sendWarning(attackingPlayer, "Don't attack bystanders.");
                return;
            }

            if (isFriendlyFire(attackingPlayer, defendingPlayer)) {
                event.setCancelled(true);
                ChatUtil.sendWarning(attackingPlayer, "Don't hit your team mates!");
            } else {
                ChatUtil.sendNotice(attackingPlayer, "You've hit " + defendingPlayer.getName() + "!");
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerDeath(PlayerDeathEvent event) {

            final Player player = event.getEntity();
            if (getTeam(player) != -1) {
                Player killer = player.getKiller();
                if (killer != null) {
                    event.setDeathMessage(player.getName() + " has been taken out by " + killer.getName());
                } else {
                    event.setDeathMessage(player.getName() + " is out");
                }
                event.getDrops().clear();
                event.setDroppedExp(0);

                left(player);
            }
        }

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {

            final Player p = event.getPlayer();

            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {
                    // Technically forced, but because this
                    // happens from disconnect/quit button
                    // we don't want it to count as forced
                    removeGoneFromTeam(p, false);
                }
            }, 1);
        }

        @EventHandler(ignoreCancelled = true)
        public void onPlayerRespawn(PlayerRespawnEvent event) {

            final Player p = event.getPlayer();

            server.getScheduler().runTaskLater(inst, new Runnable() {

                @Override
                public void run() {
                    removeGoneFromTeam(p, true);
                }
            }, 1);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {

            Player player = event.getPlayer();

            if (getTeam(player) != -1) left(event.getPlayer());
        }

        @EventHandler
        public void onZombieLocalSpawn(ApocalypseLocalSpawnEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }

        @EventHandler
        public void onKick(PlayerKickEvent event) {

            if (getTeam(event.getPlayer()) != -1) event.setCancelled(true);
        }
    }

    public class Commands {

        @Command(aliases = {"skywars", "sw"}, desc = "Sky wars commands")
        @NestedCommand({NestedCommands.class})
        public void skyWarCmds(CommandContext args, CommandSender sender) throws CommandException {

        }
    }

    public class NestedCommands {

        @Command(aliases = {"join", "j"},
                usage = "[Player] [Team Number]", desc = "Join the Minigame",
                anyFlags = true, min = 0, max = 2)
        public void joinSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            joinCmd(args, sender);
        }

        @Command(aliases = {"leave", "l"},
                usage = "[Player]", desc = "Leave the Minigame",
                min = 0, max = 1)
        public void leaveSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            leaveCmd(args, sender);
        }

        @Command(aliases = {"reset", "r"}, desc = "Reset the Minigame.",
                flags = "p",
                min = 0, max = 0)
        public void resetSkyWarCmd(CommandContext args, CommandSender sender) throws CommandException {

            resetCmd(args, sender);
        }

        @Command(aliases = {"start", "s"},
                usage = "", desc = "Minigame start command",
                anyFlags = true, min = 0, max = 0)
        public void startSkywarCmd(CommandContext args, CommandSender sender) throws CommandException {

            startCmd(args, sender);
        }
    }

    private WorldGuardPlugin getWorldGuard() throws UnknownPluginException {

        Plugin plugin = server.getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            throw new UnknownPluginException("WorldGuard");
        }

        return (WorldGuardPlugin) plugin;
    }

    // Sky War Session
    private static class SkyWarSession extends PersistentSession {

        public static final long MAX_AGE = TimeUnit.DAYS.toMillis(1);

        private long nextFlight = 0;
        private long nextPushBack = 0;

        protected SkyWarSession() {

            super(MAX_AGE);
        }

        public boolean canFly() {

            return nextFlight == 0 || System.currentTimeMillis() >= nextFlight;
        }

        public void stopFlight() {

            stopFlight(2250);
        }

        public void stopFlight(long time) {

            nextFlight = System.currentTimeMillis() + time;
        }

        public boolean canPushBack() {

            return nextPushBack == 0 || System.currentTimeMillis() >= nextPushBack;
        }

        public void stopPushBack() {

            stopPushBack(5000);
        }

        public void stopPushBack(long time) {

            nextPushBack = System.currentTimeMillis() + time;
        }

        public Player getPlayer() {

            CommandSender sender = super.getOwner();
            return sender instanceof Player ? (Player) sender : null;
        }
    }
}