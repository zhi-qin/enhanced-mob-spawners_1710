package com.branders.spawnermod.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

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

    public SyncSpawnerMessage() {}

    public SyncSpawnerMessage(int x, int y, int z, short delay, short spawnCount, short requiredPlayerRange,
        short maxNearbyEntities, short minSpawnDelay, short maxSpawnDelay) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.delay = delay;
        this.spawnCount = spawnCount;
        this.requiredPlayerRange = requiredPlayerRange;
        this.maxNearbyEntities = maxNearbyEntities;
        this.minSpawnDelay = minSpawnDelay;
        this.maxSpawnDelay = maxSpawnDelay;
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
    }

    public static class Handler implements IMessageHandler<SyncSpawnerMessage, IMessage> {

        @Override
        public IMessage onMessage(SyncSpawnerMessage message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            World world = player.worldObj;

            if (world == null) return null;

            // Verify the block is a spawner
            if (world.getBlock(message.x, message.y, message.z) != Blocks.mob_spawner) return null;

            TileEntity tileEntity = world.getTileEntity(message.x, message.y, message.z);
            if (!(tileEntity instanceof TileEntityMobSpawner)) return null;

            TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileEntity;
            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);

            // Handle spawn range toggle (same logic as 1.12+)
            if (message.requiredPlayerRange == 0) nbt.setShort("SpawnRange", nbt.getShort("RequiredPlayerRange"));
            else nbt.setShort("SpawnRange", (short) 4);

            // Apply NBT values (same as 1.12+)
            nbt.setShort("Delay", (short) 0);
            nbt.setShort("SpawnCount", message.spawnCount);
            nbt.setShort("RequiredPlayerRange", message.requiredPlayerRange);
            nbt.setShort("MaxNearbyEntities", message.maxNearbyEntities);
            nbt.setShort("MinSpawnDelay", message.minSpawnDelay);
            nbt.setShort("MaxSpawnDelay", message.maxSpawnDelay);

            // Write back to spawner (equivalent to 1.12+'s logic.readNbt)
            spawner.readFromNBT(nbt);
            spawner.markDirty();
            world.markBlockForUpdate(message.x, message.y, message.z);

            // Damage the Spawner Key item
            ItemStack stack = player.getHeldItem();
            if (stack != null && stack.getItem() instanceof SpawnerKey) {
                stack.damageItem(1, player);
            }

            world.playAuxSFX(1004, message.x, message.y, message.z, 0);

            return null;
        }
    }
}
