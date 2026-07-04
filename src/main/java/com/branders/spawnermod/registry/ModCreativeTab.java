package com.branders.spawnermod.registry;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

import com.branders.spawnermod.SpawnerMod;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Custom creative tab for Enhanced Mob Spawners mod.
 */
public class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab INSTANCE = new ModCreativeTab();

    public ModCreativeTab() {
        super(SpawnerMod.MOD_ID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        // Use spawner key as the tab icon, fallback to spawn egg if not yet registered
        Item icon = ModRegistry.spawnerKey;
        return icon != null ? icon : net.minecraft.init.Items.spawn_egg;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public String getTranslatedTabLabel() {
        return "itemGroup." + SpawnerMod.MOD_ID;
    }
}
