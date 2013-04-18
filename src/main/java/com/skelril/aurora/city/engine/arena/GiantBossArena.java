package com.skelril.aurora.city.engine.arena;

import com.google.common.collect.Lists;
import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.SacrificeComponent;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.events.CreepSpeakEvent;
import com.skelril.aurora.events.PrayerApplicationEvent;
import com.skelril.aurora.events.ThrowPlayerEvent;
import com.skelril.aurora.util.*;
import com.skelril.aurora.util.player.PlayerState;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Author: Turtle9598
 */
public class GiantBossArena extends AbstractRegionedArena implements BossArena, Listener {

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    private AdminComponent adminComponent;

    private static final int groundLevel = 82;
    private static final double scalOffst = 4;

    private Giant boss = null;
    private long lastAttack = 0;
    private long lastDeath = 0;
    private boolean damageHeals = false;
    private BukkitTask mobDestroyer;
    private Random random = new Random();

    private int difficulty = Difficulty.HARD.getValue();
    private List<Location> spawnPts = new ArrayList<>();
    private final HashMap<String, PlayerState> playerState = new HashMap<>();

    public GiantBossArena(World world, ProtectedRegion region, AdminComponent adminComponent) {

        super(world, region);
        this.adminComponent = adminComponent;

        //noinspection AccessStaticViaInstance
        inst.registerEvents(this);

        // Mob Destroyer
        mobDestroyer = server.getScheduler().runTaskTimer(inst, new Runnable() {

            @Override
            public void run() {

                Entity[] contained = getContainedEntities(1);
                if (!getWorld().isThundering()) removeOutsideZombies(contained);
                if (isBossSpawned()) removeXP(contained);
            }
        }, 0, 20 * 2);

        // First spawn requirement
        getMinionSpawnPts();

        // Set difficulty
        difficulty = getWorld().getDifficulty().getValue();
    }

    @Override
    public boolean isBossSpawned() {

        if (!isArenaLoaded()) return true;
        boolean found = false;
        for (Entity e : getContainedEntities()) {

            if (e.isValid() && e instanceof Giant) {
                if (!found) {
                    boss = (Giant) e;
                    found = true;
                } else e.remove();
            }
        }
        return boss != null && boss.isValid();
    }

    public boolean isArenaLoaded() {

        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        Region region = new CuboidRegion(min, max);
        return BukkitUtil.toLocation(getWorld(), region.getCenter()).getChunk().isLoaded();
    }

    @Override
    public void spawnBoss() {

        spawnPts.clear();

        BlockVector min = getRegion().getMinimumPoint();
        BlockVector max = getRegion().getMaximumPoint();

        getMinionSpawnPts(min, max);

        Region region = new CuboidRegion(min, max);
        Location l = BukkitUtil.toLocation(getWorld(), region.getCenter().setY(groundLevel));
        boss = (Giant) getWorld().spawnEntity(l, EntityType.GIANT);
        boss.setMaxHealth(510 + (difficulty * 80));
        boss.setHealth(510 + (difficulty * 80));
        boss.setRemoveWhenFarAway(false);

        for (Player player : getContainedPlayers(1)) ChatUtil.sendWarning(player, "I live again!");
    }

    public void getMinionSpawnPts() {

        getMinionSpawnPts(getRegion().getMinimumPoint(), getRegion().getMaximumPoint());
    }

