package com.branders.spawnermod.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.config.ConfigValues;
import com.branders.spawnermod.network.SpawnerModNetwork;
import com.branders.spawnermod.network.packet.SyncSpawnerMessage;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Spawner configuration GUI screen.
 */
@SideOnly(Side.CLIENT)
public class SpawnerConfigGui extends GuiScreen {

    private static class Data {

        short LOW, DEFAULT, HIGH, HIGHEST;

        public Data(int i, int j, int k, int l) {
            LOW = (short) i;
            DEFAULT = (short) j;
            HIGH = (short) k;
            HIGHEST = (short) l;
        }
    }

    private static final Data DELAY = new Data(30, 20, 2, 1);
    private static final Data MIN_SPAWN_DELAY = new Data(300, 200, 25, 12);
    private static final Data MAX_SPAWN_DELAY = new Data(900, 800, 100, 25);
    private static final Data SPAWN_COUNT = new Data(2, 4, 24, 48);
    private static final Data MAX_NEARBY_ENTITIES = new Data(6, 6, 48, 96);
    private static final Data REQUIRED_PLAYER_RANGE = new Data(16, 32, 256, 512);

    private static final ResourceLocation SPAWNER_CONFIG_TEXTURE = new ResourceLocation(
        SpawnerMod.MOD_ID,
        "textures/gui/spawner_config_screen.png");
    private static final ResourceLocation SPAWNS_ICON_TEXTURE = new ResourceLocation(
        SpawnerMod.MOD_ID,
        "textures/gui/spawner_config_screen_icon_spawns.png");

    private GuiButton countButton;
    private GuiButton speedButton;
    // TODO: Range adjustment is currently broken, disabled temporarily
    // private GuiButton rangeButton;
    private GuiButton disableButton;

    private int countOptionValue;
    private int speedOptionValue;
    // TODO: Range adjustment disabled
    // private int rangeOptionValue;

    private short delay;
    private short minSpawnDelay;
    private short maxSpawnDelay;
    private short spawnCount;
    private short maxNearbyEntities;
    private short requiredPlayerRange;
    private boolean disabled;
    private short spawns;

    private final boolean cachedDisabled;
    private final boolean limitedSpawns;
    private final int x, y, z;
    private final TileEntityMobSpawner spawner;

    private boolean isCustomRange;
    private short customRange;

    public SpawnerConfigGui(TileEntityMobSpawner spawner, int x, int y, int z) {
        super();
        this.spawner = spawner;
        this.x = x;
        this.y = y;
        this.z = z;

        // TODO: Range adjustment disabled
        // if (ConfigValues.get("default_spawner_range_enabled") == 1) {
        // isCustomRange = true;
        // customRange = (short) ConfigValues.get("default_spawner_range");
        // }

        NBTTagCompound nbt = new NBTTagCompound();
        spawner.writeToNBT(nbt);

        delay = nbt.getShort("Delay");
        minSpawnDelay = nbt.getShort("MinSpawnDelay");
        maxSpawnDelay = nbt.getShort("MaxSpawnDelay");
        spawnCount = nbt.getShort("SpawnCount");
        maxNearbyEntities = nbt.getShort("MaxNearbyEntities");
        requiredPlayerRange = nbt.getShort("RequiredPlayerRange");

        short spawnRange = nbt.getShort("SpawnRange");
        if (spawnRange > 4) {
            disabled = true;
            cachedDisabled = true;
            requiredPlayerRange = spawnRange;
        } else {
            disabled = false;
            cachedDisabled = false;
        }

        countOptionValue = loadOptionState(spawnCount, SPAWN_COUNT);
        speedOptionValue = loadOptionState(minSpawnDelay, MIN_SPAWN_DELAY);
        // TODO: Range adjustment disabled
        // rangeOptionValue = loadOptionState(requiredPlayerRange, REQUIRED_PLAYER_RANGE);

        if (ConfigValues.get("limited_spawns_enabled") != 0) {
            limitedSpawns = true;
            if (nbt.hasKey("spawns")) {
                spawns = nbt.getShort("spawns");
                if (ConfigValues.get("limited_spawns_amount") - spawns <= 0) {
                    disabled = true;
                }
            }
        } else {
            limitedSpawns = false;
        }
    }

