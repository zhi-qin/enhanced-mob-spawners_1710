package com.branders.spawnermod.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.item.SpawnerKey;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SyncSpawnerMessage implements IMessage {

    private int x, y, z;
    private short delay;
    private short spawnCount;
    private short requiredPlayerRange;
    private short maxNearbyEntities;
    private short minSpawnDelay;
    private short maxSpawnDelay;
    private short spawnRange;

    public SyncSpawnerMessage() {}

    public SyncSpawnerMessage(int x, int y, int z, short delay, short spawnCount, short requiredPlayerRange,
        short maxNearbyEntities, short minSpawnDelay, short maxSpawnDelay, short spawnRange) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delay = delay;
        this.spawnCount = spawnCount;
        this.requiredPlayerRange = requiredPlayerRange;
        this.maxNearbyEntities = maxNearbyEntities;
        this.minSpawnDelay = minSpawnDelay;
        this.maxSpawnDelay = maxSpawnDelay;
        this.spawnRange = spawnRange;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        delay = buf.readShort();
        spawnCount = buf.readShort();
        requiredPlayerRange = buf.readShort();
        maxNearbyEntities = buf.readShort();
        minSpawnDelay = buf.readShort();
        maxSpawnDelay = buf.readShort();
        spawnRange = buf.readShort();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeShort(delay);
        buf.writeShort(spawnCount);
        buf.writeShort(requiredPlayerRange);
        buf.writeShort(maxNearbyEntities);
        buf.writeShort(minSpawnDelay);
        buf.writeShort(maxSpawnDelay);
        buf.writeShort(spawnRange);
    }

    public static class Handler implements IMessageHandler<SyncSpawnerMessage, IMessage> {

        @Override
        public IMessage onMessage(SyncSpawnerMessage message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            World world = player.worldObj;

            if (world == null) return null;

            // Server-side validation: reject changes when the spawner config is disabled
            if (ConfigValues.get("disable_spawner_config") != 0) return null;

            // Verify the block is a spawner
            if (world.getBlock(message.x, message.y, message.z) != Blocks.mob_spawner) return null;

            // Only a player actually holding a Spawner Key may reconfigure a spawner.
            // This prevents arbitrary clients from modifying spawners they cannot reach.
            ItemStack held = player.getHeldItem();
            if (held == null || !(held.getItem() instanceof SpawnerKey)) return null;

            // The spawner must be within interaction range of the player
            double distSq = player.getDistanceSq(message.x + 0.5D, message.y + 0.5D, message.z + 0.5D);
            if (distSq > 16.0D * 16.0D) return null;

            TileEntity tileEntity = world.getTileEntity(message.x, message.y, message.z);
            if (!(tileEntity instanceof TileEntityMobSpawner)) return null;

            TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileEntity;
            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);

            // A GUI save is authoritative over any redstone disable state:
            // clear the redstone flag and its stashed range so a later redstone
            // unpower can never re-enable a spawner the player just configured.
            // SpawnRange is left untouched - it is the vanilla spawn area and is
            // not used as a stash anymore.
            nbt.removeTag("ems_prev_range");
            nbt.setByte("ems_redstone_disabled", (byte) 0);

            // Apply NBT values (same as 1.12+)
            nbt.setShort("Delay", (short) 0);
            nbt.setShort("SpawnCount", message.spawnCount);
            nbt.setShort("RequiredPlayerRange", message.requiredPlayerRange);
            nbt.setShort("MaxNearbyEntities", message.maxNearbyEntities);
            nbt.setShort("MinSpawnDelay", message.minSpawnDelay);
            nbt.setShort("MaxSpawnDelay", message.maxSpawnDelay);
            nbt.setShort("SpawnRange", message.spawnRange);

            // Write back to spawner (equivalent to 1.12+'s logic.readNbt)
            spawner.readFromNBT(nbt);
            spawner.markDirty();
            world.markBlockForUpdate(message.x, message.y, message.z);

            // Damage the Spawner Key item
            held.damageItem(1, player);

            world.playAuxSFX(1004, message.x, message.y, message.z, 0);

            return null;
        }
    }
}
