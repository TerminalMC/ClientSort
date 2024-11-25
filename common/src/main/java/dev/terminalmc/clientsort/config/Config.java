/*
 * Copyright 2024 TerminalMC
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Config {
    private static final Path DIR_PATH = Path.of("config");
    private static final String FILE_NAME = ClientSort.MOD_ID + ".json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static class Options {
        // General
        public static final int defaultInteractionRateServer = 10;
        public int interactionRateServer = defaultInteractionRateServer;

        public static final int defaultInteractionRateClient = 1;
        public int interactionRateClient = defaultInteractionRateClient;

        public static final HotbarMode defaultHotbarMode = HotbarMode.HARD;
        public HotbarMode hotbarMode = defaultHotbarMode;

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

        public static final boolean defaultRmbBundle = false;
        public boolean rmbBundle = defaultRmbBundle;

        // Sorting
        public static final String defaultSortMode = SortMode.CREATIVE.name;
        public String sortModeStr = defaultSortMode;
        public transient SortMode sortMode;

        public static final String defaultShiftSortMode = SortMode.QUANTITY.name;
        public String shiftSortModeStr = defaultShiftSortMode;
        public transient SortMode shiftSortMode;

        public static final String defaultCtrlSortMode = SortMode.ALPHABET.name;
        public String ctrlSortModeStr = defaultCtrlSortMode;
        public transient SortMode ctrlSortMode;

        public static final String defaultAltSortMode = SortMode.RAW_ID.name;
        public String altSortModeStr = defaultAltSortMode;
        public transient SortMode altSortMode;

        public static final boolean defaultOptimizedCreativeSorting = true;
        public boolean optimizedCreativeSorting = defaultOptimizedCreativeSorting;
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
        }
        if (config == null) {
            config = new Config();
        }
        return config;
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (FileReader reader = new FileReader(file.toFile())) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            ClientSort.LOG.error("Unable to load config.", e);
            return null;
        }
    }

    public static void save() {
        if (instance == null) return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH)) Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

            try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(tempFile, file, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            ClientSort.onConfigSaved(instance);
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to save config.", e);
        }
    }
}
