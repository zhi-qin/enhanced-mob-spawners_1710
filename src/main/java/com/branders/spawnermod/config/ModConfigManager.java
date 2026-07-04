package com.branders.spawnermod.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import com.branders.spawnermod.SpawnerMod;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * Simple config manager using a spawnermod.json file.
 */
public class ModConfigManager {

    static class Pair<L, R> {

        private final L left;
        private final R right;

        public Pair(L left, R right) {
            assert left != null;
            assert right != null;
            this.left = left;
            this.right = right;
        }

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }
    }

    private static File file;
    public static final Gson GSON = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setPrettyPrinting()
        .create();

    public static void initConfig(File configDir) {
        ConfigValues.setDefaultConfigValues();
        file = new File(configDir, "spawnermod.json");

        if (!file.exists()) {
            SpawnerMod.LOGGER.info("Could not find config, generating new default config.");
            saveConfigToFile();
        } else {
            SpawnerMod.LOGGER.info("Reading config values from file.");
            readConfigFromFile();
        }
    }

    private static void readConfigFromFile() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            JsonObject json = new JsonParser().parse(reader)
                .getAsJsonObject();

            Pair<JsonObject, Boolean> validConfig = validateConfig(json);
            JsonObject entities = json.getAsJsonObject("disable_specific_egg_drops");

            for (String key : ConfigValues.getKeys()) {
                if (json.get(key) != null) ConfigValues.put(
                    key,
                    json.get(key)
                        .getAsInt());
                else if (entities != null && entities.get(key) != null) ConfigValues.put(
                    key,
                    entities.get(key)
                        .getAsInt());
                else SpawnerMod.LOGGER.warn("Key error: Could not find key: " + key);
            }

            JsonArray blacklist = json.getAsJsonArray("item_id_blacklist");
            if (blacklist != null) {
                for (JsonElement elem : blacklist) {
                    try {
                        String name = elem.getAsString();
                        ConfigValues.blacklistItem(name);
                    } catch (Exception e) {
                        SpawnerMod.LOGGER.warn("Failed to read element inside blacklist array!");
                    }
                }
            }

            if (validConfig.getRight()) {
                SpawnerMod.LOGGER.info("Config was broken. Saving new config which is fixed!");
                saveConfigToFile();
            }

            reader.close();
        } catch (IOException e) {
            SpawnerMod.LOGGER.warn("Could not read config file.");
            e.printStackTrace();
        }
    }

    public static void saveConfigToFile() {
        JsonObject config = new JsonObject();
        Object[] keys = ConfigValues.getKeys()
            .toArray();
        Arrays.sort(keys);

        JsonObject entities = new JsonObject();
        for (Object key : keys) {
            String keyStr = (String) key;
            if (keyStr.matches("\\w+:\\w+") || keyStr.matches("\\w+")) {
                // Check if it looks like an entity name (alphanumeric, no namespace separator)
                if (keyStr.indexOf(':') > 0 || Character.isUpperCase(keyStr.charAt(0)))
                    entities.addProperty(keyStr, ConfigValues.get(keyStr));
                else config.addProperty(keyStr, ConfigValues.get(keyStr));
            } else {
                config.addProperty(keyStr, ConfigValues.get(keyStr));
            }
        }

        if (entities.entrySet()
            .size() > 0) {
            config.add("disable_specific_egg_drops", entities);
        }

        JsonArray blacklist = new JsonArray();
        Iterator<String> it = ConfigValues.getBlacklistIds();
        while (it.hasNext()) {
            blacklist.add(new JsonPrimitive(it.next()));
        }
        config.add("item_id_blacklist", blacklist);

        String jsonConfig = GSON.toJson(config);

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(jsonConfig);
            writer.close();
        } catch (IOException e) {
            SpawnerMod.LOGGER.warn("Could not save config file.");
            e.printStackTrace();
        }
    }

    private static Pair<JsonObject, Boolean> validateConfig(JsonObject json) {
        boolean brokenConfig = false;

        if (json.getAsJsonObject("disable_specific_egg_drops") == null) {
            SpawnerMod.LOGGER.info("Broken config. Group key=disable_specific_egg_drops was not found.");
            JsonObject entities = new JsonObject();
            for (Object k : ConfigValues.getKeys()
                .toArray()) {
                String key = (String) k;
                if (Character.isUpperCase(key.charAt(0))) entities.addProperty(key, 0);
            }
            json.add("disable_specific_egg_drops", entities);
        }

        if (json.getAsJsonArray("item_id_blacklist") == null) {
            json.add("item_id_blacklist", new JsonArray());
            brokenConfig = true;
        }

        for (String key : ConfigValues.getKeys()) {
            JsonElement elem = json.get(key);
            if (elem == null) {
                JsonObject eggDrops = json.getAsJsonObject("disable_specific_egg_drops");
                if (eggDrops != null) elem = eggDrops.get(key);
            }

            if (elem == null) {
                SpawnerMod.LOGGER.info("Broken config. Key=" + key + " was not found. Adding it ...");
                brokenConfig = true;
                json.addProperty(key, ConfigValues.get(key));
            }
        }

        return new Pair<JsonObject, Boolean>(json, brokenConfig);
    }
}
