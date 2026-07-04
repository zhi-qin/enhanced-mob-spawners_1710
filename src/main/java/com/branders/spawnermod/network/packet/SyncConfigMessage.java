package com.branders.spawnermod.network.packet;

import com.branders.spawnermod.config.ConfigValues;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class SyncConfigMessage implements IMessage {

    private int disableSpawnerConfig;
    private int disableCount;
    private int disableRange;
    private int disableSpeed;
    private int limitedSpawnsEnabled;
    private int limitedSpawnsAmount;
    private int defaultSpawnerRangeEnabled;
    private int defaultSpawnerRange;

    public SyncConfigMessage() {}

    public SyncConfigMessage(int disableSpawnerConfig, int disableCount, int disableRange, int disableSpeed,
        int limitedSpawnsEnabled, int limitedSpawnsAmount, int defaultSpawnerRangeEnabled, int defaultSpawnerRange) {
        this.disableSpawnerConfig = disableSpawnerConfig;
        this.disableCount = disableCount;
        this.disableRange = disableRange;
        this.disableSpeed = disableSpeed;
        this.limitedSpawnsEnabled = limitedSpawnsEnabled;
        this.limitedSpawnsAmount = limitedSpawnsAmount;
        this.defaultSpawnerRangeEnabled = defaultSpawnerRangeEnabled;
        this.defaultSpawnerRange = defaultSpawnerRange;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        disableSpawnerConfig = buf.readInt();
        disableCount = buf.readInt();
        disableRange = buf.readInt();
        disableSpeed = buf.readInt();
        limitedSpawnsEnabled = buf.readInt();
        limitedSpawnsAmount = buf.readInt();
        defaultSpawnerRangeEnabled = buf.readInt();
        defaultSpawnerRange = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(disableSpawnerConfig);
        buf.writeInt(disableCount);
        buf.writeInt(disableRange);
        buf.writeInt(disableSpeed);
        buf.writeInt(limitedSpawnsEnabled);
        buf.writeInt(limitedSpawnsAmount);
        buf.writeInt(defaultSpawnerRangeEnabled);
        buf.writeInt(defaultSpawnerRange);
    }

    public static class Handler implements IMessageHandler<SyncConfigMessage, IMessage> {

        @Override
        public IMessage onMessage(SyncConfigMessage message, MessageContext ctx) {
            ConfigValues.put("disable_spawner_config", message.disableSpawnerConfig);
            ConfigValues.put("disable_count", message.disableCount);
            ConfigValues.put("disable_range", message.disableRange);
            ConfigValues.put("disable_speed", message.disableSpeed);
            ConfigValues.put("limited_spawns_enabled", message.limitedSpawnsEnabled);
            ConfigValues.put("limited_spawns_amount", message.limitedSpawnsAmount);
            ConfigValues.put("default_spawner_range_enabled", message.defaultSpawnerRangeEnabled);
            ConfigValues.put("default_spawner_range", message.defaultSpawnerRange);
            return null;
        }
    }
}
