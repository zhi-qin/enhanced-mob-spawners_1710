package com.branders.spawnermod.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.EntityList;

import com.branders.spawnermod.SpawnerMod;

/**
 * All mod config values are stored here.
 */
public class ConfigValues {

    private static HashMap<String, Integer> CONFIG_SPEC = new HashMap<String, Integer>();
    private static ArrayList<String> ITEM_ID_BLACKLIST = new ArrayList<String>();
    private static ArrayList<String> SPAWN_EGG_ENTITIES = new ArrayList<String>();

    public static void setDefaultConfigValues() {
        CONFIG_SPEC.put("monster_egg_drop_chance", 4);
        CONFIG_SPEC.put("disable_silk_touch", 0);
        CONFIG_SPEC.put("disable_spawner_config", 0);
        CONFIG_SPEC.put("disable_count", 0);
        CONFIG_SPEC.put("disable_range", 0);
        CONFIG_SPEC.put("disable_speed", 0);
        CONFIG_SPEC.put("limited_spawns_enabled", 0);
        CONFIG_SPEC.put("limited_spawns_amount", 32);
        CONFIG_SPEC.put("disable_egg_removal_from_spawner", 0);
        CONFIG_SPEC.put("monster_egg_only_drop_when_killed_by_player", 0);
        CONFIG_SPEC.put("default_spawner_range_enabled", 0);
        CONFIG_SPEC.put("default_spawner_range", 52);
        CONFIG_SPEC.put("spawner_hardness", 5);
        CONFIG_SPEC.put("display_item_id_from_right_click_in_log", 0);

        addSpawnEggs();
    }

    private static void addSpawnEggs() {
        // In 1.7.10, spawn eggs use damage values = entity ID
        // classToIDMapping is private in 1.7.10, so iterate IDtoClassMapping instead
        // First build a map from class -> entity name
        Map<Class, String> classToName = new HashMap<Class, String>();
        for (Object o : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry<String, Class> entry = (Map.Entry<String, Class>) o;
            classToName.put(entry.getValue(), entry.getKey());
        }

        // Then iterate IDtoClassMapping (public in 1.7.10) to get entity IDs
        for (Object o : EntityList.IDtoClassMapping.entrySet()) {
            Map.Entry<Integer, Class> entry = (Map.Entry<Integer, Class>) o;
            Integer id = entry.getKey();
            Class<?> entityClass = entry.getValue();
            String entityName = classToName.get(entityClass);
            if (entityName != null && EntityList.entityEggs.containsKey(id)) {
                CONFIG_SPEC.put(entityName, 0);
                SPAWN_EGG_ENTITIES.add(entityName);
            }
        }
    }

    public static void put(String key, int value) {
        CONFIG_SPEC.put(key, value);
    }

    public static int get(String key) {
        if (CONFIG_SPEC.containsKey(key)) return CONFIG_SPEC.get(key);
        else {
            SpawnerMod.LOGGER.warn("Key=" + key + " was not found when trying to access it! Returning 0");
            return 0;
        }
    }

    public static Set<String> getKeys() {
        return CONFIG_SPEC.keySet();
    }

    public static boolean isItemIdBlacklisted(String registryName) {
        return ITEM_ID_BLACKLIST.contains(registryName);
    }

    public static void blacklistItem(String registryName) {
        ITEM_ID_BLACKLIST.add(registryName);
    }

    public static Iterator<String> getBlacklistIds() {
        return ITEM_ID_BLACKLIST.iterator();
    }

    public static Iterator<String> getSpawnEggEntities() {
        return SPAWN_EGG_ENTITIES.iterator();
    }

    public static boolean isEggDisabled(String identifier) {
        if (get(identifier) == 0) return false;
        else return true;
    }
}