    @Override
    public void initGui() {
        int centerX = width / 2 - 48;

        countButton = new GuiButton(
            0,
            centerX,
            55,
            108,
            20,
            StatCollector.translateToLocal("button.count." + getButtonText(countOptionValue)));
        buttonList.add(countButton);

        speedButton = new GuiButton(
            1,
            centerX,
            80,
            108,
            20,
            StatCollector.translateToLocal("button.speed." + getButtonText(speedOptionValue)));
        buttonList.add(speedButton);

        // TODO: Range button disabled (range adjustment broken)
        // rangeButton = new GuiButton(
        // 2,
        // centerX,
        // 105,
        // 108,
        // 20,
        // StatCollector.translateToLocal("button.range." + getButtonText(rangeOptionValue)) + " "
        // + requiredPlayerRange);
        // buttonList.add(rangeButton);

        disableButton = new GuiButton(
            3,
            centerX,
            130,
            108,
            20,
            StatCollector.translateToLocal("button.toggle." + getButtonText(disabled)));
        buttonList.add(disableButton);

        buttonList
            .add(new GuiButton(4, width / 2 - 89, 180 + 10, 178, 20, StatCollector.translateToLocal("button.save")));
        buttonList
            .add(new GuiButton(5, width / 2 - 89, 180 + 35, 178, 20, StatCollector.translateToLocal("button.cancel")));

        // Ensure buttons are enabled by default (unless disabled by config)
        toggleButtons(true);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 0: // Count
                countOptionValue = (countOptionValue + 1) % 4;
                switch (countOptionValue) {
                    case 0:
                        spawnCount = SPAWN_COUNT.LOW;
                        maxNearbyEntities = MAX_NEARBY_ENTITIES.LOW;
                        break;
                    case 1:
                        spawnCount = SPAWN_COUNT.DEFAULT;
                        maxNearbyEntities = MAX_NEARBY_ENTITIES.DEFAULT;
                        break;
                    case 2:
                        spawnCount = SPAWN_COUNT.HIGH;
                        maxNearbyEntities = MAX_NEARBY_ENTITIES.HIGH;
                        break;
                    case 3:
                        spawnCount = SPAWN_COUNT.HIGHEST;
                        maxNearbyEntities = MAX_NEARBY_ENTITIES.HIGHEST;
                        break;
                }
                countButton.displayString = StatCollector
                    .translateToLocal("button.count." + getButtonText(countOptionValue));
                break;

            case 1: // Speed
                speedOptionValue = (speedOptionValue + 1) % 4;
                switch (speedOptionValue) {
                    case 0:
                        delay = DELAY.LOW;
                        minSpawnDelay = MIN_SPAWN_DELAY.LOW;
                        maxSpawnDelay = MAX_SPAWN_DELAY.LOW;
                        break;
                    case 1:
                        delay = DELAY.DEFAULT;
                        minSpawnDelay = MIN_SPAWN_DELAY.DEFAULT;
                        maxSpawnDelay = MAX_SPAWN_DELAY.DEFAULT;
                        break;
                    case 2:
                        delay = DELAY.HIGH;
                        minSpawnDelay = MIN_SPAWN_DELAY.HIGH;
                        maxSpawnDelay = MAX_SPAWN_DELAY.HIGH;
                        break;
                    case 3:
                        delay = DELAY.HIGHEST;
                        minSpawnDelay = MIN_SPAWN_DELAY.HIGHEST;
                        maxSpawnDelay = MAX_SPAWN_DELAY.HIGHEST;
                        break;
                }
                speedButton.displayString = StatCollector
                    .translateToLocal("button.speed." + getButtonText(speedOptionValue));
                break;

            // TODO: Range adjustment disabled
            // case 2: // Range
            // if (!isCustomRange) {
            // rangeOptionValue = (rangeOptionValue + 1) % 4;
            // } else {
            // if (rangeOptionValue >= 4) rangeOptionValue = 0;
            // rangeOptionValue = (rangeOptionValue + 1);
            // if (rangeOptionValue > 4) rangeOptionValue = 0;
            // }
            // switch (rangeOptionValue) {
            // case 0:
            // requiredPlayerRange = REQUIRED_PLAYER_RANGE.LOW;
            // break;
            // case 1:
            // requiredPlayerRange = REQUIRED_PLAYER_RANGE.DEFAULT;
            // break;
            // case 2:
            // requiredPlayerRange = REQUIRED_PLAYER_RANGE.HIGH;
            // break;
            // case 3:
            // requiredPlayerRange = REQUIRED_PLAYER_RANGE.HIGHEST;
            // break;
            // case 4:
            // requiredPlayerRange = customRange;
            // break;
            // }
            // rangeButton.displayString = StatCollector.translateToLocal(
            // "button.range." + getButtonText(rangeOptionValue)) + " " + requiredPlayerRange;
            // break;

            case 3: // Toggle
                disabled = !disabled;
                if (disabled) {
                    toggleButtons(false);
                    requiredPlayerRange = 0;
                } else {
                    toggleButtons(true);
                    // Restore default range when re-enabling
                    requiredPlayerRange = REQUIRED_PLAYER_RANGE.DEFAULT;
                }
                disableButton.displayString = StatCollector
                    .translateToLocal("button.toggle." + getButtonText(disabled));
                break;

            case 4: // Save
                configureSpawner();
                mc.displayGuiScreen(null);
                break;

            case 5: // Cancel
                mc.displayGuiScreen(null);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        // Draw background texture BEFORE drawing buttons
        mc.getTextureManager()
            .bindTexture(SPAWNER_CONFIG_TEXTURE);
        drawTexturedModalRect(width / 2 - 89, 5, 0, 0, 178, 177);

        // Draw title
        String title = StatCollector.translateToLocal("gui.spawnermod.spawner_config_screen_title");
        drawString(fontRendererObj, title, width / 2 - fontRendererObj.getStringWidth(title) / 2, 33, 0xFFD964);

        // Draw spawns icon if limited spawns enabled
        if (limitedSpawns) {
            mc.getTextureManager()
                .bindTexture(SPAWNS_ICON_TEXTURE);
            drawTexturedModalRect(width / 2 + 94, 23, 0, 0, 14, 14);
            String spawnsLeft = String.valueOf(ConfigValues.get("limited_spawns_amount") - spawns);
            drawString(fontRendererObj, spawnsLeft, width / 2 + 114, 27, 0xFFFFFF);
        }

        // Draw buttons AFTER background and title
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void configureSpawner() {
        if (cachedDisabled && disabled) return;

        SpawnerModNetwork.network.sendToServer(
            new SyncSpawnerMessage(
                x,
                y,
                z,
                delay,
                spawnCount,
                requiredPlayerRange,
                maxNearbyEntities,
                minSpawnDelay,
                maxSpawnDelay));
    }

    private String getButtonText(int optionValue) {
        switch (optionValue) {
            case 0:
                return "low";
            case 1:
                return "default";
            case 2:
                return "high";
            case 3:
                return "very_high";
            case 4:
                return "custom";
            default:
                return "default";
        }
    }

    private String getButtonText(boolean disabled) {
        return disabled ? "disabled" : "enabled";
    }

    private int loadOptionState(short current, Data reference) {
        if (isCustomRange && current == customRange) return 4;
        if (current == reference.LOW) return 0;
        else if (current == reference.DEFAULT) return 1;
        else if (current == reference.HIGH) return 2;
        else if (current == reference.HIGHEST) return 3;
        else return 0;
    }

    private void toggleButtons(boolean state) {
        if (ConfigValues.get("disable_count") != 0) {
            countButton.enabled = false;
            countButton.displayString = StatCollector.translateToLocal("button.count.disabled");
        } else {
            countButton.enabled = state;
        }

        if (ConfigValues.get("disable_speed") != 0) {
            speedButton.enabled = false;
            speedButton.displayString = StatCollector.translateToLocal("button.speed.disabled");
        } else {
            speedButton.enabled = state;
        }

        // TODO: Range button disabled
        // if (ConfigValues.get("disable_range") != 0) {
        // rangeButton.enabled = false;
        // rangeButton.displayString = StatCollector.translateToLocal("button.range.disabled");
        // } else {
        // rangeButton.enabled = state;
        // }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
