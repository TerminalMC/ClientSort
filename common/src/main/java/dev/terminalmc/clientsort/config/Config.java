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
import dev.terminalmc.clientsort.inventory.sort.SortOrder;
import dev.terminalmc.clientsort.main.MainSort;
import dev.terminalmc.clientsort.platform.Services;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    private static final String FILE_NAME = MainSort.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = MainSort.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {
        
        // General options
        
        public static final int interactionRateMin = 1;
        public static final int interactionRateMax = 100;
        public static final int interactionRateServerDefault = 10;
        public int interactionRateServer = interactionRateServerDefault;

        public static final int interactionRateClientDefault = 1;
        public int interactionRateClient = interactionRateClientDefault;
        
        public static final boolean serverAcceleratedSortingDefault = true;
        public boolean serverAcceleratedSorting = serverAcceleratedSortingDefault;

        public static final boolean optimizedCreativeSortingDefault = true;
        public boolean optimizedCreativeSorting = optimizedCreativeSortingDefault;

        public static final HotbarScope hotbarScopeDefault = HotbarScope.HOTBAR;
        public HotbarScope hotbarScope = hotbarScopeDefault;
        public enum HotbarScope {
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final ExtraSlotScope extraSlotScopeDefault = ExtraSlotScope.EXTRA;
        public ExtraSlotScope extraSlotScope = extraSlotScopeDefault;
        public enum ExtraSlotScope {
            EXTRA,
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final boolean rmbBundleDefault = false;
        public boolean rmbBundle = rmbBundleDefault;

        // Sort mode options
        
        public static final String sortOrderDefault = SortOrder.CREATIVE.name;
        public String sortOrderStr = sortOrderDefault;
        public transient SortOrder sortOrder;

        public static final String shiftSortOrderDefault = SortOrder.QUANTITY.name;
        public String shiftSortOrderStr = shiftSortOrderDefault;
        public transient SortOrder shiftSortOrder;

        public static final String ctrlSortOrderDefault = SortOrder.ALPHABET.name;
        public String ctrlSortOrderStr = ctrlSortOrderDefault;
        public transient SortOrder ctrlSortOrder;

        public static final String altSortOrderDefault = SortOrder.RAW_ID.name;
        public String altSortOrderStr = altSortOrderDefault;
        public transient SortOrder altSortOrder;
        
        // Sorting sound options
        
        public static final boolean soundEnabledDefault = false;
        public boolean soundEnabled = soundEnabledDefault;
        
        public static final String sortSoundDefault = "minecraft:block.note_block.xylophone";
        public String sortSound = sortSoundDefault;
        public transient @Nullable ResourceLocation sortSoundLoc = null;
        
        public static final int soundRateDefault = 1;
        public int soundRate = soundRateDefault;

        public static final float soundPitchMin = 0.5F;
        public static final float soundPitchMax = 2.0F;
        public static final float soundMinPitchDefault = 0.5F;
        public float soundMinPitch = soundMinPitchDefault;

        public static final float soundMaxPitchDefault = 2.0F;
        public float soundMaxPitch = soundMaxPitchDefault;

        public static final float soundVolumeMin = 0.0F;
        public static final float soundVolumeMax = 1.0F;
        public static final float soundVolumeDefault = 0.2F;
        public float soundVolume = soundVolumeDefault;

        public static final boolean soundAllowOverlapDefault = true;
        public boolean soundAllowOverlap = soundAllowOverlapDefault;
        
        // Legacy from pre v1.3.0

        public static final HotbarMode hotbarModeDefault = HotbarMode.HARD;
        public HotbarMode hotbarMode = hotbarModeDefault;
        public enum HotbarMode {
            NONE,
            HARD,
            SOFT;

            public HotbarScope update() {
                return switch(this) {
                    case NONE -> HotbarScope.INVENTORY;
                    case HARD -> HotbarScope.HOTBAR;
                    case SOFT -> HotbarScope.NONE;
                };
            }
        }

        public static final ExtraSlotMode extraSlotModeDefault = ExtraSlotMode.NONE;
        public ExtraSlotMode extraSlotMode = extraSlotModeDefault;
        public enum ExtraSlotMode {
            NONE,
            HOTBAR,
            INVENTORY;

            public ExtraSlotScope update() {
                return switch(this) {
                    case NONE -> ExtraSlotScope.NONE;
                    case HOTBAR -> ExtraSlotScope.HOTBAR;
                    case INVENTORY -> ExtraSlotScope.INVENTORY;
                };
            }
        }
        
        public String sortModeStr = sortOrderDefault;
        public String shiftSortModeStr = shiftSortOrderDefault;
        public String ctrlSortModeStr = ctrlSortOrderDefault;
        public String altSortModeStr = altSortOrderDefault;
    }

    // Validation

    /**
     * Ensures that all config values are valid.
     */
    private void validate() {
        update();
        // interactionRateServer
        if (options.interactionRateServer < Options.interactionRateMin)
            options.interactionRateServer = Options.interactionRateMin;
        if (options.interactionRateServer > Options.interactionRateMax)
            options.interactionRateServer = Options.interactionRateMax;
        // interactionRateClient
        if (options.interactionRateClient < Options.interactionRateMin)
            options.interactionRateClient = Options.interactionRateMin;
        if (options.interactionRateClient > Options.interactionRateMax)
            options.interactionRateClient = Options.interactionRateMax;
        // soundRate
        if (options.soundRate < Options.interactionRateMin)
            options.soundRate = Options.interactionRateMin;
        if (options.soundRate > Options.interactionRateMax)
            options.soundRate = Options.interactionRateMax;
        // soundMinPitch
        if (options.soundMinPitch < Options.soundPitchMin)
            options.soundMinPitch = Options.soundPitchMin;
        if (options.soundMinPitch > Options.soundPitchMax)
            options.soundMinPitch = Options.soundPitchMax;
        // soundMaxPitch
        if (options.soundMaxPitch < Options.soundPitchMin)
            options.soundMaxPitch = Options.soundPitchMin;
        if (options.soundMaxPitch > Options.soundPitchMax)
            options.soundMaxPitch = Options.soundPitchMax;
        // also validate against min pitch
        if (options.soundMaxPitch < options.soundMinPitch)
            options.soundMaxPitch = options.soundMinPitch;
        // soundVolume
        if (options.soundVolume < Options.soundVolumeMin)
            options.soundVolume = Options.soundVolumeMin;
        if (options.soundVolume > Options.soundVolumeMax)
            options.soundVolume = Options.soundVolumeMax;
    }

    /**
     * Updates legacy (pre v1.3.0) config values.
     */
    private void update() {
        if (options.hotbarMode != Options.hotbarModeDefault) {
            options.hotbarScope = options.hotbarMode.update();
            options.hotbarMode = Options.hotbarModeDefault;
        }
        if (options.extraSlotMode != Options.extraSlotModeDefault) {
            options.extraSlotScope = options.extraSlotMode.update();
            options.extraSlotMode = Options.extraSlotModeDefault;
        }
        if (!Options.sortOrderDefault.equals(options.sortModeStr)) {
            options.sortOrderStr = options.sortModeStr;
            options.sortModeStr = Options.sortOrderDefault;
        }
        if (!Options.shiftSortOrderDefault.equals(options.shiftSortModeStr)) {
            options.shiftSortOrderStr = options.shiftSortModeStr;
            options.shiftSortModeStr = Options.shiftSortOrderDefault;
        }
        if (!Options.ctrlSortOrderDefault.equals(options.ctrlSortModeStr)) {
            options.ctrlSortOrderStr = options.ctrlSortModeStr;
            options.ctrlSortModeStr = Options.ctrlSortOrderDefault;
        }
        if (!Options.altSortOrderDefault.equals(options.altSortModeStr)) {
            options.altSortOrderStr = options.altSortModeStr;
            options.altSortModeStr = Options.altSortOrderDefault;
        }
    }
    
    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    @SuppressWarnings("unused")
    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = CONFIG_DIR.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                MainSort.LOG.warn("Resetting config");
            }
        }
        return config != null ? config : new Config();
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable Config load(Path file, Gson gson) {
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file.toFile()), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            MainSort.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup() {
        try {
            MainSort.LOG.warn("Copying {} to {}", FILE_NAME, BACKUP_FILE_NAME);
            if (!Files.isDirectory(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(file, backupFile, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MainSort.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.validate();
        try {
            if (!Files.isDirectory(CONFIG_DIR)) Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
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
            MainSort.LOG.error("Unable to save config", e);
        }
    }
}
