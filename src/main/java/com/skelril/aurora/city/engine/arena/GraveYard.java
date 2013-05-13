package com.skelril.aurora.city.engine.arena;

import com.sk89q.commandbook.CommandBook;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.skelril.aurora.admin.AdminComponent;
import com.skelril.aurora.util.ChanceUtil;
import com.skelril.aurora.util.EnvironmentUtil;
import com.skelril.aurora.util.item.ItemUtil;
import com.skelril.aurora.util.LocationUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Lever;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GraveYard extends AbstractRegionedArena implements MonitoredArena, Listener{

    private final CommandBook inst = CommandBook.inst();
    private final Logger log = inst.getLogger();
    private final Server server = CommandBook.server();

    // Components
    private AdminComponent adminComponent;

    // Temple regions
    private ProtectedRegion temple, pressurePlateLockArea;

    // Block information
    private static Set<BaseBlock> breakable = new HashSet<>();
    static {
        breakable.add(new BaseBlock(BlockID.DIRT));
        breakable.add(new BaseBlock(BlockID.TORCH));
        breakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
    }
    private static Set<BaseBlock> autoBreakable = new HashSet<>();
    static {
        breakable.add(new BaseBlock(BlockID.STEP, 5));
        breakable.add(new BaseBlock(BlockID.STEP, 11));
        breakable.add(new BaseBlock(BlockID.STONE_BRICK, 2));
    }

    private final Random random = new Random();

    // Head Stones
    private List<Location> headStones = new ArrayList<>();

    // Pressure Plate Lock
    // Use a boolean to store the check value instead of checking for every step
    private boolean isPressurePlateLocked = true;
    private ConcurrentHashMap<Location, Boolean> pressurePlateLocks = new ConcurrentHashMap<>();

    // Block Restoration Map
    private ConcurrentHashMap<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> map = new ConcurrentHashMap<>();

    public GraveYard(World world, ProtectedRegion region) {
        super(world, region);

        findHeadStones();
        findPressurePlateLockLevers();
    }

    @EventHandler(ignoreCancelled = true)
    public void onThunderChange(ThunderChangeEvent event) {

        if (event.toThunderState()) {
            resetPressurePlateLock();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent event) {

        World world = event.getWorld();
        if (getWorld().equals(world) && world.isThundering()) {
            for (Location headStone : headStones) {
                if (ChanceUtil.getChance(18)) {
                    for (int i = 0; i < ChanceUtil.getRangedRandom(3, 6); i++) {
                        spawnAndArm(headStone, EntityType.ZOMBIE, true);
                    }
                }
            }
        }
    }

    private void localSpawn(Player player) {

        Block playerBlock = player.getLocation().getBlock();
        Location ls;

        for (int i = 0; i < ChanceUtil.getRandom(16 - playerBlock.getLightLevel()); i++) {

            ls = LocationUtil.findRandomLoc(playerBlock, 8, true, false);

            if (!BlockType.isTranslucent(ls.getBlock().getTypeId())) {
                ls = player.getLocation();
            }

            spawnAndArm(ls, EntityType.ZOMBIE, true);
        }
    }

    private void spawnAndArm(Location location, EntityType type, boolean allowItemPickup) {

        if (!location.getChunk().isLoaded()) return;

        Entity e = spawn(location, type);
        if (e == null) return;
        arm(e, allowItemPickup);
    }

    private Entity spawn(Location location, EntityType type) {

        if (location == null || !type.isAlive()) return null;
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, type);
        entity.setCustomName("Grave Zombie");
        entity.setCustomNameVisible(false);
        return entity;
    }

    private void arm(Entity e, boolean allowItemPickup) {

        if (!(e instanceof LivingEntity)) return;

        EntityEquipment equipment = ((LivingEntity) e).getEquipment();
        ((LivingEntity) e).setCanPickupItems(allowItemPickup);

        if (ChanceUtil.getChance(50)) {
            if (ChanceUtil.getChance(35)) {
                equipment.setArmorContents(ItemUtil.diamondArmour);
            } else {
                equipment.setArmorContents(ItemUtil.ironArmour);
            }

            if (ChanceUtil.getChance(4)) equipment.setHelmet(null);
            if (ChanceUtil.getChance(4)) equipment.setChestplate(null);
            if (ChanceUtil.getChance(4)) equipment.setLeggings(null);
            if (ChanceUtil.getChance(4)) equipment.setBoots(null);
        }

        if (ChanceUtil.getChance(50)) {
            ItemStack sword = new ItemStack(ItemID.IRON_SWORD);
            if (ChanceUtil.getChance(35)) sword = new ItemStack(ItemID.DIAMOND_SWORD);
            ItemMeta meta = sword.getItemMeta();
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ALL, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.DAMAGE_UNDEAD, ChanceUtil.getRandom(5), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.FIRE_ASPECT, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.KNOCKBACK, ChanceUtil.getRandom(2), false);
            if (ChanceUtil.getChance(2)) meta.addEnchant(Enchantment.LOOT_BONUS_MOBS, ChanceUtil.getRandom(3), false);
            sword.setItemMeta(meta);
            equipment.setItemInHand(sword);
        }

        if (allowItemPickup) {
            equipment.setItemInHandDropChance(1);
            equipment.setHelmetDropChance(1);
            equipment.setChestplateDropChance(1);
            equipment.setLeggingsDropChance(1);
            equipment.setBootsDropChance(1);
        } else {
            equipment.setItemInHandDropChance(.55F);
            equipment.setHelmetDropChance(.55F);
            equipment.setChestplateDropChance(.55F);
            equipment.setLeggingsDropChance(.55F);
            equipment.setBootsDropChance(.55F);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();

        if (contains(entity)) {

            if (entity.getCustomName() != null) {
                String customName = entity.getCustomName();
                List<ItemStack> drops = event.getDrops();

                if (customName.equals("Grave Zombie")) {
                    if (ChanceUtil.getChance(400) || getWorld().isThundering() && ChanceUtil.getChance(275)) {
                        ItemStack stack = new ItemStack(ItemID.EMERALD);
                        ItemMeta meta = stack.getItemMeta();
                        meta.setDisplayName(ChatColor.DARK_RED + "Gem of Darkness");
                        stack.setItemMeta(meta);
                        drops.add(stack);
                    }

                    if (ChanceUtil.getChance(237)) {
                        ItemStack phantomGold = new ItemStack(ItemID.GOLD_BAR, ChanceUtil.getRandom(3));
                        ItemMeta meta = phantomGold.getItemMeta();
                        meta.setDisplayName(ChatColor.GOLD + "Phantom Gold");
                        phantomGold.setItemMeta(meta);
                        drops.add(phantomGold);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {

        int fromType = event.getSource().getTypeId();

        if (fromType == BlockID.GRASS && contains(event.getBlock())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {

        Block block = event.getBlock();
        BaseBlock baseBlock = new BaseBlock(block.getTypeId(), block.getData());
        if (contains(block)) {

            if (!breakable.contains(baseBlock)) {
                event.setCancelled(true);
                return;
            }

            map.put(block.getLocation(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), baseBlock));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (contains(event.getBlock())) {

            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {

        Block block = event.getBlock();
        Location contactedLoc = block.getLocation();
        if (LocationUtil.isInRegion(getWorld(), temple, contactedLoc)) {
            if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE && isPressurePlateLocked) {
                throwSlashPotion(contactedLoc);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {

        Block block = event.getClickedBlock();
        Location clickedLoc = block.getLocation();
        if (LocationUtil.isInRegion(getWorld(), temple, clickedLoc)) {
            if (event.getAction().equals(Action.PHYSICAL)) {
                if (block.getTypeId() == BlockID.STONE_PRESSURE_PLATE && isPressurePlateLocked) {
                    throwSlashPotion(clickedLoc);
                }
            }
        }
    }

    private void throwSlashPotion(Location location) {

        ThrownPotion potion = (ThrownPotion) getWorld().spawnEntity(location, EntityType.SPLASH_POTION);
        PotionType type = PotionType.values()[ChanceUtil.getRandom(PotionType.values().length) - 1];
        potion.setItem(new Potion(type).splash().toItemStack(1));
        potion.setVelocity(new Vector(
                random.nextDouble() * .25 - .25,
                random.nextDouble() * .4 + .1,
                random.nextDouble() * .25 - .25
        ));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (LocationUtil.isInRegion(getWorld(), getRegion(), player.getLocation())) {

            List<ItemStack> drops = event.getDrops();
            makeGrave(player.getName(), ItemUtil.clone(drops.toArray(new ItemStack[drops.size()])));
            drops.clear();

            event.setDeathMessage(ChatColor.DARK_RED + "RIP ~ " + player.getName());
        }
    }

    private void makeGrave(String name, ItemStack[] itemStacks) {

        Location headStone = headStones.get(ChanceUtil.getRandom(headStones.size()) - 1).clone();
        BlockState signState = headStone.getBlock().getState();

        if (signState instanceof Sign) {

            ((Sign) signState).setLine(1, "RIP");
            ((Sign) signState).setLine(2, name);

            headStone.add(0, -2, 0);

            BlockState chestState = headStone.getBlock().getState();

            if (chestState instanceof Chest) {

                ((Chest) chestState).getInventory().clear();
                ((Chest) chestState).getInventory().addItem(itemStacks);
            } else {
                headStone.add(0, -1, 0);

                chestState = headStone.getBlock().getState();

                if (chestState instanceof Chest) {

                    ((Chest) chestState).getInventory().clear();
                    ((Chest) chestState).getInventory().addItem(itemStacks);
                }
            }
        }
    }

    private void findHeadStones() {

        com.sk89q.worldedit.Vector min = getRegion().getMinimumPoint();
        com.sk89q.worldedit.Vector max = getRegion().getMaximumPoint();

        int minX = min.getBlockX();
        int minZ = min.getBlockZ();
        int maxX = max.getBlockX();
        int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!checkHeadStone(x, 82, z)) {
                    checkHeadStone(x, 81, z);
                }
            }
        }
    }

    private boolean checkHeadStone(int x, int y, int z) {

        BlockState block = getWorld().getBlockAt(x, y, z).getState();
        if (!block.getChunk().isLoaded()) block.getChunk().load();
        if (block.getTypeId() == BlockID.WALL_SIGN) {
            ((Sign) block).setLine(0, null);
            ((Sign) block).setLine(1, null);
            ((Sign) block).setLine(2, null);
            ((Sign) block).setLine(3, null);
            block.update(true);
            headStones.add(block.getLocation());
            return true;
        }
        return false;
    }

    private void findPressurePlateLockLevers() {

        com.sk89q.worldedit.Vector min = pressurePlateLockArea.getMinimumPoint();
        com.sk89q.worldedit.Vector max = pressurePlateLockArea.getMaximumPoint();

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
                    if (block.getTypeId() == BlockID.LEVER) {
                        Lever lever = (Lever) block.getData();
                        lever.setPowered(false);
                        block.setData(lever);
                        block.update(true);
                        pressurePlateLocks.put(block.getLocation(), !ChanceUtil.getChance(3));
                    }
                }
            }
        }
    }

    private boolean checkPressurePlateLock() {

        for (Map.Entry<Location, Boolean> lever : pressurePlateLocks.entrySet()) {

            if (!lever.getKey().getBlock().getChunk().isLoaded()) return false;
            Lever aLever = (Lever) lever.getKey().getBlock().getState().getData();
            if (aLever.isPowered() != lever.getValue()) return false;
        }
        return true;
    }

    private void resetPressurePlateLock() {

        BlockState state;
        for (Location entry : pressurePlateLocks.keySet()) {

            if (!entry.getBlock().getChunk().isLoaded()) entry.getBlock().getChunk().load();
            state = entry.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
            pressurePlateLocks.put(entry, !ChanceUtil.getChance(3));
        }
    }

    private boolean breakBlock(Location location) {

        Block block = location.getBlock();
        return ChanceUtil.getChance(3) && autoBreakable.contains(new BaseBlock(block.getTypeId(), block.getData()));
    }

    private void fogPlayer(Player player) {

        Location loc = player.getLocation();

        if (loc.getBlock().getLightLevel() <= 4 || loc.getBlock().getLightFromSky() <= 12 && loc.getBlockY() < 93) {

            if (ItemUtil.hasFearHelmet(player)) return;
            ItemStack[] inventoryContents = player.getInventory().getContents();
            if (ItemUtil.findItemOfName(inventoryContents, ChatColor.RED + "Gem of Darkness")) return;
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 20, 2));
        }
    }

    @Override
    public void forceRestoreBlocks() {

        resetPressurePlateLock();
        BaseBlock b;
        for (Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e : map.entrySet()) {
            b = e.getValue().getValue();
            if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
            e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
        }
        map.clear();
    }

    public void restoreBlocks() {

        int min = 1000 * 60 * 10;

        BaseBlock b;
        Map.Entry<Location, AbstractMap.SimpleEntry<Long, BaseBlock>> e;
        Iterator<Map.Entry<Location,AbstractMap.SimpleEntry<Long,BaseBlock>>> it = map.entrySet().iterator();

        while(it.hasNext()) {
            e = it.next();
            if ((System.currentTimeMillis() - e.getValue().getKey()) > min) {
                b = e.getValue().getValue();
                if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
                e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                it.remove();
            } else if (System.currentTimeMillis() - e.getValue().getKey() > (min / 20)
                    && EnvironmentUtil.isShrubBlock(e.getValue().getValue().getType())) {
                b = e.getValue().getValue();
                if (!e.getKey().getChunk().isLoaded()) e.getKey().getChunk().load();
                e.getKey().getBlock().setTypeIdAndData(b.getType(), (byte) b.getData(), true);
                it.remove();
            }
        }
    }

    @Override
    public void run() {

        restoreBlocks();
        isPressurePlateLocked = !checkPressurePlateLock();

        Entity[] contained = getContainedEntities();
        for (Entity entity : contained) {

            // Auto break stuff
            Location belowLoc = entity.getLocation().add(0, -1, 0);
            if (breakBlock(belowLoc) || breakBlock(belowLoc.add(0, -1, 0))) {

                Block belowBlock = belowLoc.getBlock();
                BaseBlock belowBaseBlock = new BaseBlock(belowBlock.getTypeId(), belowBlock.getData());
                map.put(belowLoc, new AbstractMap.SimpleEntry<>(System.currentTimeMillis(), belowBaseBlock));
                belowLoc.getBlock().breakNaturally();
            }

            // Blind people
            if (entity instanceof Player) {
                fogPlayer((Player) entity);
                localSpawn((Player) entity);
            }
        }
    }

    @Override
    public void disable() {

        restoreBlocks();
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
            } catch (Exception e) {
                log.warning("The player: " + player.getName() + " may have an unfair advantage.");
            }
        }
    }

    @Override
    public ArenaType getArenaType() {

        return ArenaType.MONITORED;
    }
}