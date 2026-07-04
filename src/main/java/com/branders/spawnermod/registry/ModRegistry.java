package com.branders.spawnermod.registry;

import net.minecraft.item.Item;

import com.branders.spawnermod.item.ItemDebugHarmPotion;
import com.branders.spawnermod.item.SpawnerKey;

import cpw.mods.fml.common.registry.GameRegistry;

public class ModRegistry {

    public static Item spawnerKey;
    public static Item debugHarmPotion;

    public static void register() {
        spawnerKey = new SpawnerKey().setCreativeTab(ModCreativeTab.INSTANCE);
        debugHarmPotion = new ItemDebugHarmPotion().setCreativeTab(ModCreativeTab.INSTANCE);

        GameRegistry.registerItem(spawnerKey, "spawner_key");
        GameRegistry.registerItem(debugHarmPotion, "debug_harm_potion");
    }
}
