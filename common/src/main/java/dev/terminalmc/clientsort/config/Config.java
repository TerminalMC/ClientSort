/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.clientsort.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.inventory.sort.SortMode;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = ClientSort.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = ClientSort.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {
        // General
        public static final int interactionRateServerDefault = 10;
        public int interactionRateServer = interactionRateServerDefault;

        public static final int interactionRateClientDefault = 1;
        public int interactionRateClient = interactionRateClientDefault;

        public static final HotbarMode hotbarModeDefault = HotbarMode.HARD;
        public HotbarMode hotbarMode = hotbarModeDefault;

        public static final ExtraSlotMode extraSlotModeDefault = ExtraSlotMode.NONE;
        public ExtraSlotMode extraSlotMode = extraSlotModeDefault;

        public enum HotbarMode {
            NONE,
            HARD,
            SOFT;

            public String lowerName() {
                return switch(this) {
                    case NONE -> "merge";
                    case HARD -> "split";
                    case SOFT -> "off";
                };
            }
        }

        public enum ExtraSlotMode {
            NONE,
            HOTBAR,
            INVENTORY;

            public String lowerName() {
                return switch(this) {
                    case NONE -> "none";
                    case HOTBAR -> "hotbar";
                    case INVENTORY -> "inventory";
                };
            }
        }

        public static final boolean lmbBundleDefault = false;
        public boolean lmbBundle = lmbBundleDefault;

        // Sorting
        public static final String sortModeDefault = SortMode.CREATIVE.name;
        public String sortModeStr = sortModeDefault;
        public transient SortMode sortMode;

        public static final String shiftSortModeDefault = SortMode.QUANTITY.name;
        public String shiftSortModeStr = shiftSortModeDefault;
        public transient SortMode shiftSortMode;

        public static final String ctrlSortModeDefault = SortMode.ALPHABET.name;
        public String ctrlSortModeStr = ctrlSortModeDefault;
        public transient SortMode ctrlSortMode;

        public static final String altSortModeDefault = SortMode.RAW_ID.name;
        public String altSortModeStr = altSortModeDefault;
        public transient SortMode altSortMode;

        public static final boolean optimizedCreativeSortingDefault = true;
        public boolean optimizedCreativeSorting = optimizedCreativeSortingDefault;
        
        // Sounds
        public static final boolean soundEnabledDefault = false;
        public boolean soundEnabled = soundEnabledDefault;
        
        public static final String sortSoundDefault = "minecraft:block.note_block.xylophone";
        public String sortSound = sortSoundDefault;
        public transient @Nullable ResourceLocation sortSoundLoc = null;
        
        public static final int soundRateDefault = 1;
        public int soundRate = soundRateDefault;
        
        public static final float soundMinPitchDefault = 0.5F;
        public float soundMinPitch = soundMinPitchDefault;

        public static final float soundMaxPitchDefault = 2.0F;
        public float soundMaxPitch = soundMaxPitchDefault;
        
        public static final float soundVolumeDefault = 0.2F;
        public float soundVolume = soundVolumeDefault;

        public static final boolean soundAllowOverlapDefault = true;
        public boolean soundAllowOverlap = soundAllowOverlapDefault;
    }

    // Cleanup

    private void cleanup() {
        // Called before config is saved
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                ClientSort.LOG.warn("Resetting config");
            }
        }
        return config != null ? config : new Config();
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            ClientSort.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup() {
        try {
            ClientSort.LOG.warn("Copying {} to {}", FILE_NAME, BACKUP_FILE_NAME);
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(file, backupFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8)) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            ClientSort.onConfigSaved(instance);
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to save config", e);
        }
    }
}
