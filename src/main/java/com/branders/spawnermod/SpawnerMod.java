package com.branders.spawnermod;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.MinecraftForge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.branders.spawnermod.command.SpawnerModCommands;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.config.ModConfigManager;
import com.branders.spawnermod.entity.EntityDebugHarmPotion;
import com.branders.spawnermod.event.SpawnerEventHandler;
import com.branders.spawnermod.network.SpawnerModNetwork;
import com.branders.spawnermod.proxy.CommonProxy;
import com.branders.spawnermod.registry.ModRegistry;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;

@Mod(modid = SpawnerMod.MOD_ID, name = SpawnerMod.NAME, version = SpawnerMod.VERSION)
public class SpawnerMod {

    public static final String MOD_ID = "spawnermod";
    public static final String NAME = "Enhanced Mob Spawners";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Instance(MOD_ID)
    public static SpawnerMod instance;

    @SidedProxy(
        clientSide = "com.branders.spawnermod.proxy.ClientProxy",
        serverSide = "com.branders.spawnermod.proxy.CommonProxy")
    public static CommonProxy proxy;

    public static final SpawnerEventHandler eventHandler = new SpawnerEventHandler();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Enhanced Mob Spawners loading...");

        // Load config
        ModConfigManager.initConfig(event.getModConfigurationDirectory());

        // Register network
        SpawnerModNetwork.registerMessages();

        // Register items
        ModRegistry.register();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(eventHandler);
        // Note: SpawnerModCommands is registered via serverStart event, no need to register on EVENT_BUS

        // Set custom hardness for spawner block via reflection.
        // ReflectionHelper tries both the MCP name (dev) and the SRG name
        // (reobfuscated production jars), otherwise the field lookup silently
        // fails at runtime on real installs.
        try {
            java.lang.reflect.Field hardnessField = ReflectionHelper
                .findField(Block.class, "blockHardness", "field_149782_v");
            float customHardness = (float) ConfigValues.get("spawner_hardness");
            hardnessField.setFloat(Blocks.mob_spawner, customHardness);
            LOGGER.info("Set spawner block hardness to " + customHardness);
        } catch (Exception e) {
            LOGGER.warn("Could not set spawner block hardness: " + e.getMessage());
        }

        // Register entity
        EntityRegistry
            .registerModEntity(EntityDebugHarmPotion.class, "debug_harm_potion", 1, SpawnerMod.instance, 64, 10, true);

        proxy.registerRenderers();
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {}

    @EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new SpawnerModCommands());
    }
}
