package com.branders.spawnermod.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemMonsterPlacer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.network.SpawnerModNetwork;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Handles events regarding the mob spawner.
 */
public class SpawnerEventHandler {

    // Last seen spawner Delay per "dim_x_y_z". Used to detect when a spawner
    // completes a spawn cycle (vanilla resets Delay to a positive value after
    // spawning) so we can count towards the limited spawns limit.
    private Map<String, Short> spawnerDelays = new HashMap<String, Short>();

    /**
     * Called when player right-clicks a block.
     * Used to retrieve egg from Spawner.
     */
    @SubscribeEvent
    public void onBlockInteract(PlayerInteractEvent event) {

        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;

        if (event.entityPlayer == null) return;

        EntityPlayer player = event.entityPlayer;

        if (player.isSneaking()) return;

        // Only handle egg extraction on server side
        // Client side interactions (like spawn eggs, spawner key) should pass through
        if (event.world.isRemote) return;

        if (event.world.getBlock(event.x, event.y, event.z) != Blocks.mob_spawner) return;

        if (ConfigValues.get("disable_egg_removal_from_spawner") != 0) return;

        // Get the spawner tile entity and check if it's empty
        TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
        if (!(te instanceof TileEntityMobSpawner)) return;
        TileEntityMobSpawner spawner = (TileEntityMobSpawner) te;

        NBTTagCompound nbt = new NBTTagCompound();
        spawner.writeToNBT(nbt);
        String currentEntity = getEntityNameFromNBT(nbt);
        boolean isEmpty = (currentEntity == null || currentEntity.isEmpty());

        ItemStack heldItem = player.getHeldItem();
        Item item = heldItem != null ? heldItem.getItem() : null;

        // Spawner key should handle its own GUI opening - don't interfere
        if (item != null && item instanceof com.branders.spawnermod.item.SpawnerKey) return;
        // Block items should not extract egg
        if (item instanceof ItemBlock) return;
        // Spawn eggs: let vanilla handle adding entity to spawner, don't extract egg
        if (item instanceof ItemMonsterPlacer) {
            // In 1.7.10, we need to manually handle spawn egg on spawner
            // because the event system might interfere with vanilla behavior
            handleSpawnEggOnSpawner(player, event.x, event.y, event.z, event.world, (ItemMonsterPlacer) item);
            // Cancel the event to prevent vanilla ItemMonsterPlacer from spawning an entity
            event.setCanceled(true);
            return;
        }

        // Check blacklist
        if (item != null) {
            String registryName = Item.itemRegistry.getNameForObject(item);
            if (ConfigValues.get("display_item_id_from_right_click_in_log") == 1)
                SpawnerMod.LOGGER.info("Right clicked with item id: " + registryName);
            if (ConfigValues.isItemIdBlacklisted(registryName)) return;
        }

        // Empty hand or non-egg item: extract egg if spawner has an entity
        if (!isEmpty) {
            dropMonsterEgg(event.x, event.y, event.z, event.world);
        }
    }

    /**
     * Called when player breaks a block.
     * If silk touch was used we want to drop the monster egg.
     */
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {

        if (event.world.isRemote) return;

        EntityPlayer player = event.getPlayer();
        if (player == null) return;

        if (player.capabilities.isCreativeMode) return;

        if (event.block != Blocks.mob_spawner) return;

        ItemStack stack = player.getHeldItem();

        if (checkSilkTouch(stack) && ConfigValues.get("disable_silk_touch") == 0) {
            if (ConfigValues.get("disable_egg_removal_from_spawner") == 0)
                dropMonsterEgg(event.x, event.y, event.z, event.world);
        }
        // No XP orb is spawned here on purpose: vanilla 1.7.10 already drops
        // 15 + rand(15) + rand(15) XP via BlockMobSpawner.dropBlockAsItemWithChance
        // -> dropXpOnBlockBreak when a player harvests the block. Spawning another
        // orb here (as the Fabric original does, which cancels vanilla XP via a
        // mixin) would double the experience.
    }

    /**
     * Cancel the normal spawner block drop (we handle it ourselves).
     * Vanilla spawners drop no items anyway; this only matters when silk touch
     * drops are disabled via config and the vanilla item drop list has to stay empty.
     */
    @SubscribeEvent
    public void onHarvestDrop(BlockEvent.HarvestDropsEvent event) {
        if (event.block == Blocks.mob_spawner) {
            if (ConfigValues.get("disable_silk_touch") == 1) {
                event.drops.clear();
            }
        }
    }

