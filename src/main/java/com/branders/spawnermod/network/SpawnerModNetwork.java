package com.branders.spawnermod.network;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.network.packet.SyncConfigMessage;
import com.branders.spawnermod.network.packet.SyncSpawnerMessage;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class SpawnerModNetwork {

    public static SimpleNetworkWrapper network;

    public static void registerMessages() {
        network = NetworkRegistry.INSTANCE.newSimpleChannel(SpawnerMod.MOD_ID);

        // Client to Server: Spawner config changes
        network.registerMessage(SyncSpawnerMessage.Handler.class, SyncSpawnerMessage.class, 0, Side.SERVER);

        // Server to Client: Config sync
        network.registerMessage(SyncConfigMessage.Handler.class, SyncConfigMessage.class, 1, Side.CLIENT);
    }

    /**
     * Send config sync to client
     */
    public static void sendConfigToClient(net.minecraft.entity.player.EntityPlayerMP player) {
        network.sendTo(
            new SyncConfigMessage(
                ConfigValues.get("disable_spawner_config"),
                ConfigValues.get("disable_count"),
                ConfigValues.get("disable_range"),
                ConfigValues.get("disable_speed"),
                ConfigValues.get("limited_spawns_enabled"),
                ConfigValues.get("limited_spawns_amount"),
                ConfigValues.get("default_spawner_range_enabled"),
                ConfigValues.get("default_spawner_range")),
            player);
    }
}