    public void getMinionSpawnPts(BlockVector min, BlockVector max) {

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int minY = min.getBlockY();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();
        int maxY = max.getBlockY();

        BlockState block;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = maxY; y >= minY; --y) {
                    block = getWorld().getBlockAt(x, y, z).getState();
                    if (!block.getChunk().isLoaded()) block.getChunk().load();
                    if (block.getTypeId() == BlockID.GOLD_BLOCK) {
                        spawnPts.add(block.getLocation().add(0, 2, 0));
                    }
                }
            }
        }
    }

    @Override
    public LivingEntity getBoss() {

        return boss;
    }

    @Override
    public void forceRestoreBlocks() {

        // Nothing to do here... YET!
    }

    @Override
    public void run() {

        if (!isBossSpawned() && (lastDeath == 0 || System.currentTimeMillis() - lastDeath >= 1000 * 60 * 3)) {
            removeMobs();
            spawnBoss();
        } else if (!isEmpty()) {
            equalize();
            runAttack(ChanceUtil.getRandom(OPTION_COUNT));
        }
    }

    public Runnable spawnXP = new Runnable() {
        @Override
        public void run() {
            for (Location pt : spawnPts) {
                if (!ChanceUtil.getChance(6)) continue;
                ThrownExpBottle bottle = (ThrownExpBottle) getWorld().spawnEntity(pt, EntityType.THROWN_EXP_BOTTLE);
                bottle.setVelocity(new Vector(
                        random.nextDouble() * 1.7 - 1.5,
                        random.nextDouble() * 1.5,
                        random.nextDouble() * 1.7 - 1.5)
                );
            }
        }
    };

    public void removeXP(Entity[] contained) {

        for (Entity e : contained) {

            if (e instanceof ExperienceOrb && e.getTicksLived() > 20 * 13) e.remove();
        }
    }

    public void removeMobs() {

        for (Entity e : getContainedEntities(1)) {

            if (EnvironmentUtil.isHostileEntity(e)) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                e.remove();
            }
        }
    }

    public void removeOutsideZombies(Entity[] contained) {

        for (Entity e : contained) {
            if (e instanceof Zombie && ((Zombie) e).isBaby() && !contains(e)) {
                for (int i = 0; i < 20; i++) getWorld().playEffect(e.getLocation(), Effect.SMOKE, 0);
                e.remove();
            }
        }
    }

    @Override
    public void disable() {

        removeMobs();
        if (mobDestroyer != null) mobDestroyer.cancel();
    }

    @Override
    public String getId() {

        return getRegion().getId();
    }

    @Override
    public void equalize() {

        for (Player player : getContainedPlayers()) {
            try {
                adminComponent.standardizePlayer(player);

                if (!isBossSpawned()) return;

                if (player.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    player.damage(32, boss);
                }

                if (Math.abs(groundLevel - player.getLocation().getY()) > 10) runAttack(4);
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }

    private static final List<PlayerTeleportEvent.TeleportCause> causes = new ArrayList<>(2);

    static {
        causes.add(PlayerTeleportEvent.TeleportCause.COMMAND);
        causes.add(PlayerTeleportEvent.TeleportCause.PLUGIN);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        if (contains(event.getTo(), 1) && causes.contains(event.getCause())) {

            Player player = event.getPlayer();

            for (PotionEffectType potionEffectType : PotionEffectType.values()) {
                if (potionEffectType == null) continue;
                if (player.hasPotionEffect(potionEffectType)) player.removePotionEffect(potionEffectType);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrayerApplication(PrayerApplicationEvent event) {

        if (contains(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreepSpeak(CreepSpeakEvent event) {

        if (contains(event.getPlayer(), 1) || contains(event.getTargeter(), 1)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {

        if (event.getWorld().equals(getWorld())) {

            int diff = getWorld().getDifficulty().getValue();
            if (event.toThunderState()) {
                difficulty = diff + diff;
            } else {
                difficulty = diff;
            }

            if (isBossSpawned()) {
                int newHealth = 510 + (difficulty * 80);
                boss.setHealth(Math.min(boss.getHealth(), newHealth));
                boss.setMaxHealth(newHealth);
            }
        }
    }

    /*
    @EventHandler(ignoreCancelled = true)
    public void onMobSpawn(CreatureSpawnEvent event) {

        Entity e = event.getEntity();
        if (contains(e) && !event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.CUSTOM)) {
            event.setCancelled(true);
        }
    }
    */

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {

        Player player = event.getPlayer();

        if (contains(player, 1)) {

            ItemStack stack = event.getItem();

            if (stack.getItemMeta() instanceof PotionMeta) {

                PotionMeta pMeta = (PotionMeta) stack.getItemMeta();

                if (pMeta.hasCustomEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    ChatUtil.sendWarning(player, "You find yourself unable to drink the potion.");
                    event.setCancelled(true);
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageEvent(EntityDamageEvent event) {

        Entity defender = event.getEntity();
        Entity attacker = null;
        Projectile projectile = null;

        if (event.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
                && !contains(defender) && contains(defender, 1)) {
            event.setCancelled(true);
            return;
        } else if (!contains(defender, 1)) return;

        if (event instanceof EntityDamageByEntityEvent) attacker = ((EntityDamageByEntityEvent) event).getDamager();

        if (attacker != null) {
            if (attacker instanceof Projectile) {
                if (((Projectile) attacker).getShooter() != null) {
                    projectile = (Projectile) attacker;
                    attacker = projectile.getShooter();
                } else if (!(attacker instanceof LivingEntity)) return;
            }

            if (defender instanceof Giant && attacker instanceof Player && !contains(attacker)) {

                // Heal boss
                boss.setHealth(Math.min(boss.getMaxHealth(), event.getDamage() + boss.getHealth()));

                // Evil code of doom
                ChatUtil.sendNotice((Player) attacker, "Come closer...");
                attacker.teleport(boss.getLocation());
                ((Player) attacker).damage(difficulty * 32, boss);
                server.getPluginManager().callEvent(new ThrowPlayerEvent((Player) attacker));
                attacker.setVelocity(new Vector(
                        random.nextDouble() * 1.7 - 1.5,
                        random.nextDouble() * 2,
                        random.nextDouble() * 1.7 - 1.5
                ));
            }

            if (attacker instanceof Player) {

                Player player = (Player) attacker;
                if (defender instanceof LivingEntity) {
                    if (ItemUtil.hasMasterSword(player)) {

                        if (ChanceUtil.getChance(difficulty * 3 + 1)) {
                            EffectUtil.Master.healingLight(player, (LivingEntity) defender);
                        }

                        if (ChanceUtil.getChance(difficulty * 3)) {
                            List<LivingEntity> entities = new ArrayList<>();
                            for (Entity e : player.getNearbyEntities(6, 4, 6)) {

                                if (EnvironmentUtil.isHostileEntity(e)) entities.add((LivingEntity) e);
                            }
                            EffectUtil.Master.doomBlade(player, entities);
                        }
                    }
                }
            }
        }

        if (attacker != null && !contains(attacker, 1) || !contains(defender, 1)) return;

        Player[] contained = getContainedPlayers();

        if (defender instanceof Giant) {
            Giant boss = (Giant) defender;
            if (damageHeals) {
                boss.setHealth(Math.min(boss.getMaxHealth(), (event.getDamage() * difficulty) + boss.getHealth()));
            }
            /*
            else if (projectile != null && projectile instanceof Arrow) {
                event.setDamage(event.getDamage() / 2);
            }
            */

            if (ChanceUtil.getChance(7)) {
                for (Location spawnPt : spawnPts) {
                    if (ChanceUtil.getChance(18)) {
                        for (int i = 0; i < Math.max(3, contained.length); i++) {
                            Zombie z = (Zombie) getWorld().spawnEntity(spawnPt, EntityType.ZOMBIE);
                            z.setBaby(true);
                            EntityEquipment equipment = z.getEquipment();
                            equipment.setItemInHand(new ItemStack(ItemID.STONE_SWORD));
                            if (attacker != null && attacker instanceof LivingEntity) {
                                z.setTarget((LivingEntity) attacker);
                            }
                        }
                    }
                }
            }

            if (attacker != null && attacker instanceof Player) {

                if (ItemUtil.hasForgeBook((Player) attacker)) {

                    ((Giant) defender).setHealth(0);

                    final Player finalAttacker = (Player) attacker;
                    if (!finalAttacker.getGameMode().equals(GameMode.CREATIVE)) {
                        server.getScheduler().runTaskLater(inst, new Runnable() {
                            @Override
                            public void run() {

                                (finalAttacker).setItemInHand(null);
                            }
                        }, 1);
                    }
                }
            }

        } else if (defender instanceof Player) {
            Player player = (Player) defender;
            if (ItemUtil.hasAncientArmour(player)) {
                if (attacker != null) {
                    if (attacker instanceof Zombie) {
                        Zombie zombie = (Zombie) attacker;
                        if (zombie.isBaby() && ChanceUtil.getChance(difficulty * 5)) {
                            ChatUtil.sendNotice(player, "Your armour weakens the zombies.");
                            for (Entity e : player.getNearbyEntities(8, 8, 8)) {
                                if (e.isValid() && e instanceof Zombie && ((Zombie) e).isBaby()) {
                                    ((Zombie) e).damage(18);
                                }
                            }
                        }
                    }

                    if (ChanceUtil.getChance(difficulty * 3)) EffectUtil.Ancient.powerBurst(player, event.getDamage());
                }
                if (ChanceUtil.getChance(difficulty) && defender.getFireTicks() > 0) {
                    ChatUtil.sendNotice((Player) defender, "Your armour extinguishes the fire.");
                    defender.setFireTicks(0);
                }
                if (damageHeals && ChanceUtil.getChance(difficulty * 3 + 1)) {
                    ChatUtil.sendNotice(getContainedPlayers(), ChatColor.AQUA,
                            player.getDisplayName() + " has broken the giant's spell.");
                    damageHeals = false;
                }
            }
        }
    }

    private static PotionEffect[] effects = new PotionEffect[] {
            new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 60 * 3, 2),
            new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 60 * 3, 2)
    };

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        if (contains(event.getEntity())) {
            Entity e = event.getEntity();
            if (e instanceof Giant) {
                int amt = getContainedPlayers() != null ? getContainedPlayers().length : 0;

                // Sacrificial drops
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 1, 200000));
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 10, 2000));
                event.getDrops().addAll(SacrificeComponent.getCalculatedLoot(server.getConsoleSender(), 32, 200));

                // Gold drops
                for (int i = 0; i < Math.sqrt(amt) + scalOffst; i++) {
                    event.getDrops().add(new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRangedRandom(32, 64)));
                }

                // Unique drops
                if (ChanceUtil.getChance(25)) {
                    event.getDrops().add(BookUtil.Lore.Monsters.skelril());
                }
                if (ChanceUtil.getChance(250)) {
                    event.getDrops().add(ItemUtil.Master.makeSword());
                }
                if (ChanceUtil.getChance(250)) {
                    event.getDrops().add(ItemUtil.Master.makeBow());
                }

                lastDeath = System.currentTimeMillis();
                boss = null;
                for (Entity entity : getContainedEntities()) {
                    if (entity instanceof Zombie) ((Zombie) entity).addPotionEffects(Lists.newArrayList(effects));
                }

                for (int i = 0; i < 7; i++) {
                    server.getScheduler().runTaskLater(inst, spawnXP, i * 2 * 20);
                }
            } else if (e instanceof Zombie && ((Zombie) e).isBaby()) {
                if (ChanceUtil.getChance(28)) {
                    event.getDrops().add(new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(3)));
                }
                event.setDroppedExp(14);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        if (contains(player, 1) && !adminComponent.isAdmin(player)) {
            if (isBossSpawned()) boss.setHealth(Math.min(boss.getMaxHealth(), boss.getHealth() + 250));
            playerState.put(player.getName(), new PlayerState(player.getName(),
                    player.getInventory().getContents(),
                    player.getInventory().getArmorContents(),
                    player.getLevel(),
                    player.getExp()));
            event.getDrops().clear();
            // event.setDroppedExp(0);
            // event.setKeepLevel(true);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        // Restore their inventory if they have one stored
        if (playerState.containsKey(player.getName()) && !adminComponent.isAdmin(player)) {

            try {
                PlayerState identity = playerState.get(player.getName());

                // Restore the contents
                player.getInventory().setArmorContents(identity.getArmourContents());
                player.getInventory().setContents(identity.getInventoryContents());
                // player.setLevel(identity.getLevel());
                // player.setExp(identity.getExperience());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                playerState.remove(player.getName());
            }
        }
    }

    private final int OPTION_COUNT = 7;

    private void runAttack(int attackCase) {

        if (!isBossSpawned() || (lastAttack != 0
                && System.currentTimeMillis() - lastAttack <= ChanceUtil.getRangedRandom(13000, 17000))) return;

        Player[] containedP = getContainedPlayers(1);
        Player[] contained = getContainedPlayers();
        if (contained == null || contained.length <= 0) return;

        if (attackCase < 1 || attackCase > OPTION_COUNT) attackCase = ChanceUtil.getRandom(OPTION_COUNT);
        switch (attackCase) {
            case 1:
                ChatUtil.sendWarning(containedP, "Taste my wrath!");
                for (Player player : contained) {

                    // Call this event to notify AntiCheat
                    server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                    player.setVelocity(new Vector(
                            random.nextDouble() * 1.7 - 1.5,
                            random.nextDouble() * 2,
                            random.nextDouble() * 1.7 - 1.5
                    ));
                    player.setFireTicks(difficulty * 18);
                }
                break;
            case 2:
                ChatUtil.sendWarning(containedP, "Embrace my corruption!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 12, difficulty > 2 ? 2 : 1));
                }
                break;
            case 3:
                ChatUtil.sendWarning(containedP, "Are you BLIND? Mwhahahaha!");
                for (Player player : contained) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 1));
                }
                break;
            case 4:
                ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "Tango time!");
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        if (!isBossSpawned()) return;
                        for (Player player : getContainedPlayers()) {
                            if (player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2).getTypeId()
                                    != BlockID.DIAMOND_BLOCK) {
                                ChatUtil.sendNotice(player, "Come closer...");
                                player.teleport(boss.getLocation());
                                player.damage(difficulty * 32, boss);

                                // Call this event to notify AntiCheat
                                server.getPluginManager().callEvent(new ThrowPlayerEvent(player));
                                player.setVelocity(new Vector(
                                        random.nextDouble() * 1.7 - 1.5,
                                        random.nextDouble() * 2,
                                        random.nextDouble() * 1.7 - 1.5
                                ));
                            } else {
                                ChatUtil.sendNotice(player, "Fine... No tango this time...");
                            }
                        }
                    }
                }, 20 * (28 - (difficulty * 7)));
                break;
            case 5:
                if (!damageHeals) {
                    ChatUtil.sendWarning(containedP, "I am everlasting!");
                    damageHeals = true;
                    server.getScheduler().runTaskLater(inst, new Runnable() {

                        @Override
                        public void run() {

                            if (damageHeals) {
                                damageHeals = false;
                                if (!isBossSpawned()) return;
                                ChatUtil.sendNotice(getContainedPlayers(1), "Thank you for your assistance.");
                            }
                        }
                    }, 20 * (difficulty * 10));
                    break;
                }
                runAttack(ChanceUtil.getRandom(OPTION_COUNT));
                return;
            case 6:
                ChatUtil.sendWarning(containedP, "Fire is your friend...");
                for (Player player : contained) {
                    player.setFireTicks(20 * (difficulty * 15));
                }
                break;
            case 7:
                ChatUtil.sendWarning(containedP, ChatColor.DARK_RED + "Bask in my glory!");
                server.getScheduler().runTaskLater(inst, new Runnable() {

                    @Override
                    public void run() {

                        // Set defaults
                        boolean baskInGlory = getContainedPlayers().length == 0;
                        damageHeals = true;

                        // Check Players
                        for (Player player : getContainedPlayers()) {
                            if (player.getLocation().getBlock().getRelative(BlockFace.DOWN, 2).getTypeId()
                                    != BlockID.DIAMOND_BLOCK) {
                                ChatUtil.sendWarning(player, ChatColor.DARK_RED + "You!");
                                baskInGlory = true;
                            }
                        }

                        //Attack
                        if (baskInGlory) {
                            int dmgFact = difficulty * 3 + 1;
                            for (Location pt : spawnPts) {
                                if (ChanceUtil.getChance(12)) {
                                    getWorld().createExplosion(pt.getX(), pt.getY(), pt.getZ(), dmgFact, false, false);
                                }
                            }

                            //Schedule Reset
                            server.getScheduler().runTaskLater(inst, new Runnable() {

                                @Override
                                public void run() {

                                    damageHeals = false;
                                }
                            }, 10);
                            return;
                        }
                        // Notify if avoided
                        ChatUtil.sendNotice(getContainedPlayers(1), "Gah... Afraid are you friends?");
                    }
                }, 20 * (28 - (difficulty * 7)));
                break;
        }
        lastAttack = System.currentTimeMillis();
    }
}