    /**
     * Drops the Monster Egg which is inside the spawner.
     */
    private void dropMonsterEgg(int x, int y, int z, World world) {
        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileEntityMobSpawner)) return;

        TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileEntity;
        NBTTagCompound nbt = new NBTTagCompound();
        spawner.writeToNBT(nbt);

        // Get entity name from spawner NBT
        // In 1.7.10, the entity is stored as string "EntityId" (e.g., "Pig", "Zombie")
        // or in "SpawnData" compound
        String entityName = null;

        if (nbt.hasKey("SpawnData")) {
            NBTTagCompound spawnData = nbt.getCompoundTag("SpawnData");
            if (spawnData.hasKey("id")) {
                String id = spawnData.getString("id");
                // Strip namespace if present
                if (id.indexOf(':') > 0) {
                    id = id.substring(id.indexOf(':') + 1);
                }
                entityName = id;
            }
        }

        if (entityName == null) {
            entityName = nbt.getString("EntityId");
        }

        if (entityName == null || entityName.isEmpty()) return;

        // Just in case
        if (entityName.contains("area_effect_cloud") || entityName.contains("AreaEffectCloud")) return;

        // Get spawn egg for this entity
        int entityId = getEntityIdFromString(entityName);
        if (entityId == 0) {
            SpawnerMod.LOGGER.info("Could not find spawn egg for: " + entityName);
            return;
        }

        ItemStack eggStack = new ItemStack(Items.spawn_egg, 1, entityId);

        // Get random fly-out position offsets
        double d0 = (double) (world.rand.nextFloat() * 0.7F) + 0.15D;
        double d1 = (double) (world.rand.nextFloat() * 0.7F) + 0.06D + 0.6D;
        double d2 = (double) (world.rand.nextFloat() * 0.7F) + 0.15D;

        // Create entity item
        EntityItem entityItem = new EntityItem(world, (double) x + d0, (double) y + d1, (double) z + d2, eggStack);
        entityItem.delayBeforeCanPickup = 10;

        // Spawn entity item (egg)
        world.spawnEntityInWorld(entityItem);

        // Clear the spawner (empty)
        nbt.setString("EntityId", "");
        if (nbt.hasKey("SpawnData")) nbt.removeTag("SpawnData");
        spawner.readFromNBT(nbt);
        spawner.markDirty();
        world.markBlockForUpdate(x, y, z);
    }

    /**
     * Mob drops: chance to drop spawn egg
     */
    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        EntityPlayer player = event.source.getEntity() instanceof EntityPlayer ? (EntityPlayer) event.source.getEntity()
            : null;

        // Leave if eggs should only drop when killed by a player
        if (ConfigValues.get("monster_egg_only_drop_when_killed_by_player") == 1 && player == null) return;

        Random random = new Random();
        if (random.nextFloat() > ConfigValues.get("monster_egg_drop_chance") / 100f) return;

        String entityString = EntityList.getEntityString(event.entityLiving);
        if (entityString == null) return;

        if (ConfigValues.isEggDisabled(entityString)) return;

        int entityId = getEntityIdFromString(entityString);
        if (entityId == 0) return;

        ItemStack egg = new ItemStack(Items.spawn_egg, 1, entityId);
        event.entityLiving.entityDropItem(egg, 0.0F);
    }

    /**
     * Get entity ID from entity name string.
     * In 1.7.10, classToIDMapping is private, so we iterate IDtoClassMapping instead.
     */
    private int getEntityIdFromString(String name) {
        Class<?> entityClass = (Class<?>) EntityList.stringToClassMapping.get(name);
        if (entityClass != null) {
            for (Object o : EntityList.IDtoClassMapping.entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                if (entry.getValue() == entityClass) {
                    return (Integer) entry.getKey();
                }
            }
        }
        return 0;
    }

    /**
     * Get entity string name from entity ID.
     * Reverse lookup: ID -> Class -> String name
     */
    private String getEntityStringFromId(int id) {
        Class<?> entityClass = (Class<?>) EntityList.IDtoClassMapping.get(id);
        if (entityClass != null) {
            for (Object o : EntityList.stringToClassMapping.entrySet()) {
                Map.Entry<String, Class> entry = (Map.Entry<String, Class>) o;
                if (entry.getValue() == entityClass) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * Get entity name string from spawner NBT.
     * Checks both SpawnData compound and EntityId string.
     */
    private String getEntityNameFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("SpawnData")) {
            NBTTagCompound spawnData = nbt.getCompoundTag("SpawnData");
            if (spawnData.hasKey("id")) {
                String id = spawnData.getString("id");
                if (id.indexOf(':') > 0) {
                    id = id.substring(id.indexOf(':') + 1);
                }
                return id;
            }
        }
        String entityId = nbt.getString("EntityId");
        if (entityId != null && !entityId.isEmpty()) {
            return entityId;
        }
        return null;
    }

    /**
     * Handle spawn egg right-click on spawner to set entity.
     * In 1.7.10, we need to manually implement this because the event system
     * might interfere with vanilla ItemMonsterPlacer behavior.
     */
    private void handleSpawnEggOnSpawner(EntityPlayer player, int x, int y, int z, World world,
        ItemMonsterPlacer eggItem) {
        if (world.isRemote) return;

        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityMobSpawner)) return;

        TileEntityMobSpawner spawner = (TileEntityMobSpawner) te;
        ItemStack stack = player.getHeldItem();
        if (stack == null) return;

        // Get entity ID from the spawn egg item damage value
        int entityId = stack.getItemDamage();
        String entityName = getEntityStringFromId(entityId);

        if (entityName == null || entityName.isEmpty()) {
            SpawnerMod.LOGGER.warn("Could not find entity name for egg ID: " + entityId);
            return;
        }

        // Set the spawner's entity
        NBTTagCompound nbt = new NBTTagCompound();
        spawner.writeToNBT(nbt);
        nbt.setString("EntityId", entityName);

        // Also update SpawnData for compatibility
        NBTTagCompound spawnData = new NBTTagCompound();
        spawnData.setString("id", entityName);
        nbt.setTag("SpawnData", spawnData);

        spawner.readFromNBT(nbt);
        spawner.markDirty();
        world.markBlockForUpdate(x, y, z);

        // Play sound effect
        world.playAuxSFX(1004, x, y, z, 0);

        // Consume one egg if not in creative mode
        if (!player.capabilities.isCreativeMode) {
            stack.stackSize--;
            if (stack.stackSize <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
        }
    }

    private boolean checkSilkTouch(ItemStack stack) {
        if (stack == null) return false;
        // In 1.7.10, silk touch is enchantment ID 33
        if (stack.getEnchantmentTagList() == null) return false;
        for (int i = 0; i < stack.getEnchantmentTagList()
            .tagCount(); i++) {
            NBTTagCompound tag = stack.getEnchantmentTagList()
                .getCompoundTagAt(i);
            if (tag.getShort("id") == 33) { // Silk Touch enchantment ID
                return true;
            }
        }
        return false;
    }

    /**
     * Player join event - sync config to client
     */
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            SpawnerModNetwork.sendConfigToClient((net.minecraft.entity.player.EntityPlayerMP) event.player);
        }
    }

    /**
     * World tick event for limited spawns, redstone control, and default range
     */
    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote) return;
        if (event.phase != TickEvent.Phase.END) return;

        // Handle limited spawns - check all loaded spawners
        if (ConfigValues.get("limited_spawns_enabled") != 0) {
            checkLimitedSpawns(event.world);
        } else {
            spawnerDelays.clear();
        }

        // Handle redstone control for spawners
        checkRedstoneControl(event.world);

        // Apply the default spawner range to spawners that still use the
        // vanilla default. It is applied lazily and persisted via a flag so
        // newly placed spawners get it too (not only spawners present at
        // world load) without re-applying after a chunk reload.
        if (ConfigValues.get("default_spawner_range_enabled") == 1) {
            applyDefaultRange(event.world);
        }
    }

    /**
     * Drop per-spawner tracking state when a world unloads so it cannot leak
     * across world reloads.
     */
    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        spawnerDelays.clear();
    }

    /**
     * Check and update limited spawns for all loaded spawners.
     *
     * The spawn count is tracked in the spawner NBT field "spawns". Because this
     * port has no mixin hook into the vanilla spawner logic, a spawn cycle is
     * detected by watching the Delay field: after a successful spawn vanilla
     * resets Delay to a positive random value inside [MinSpawnDelay,
     * MaxSpawnDelay], so the transition "Delay <= 0 -> Delay > 0" marks one
     * completed spawn. When the cap is reached the spawner is disabled by
     * setting RequiredPlayerRange to 0.
     */
    private void checkLimitedSpawns(World world) {
        int amount = ConfigValues.get("limited_spawns_amount");
        int dim = world.provider.dimensionId;
        // Keys of the spawners currently loaded in this world, used to drop
        // tracking state for spawners that got unloaded.
        Set<String> seen = new HashSet<String>();

        for (Object obj : world.loadedTileEntityList) {
            if (!(obj instanceof TileEntityMobSpawner)) continue;
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) obj;
            int x = spawner.xCoord;
            int y = spawner.yCoord;
            int z = spawner.zCoord;
            String key = dim + "_" + x + "_" + y + "_" + z;
            seen.add(key);

            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);

            // Detect a completed spawn cycle via the Delay transition.
            short delay = nbt.getShort("Delay");
            Short lastDelay = spawnerDelays.get(key);
            if (lastDelay != null && lastDelay <= 0 && delay > 0) {
                short spawns = nbt.getShort("spawns");
                if (spawns < Short.MAX_VALUE) spawns++;
                nbt.setShort("spawns", spawns);
                spawner.readFromNBT(nbt);
                spawner.markDirty();
            }
            spawnerDelays.put(key, delay);

            // Disable the spawner once the limit has been reached
            if (nbt.getShort("spawns") >= amount && nbt.getShort("RequiredPlayerRange") != 0) {
                nbt.setShort("RequiredPlayerRange", (short) 0);
                spawner.readFromNBT(nbt);
                spawner.markDirty();
                world.markBlockForUpdate(x, y, z);
            }
        }

        // Forget spawners that are no longer loaded
        spawnerDelays.keySet()
            .retainAll(seen);
    }

    /**
     * Check redstone control for all loaded spawners.
     * When a spawner receives a redstone signal, disable it (set range to 0)
     * and remember the original range. When the signal is removed, restore it.
     *
     * The custom NBT flag "ems_redstone_disabled" distinguishes a redstone
     * disable from a disable done through the GUI or the limited spawns limit,
     * so removing the redstone signal can never re-enable a spawner that was
     * intentionally disabled some other way.
     */
    private void checkRedstoneControl(World world) {
        for (Object obj : world.loadedTileEntityList) {
            if (!(obj instanceof TileEntityMobSpawner)) continue;
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) obj;
            int x = spawner.xCoord;
            int y = spawner.yCoord;
            int z = spawner.zCoord;

            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);

            boolean isPowered = world.isBlockIndirectlyGettingPowered(x, y, z);
            short currentRange = nbt.getShort("RequiredPlayerRange");
            short spawnRange = nbt.getShort("SpawnRange");
            boolean redstoneDisabled = nbt.getByte("ems_redstone_disabled") != 0;

            if (isPowered) {
                // Disable while powered, stashing the current range in SpawnRange.
                // Skip spawners that are already disabled (currentRange == 0) so an
                // intentional GUI / limited-spawns disable is never overwritten.
                if (!redstoneDisabled && currentRange > 0) {
                    nbt.setShort("SpawnRange", currentRange);
                    nbt.setShort("RequiredPlayerRange", (short) 0);
                    nbt.setByte("ems_redstone_disabled", (byte) 1);
                    spawner.readFromNBT(nbt);
                    spawner.markDirty();
                    world.markBlockForUpdate(x, y, z);
                }
            } else if (redstoneDisabled && currentRange == 0 && spawnRange > 4) {
                // Redstone power removed - restore the range stashed by redstone
                nbt.setShort("RequiredPlayerRange", spawnRange);
                nbt.setShort("SpawnRange", (short) 4); // Reset to default
                nbt.setByte("ems_redstone_disabled", (byte) 0);
                spawner.readFromNBT(nbt);
                spawner.markDirty();
                world.markBlockForUpdate(x, y, z);
            }
        }
    }

    /**
     * Apply the default spawner range to every spawner that still uses the
     * vanilla default (RequiredPlayerRange == 16). The custom flag
     * "ems_range_set" is persisted so the range is applied exactly once per
     * spawner, including spawners placed after world load, and is not applied
     * again after a chunk reload.
     */
    private void applyDefaultRange(World world) {
        short defaultRange = (short) ConfigValues.get("default_spawner_range");

        for (Object obj : world.loadedTileEntityList) {
            if (!(obj instanceof TileEntityMobSpawner)) continue;
            TileEntityMobSpawner spawner = (TileEntityMobSpawner) obj;

            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);

            if (nbt.getByte("ems_range_set") == 0 && nbt.getShort("RequiredPlayerRange") == 16) {
                nbt.setShort("RequiredPlayerRange", defaultRange);
                nbt.setByte("ems_range_set", (byte) 1);
                spawner.readFromNBT(nbt);
                spawner.markDirty();
                world.markBlockForUpdate(spawner.xCoord, spawner.yCoord, spawner.zCoord);
            }
        }
    }

    /**
     * Entity join event - placeholder for future spawner spawn tracking.
     * Currently unused; limited spawns are tracked via world tick instead.
     */
    @SubscribeEvent
    public void onEntityJoinWorld(net.minecraftforge.event.entity.EntityJoinWorldEvent event) {
        // Intentionally empty - limited spawns checked via onWorldTick
    }
}
