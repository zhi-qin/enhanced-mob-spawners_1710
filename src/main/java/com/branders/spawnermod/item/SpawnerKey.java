package com.branders.spawnermod.item;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.world.World;

import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.gui.SpawnerConfigGui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SpawnerKey extends Item {

    public SpawnerKey() {
        super();
        this.setMaxDamage(10);
        this.setMaxStackSize(1);
        this.setUnlocalizedName("spawner_key");
        this.setTextureName("spawnermod:spawner_key");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        if (ConfigValues.get("disable_spawner_config") != 0) {
            list.add("\u00a7cSpawner key is disabled");
        }
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {

        if (ConfigValues.get("disable_spawner_config") != 0) return false;

        // Leave if we didn't right click a spawner
        if (world.getBlock(x, y, z) != Blocks.mob_spawner) return false;

        // Only open GUI on client side
        if (world.isRemote) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            if (tileEntity instanceof TileEntityMobSpawner) {
                TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileEntity;
                openSpawnerGui(spawner, x, y, z);
            }
        }

        return true;
    }

    @SideOnly(Side.CLIENT)
    private void openSpawnerGui(TileEntityMobSpawner spawner, int x, int y, int z) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new SpawnerConfigGui(spawner, x, y, z));
    }
}
