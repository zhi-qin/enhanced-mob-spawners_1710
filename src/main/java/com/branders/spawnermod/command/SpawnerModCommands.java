package com.branders.spawnermod.command;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MovingObjectPosition;

import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.config.ModConfigManager;

public class SpawnerModCommands extends CommandBase {

    private List<String> aliases = new ArrayList<String>();

    public SpawnerModCommands() {
        aliases.add("ems");
    }

    @Override
    public String getCommandName() {
        return "ems";
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/ems <key> [value] | /ems check | /ems setspawnrange <1-128>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }

        // /ems check - inspect the spawner the player is looking at
        if (args[0].equals("check")) {
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: Only players can use this command"));
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            MovingObjectPosition mop = player.rayTrace(8.0D, 1.0F);

            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: You are not looking at a block"));
                return;
            }

            int x = mop.blockX;
            int y = mop.blockY;
            int z = mop.blockZ;

            if (player.worldObj.getBlock(x, y, z) != Blocks.mob_spawner) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: You are not looking at a mob spawner"));
                return;
            }

            TileEntity te = player.worldObj.getTileEntity(x, y, z);
            if (!(te instanceof TileEntityMobSpawner)) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: Failed to read spawner data"));
                return;
            }

            NBTTagCompound nbt = new NBTTagCompound();
            ((TileEntityMobSpawner) te).writeToNBT(nbt);

            String entityName = nbt.getString("EntityId");
            if (entityName == null || entityName.isEmpty()) {
                entityName = "None";
            }

            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "===== Spawner Data ====="));
            sender.addChatMessage(
                new ChatComponentText(EnumChatFormatting.AQUA + "Entity: " + EnumChatFormatting.WHITE + entityName));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "SpawnCount: " + EnumChatFormatting.WHITE + nbt.getShort("SpawnCount")));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "MinSpawnDelay: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("MinSpawnDelay")
                        + " ticks"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "MaxSpawnDelay: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("MaxSpawnDelay")
                        + " ticks"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "MaxNearbyEntities: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("MaxNearbyEntities")));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "RequiredPlayerRange: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("RequiredPlayerRange")
                        + " blocks"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "SpawnRange: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("SpawnRange")
                        + " blocks"));
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.AQUA + "Delay: "
                        + EnumChatFormatting.WHITE
                        + nbt.getShort("Delay")
                        + " ticks remaining"));
            return;
        }

        // /ems setspawnrange <1-128> - set the vanilla spawn area (SpawnRange)
        // of the spawner the player is looking at. A bigger SpawnRange lets more
        // mobs spawn per wave (they are spread over a larger area).
        if (args[0].equals("setspawnrange")) {
            if (!(sender instanceof EntityPlayer)) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: Only players can use this command"));
                return;
            }
            if (args.length < 2) {
                throw new WrongUsageException(getCommandUsage(sender));
            }

            int range;
            try {
                range = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            if (range < 1 || range > 128) {
                sender.addChatMessage(
                    new ChatComponentText(
                        EnumChatFormatting.RED + "[EMS]: SpawnRange must be between 1 and 128, got " + range));
                return;
            }

            EntityPlayer player = (EntityPlayer) sender;
            MovingObjectPosition mop = player.rayTrace(8.0D, 1.0F);
            if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: You are not looking at a block"));
                return;
            }

            int x = mop.blockX;
            int y = mop.blockY;
            int z = mop.blockZ;

            if (player.worldObj.getBlock(x, y, z) != Blocks.mob_spawner) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: You are not looking at a mob spawner"));
                return;
            }

            TileEntity te = player.worldObj.getTileEntity(x, y, z);
            if (!(te instanceof TileEntityMobSpawner)) {
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.RED + "[EMS]: Failed to read spawner data"));
                return;
            }

            TileEntityMobSpawner spawner = (TileEntityMobSpawner) te;
            NBTTagCompound nbt = new NBTTagCompound();
            spawner.writeToNBT(nbt);
            nbt.setShort("SpawnRange", (short) range);
            spawner.readFromNBT(nbt);
            spawner.markDirty();
            player.worldObj.markBlockForUpdate(x, y, z);

            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.GREEN + "[EMS]: SpawnRange set to "
                        + range
                        + " (spawn area "
                        + (range * 2 + 1)
                        + "x"
                        + (range * 2 + 1)
                        + ")"));
            return;
        }

        if (args[0].equals("reset")) {
            ConfigValues.setDefaultConfigValues();
            ModConfigManager.saveConfigToFile();
            sender.addChatMessage(new ChatComponentText(EnumChatFormatting.GREEN + "[EMS]: Config reset to default"));
            return;
        }

        if (args.length == 1) {
            // Print current value
            int value = ConfigValues.get(args[0]);
            sender.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.YELLOW + "[EMS]: " + args[0] + " is currently set to " + value));
            return;
        }

        if (args.length == 2) {
            try {
                int value = Integer.parseInt(args[1]);
                ConfigValues.put(args[0], value);
                ModConfigManager.saveConfigToFile();
                sender.addChatMessage(
                    new ChatComponentText(EnumChatFormatting.GREEN + "[EMS]: " + args[0] + " updated to " + value));
            } catch (NumberFormatException e) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
        }
    }

}
