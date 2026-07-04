package com.branders.spawnermod.registry;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

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

        // 合成配方：2个钻石 + 1个刷怪笼 → 1个刷怪笼钥匙
        GameRegistry.addShapedRecipe(
            new ItemStack(spawnerKey, 1),
            " D ",
            " D ",
            " S ",
            'D',
            Items.diamond,
            'S',
            Blocks.mob_spawner);
    }
}
