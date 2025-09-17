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
import dev.terminalmc.clientsort.platform.Services;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ServerConfig {

    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    public static final String FILE_NAME = ClientSort.MOD_ID + "-server.json";
    private static final String BACKUP_FILE_NAME = ClientSort.MOD_ID + "-server.unreadable.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    // Options

    public final Options options = new Options();

    public static Options serverOptions() {
        return ServerConfig.get().options;
    }

    public static class Options {

        public static final boolean validateOperationResultsDefault = true;
        public boolean validateOperationResults = validateOperationResultsDefault;

        public static final Supplier<List<ServerClassPolicy>> classPoliciesDefaultList =
                () -> List.of(
                        new ServerClassPolicy(
                                "com.simibubi.create.content.equipment.toolbox.ToolboxMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tiviacz.travelersbackpack.inventory.menu.BackpackBaseMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tiviacz.travelersbackpack.inventory.menu.BackpackItemMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tiviacz.travelersbackpack.inventory.menu.BackpackBlockEntityMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tiviacz.travelersbackpack.inventory.menu.BackpackSettingsMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tom.storagemod.menu.CraftingTerminalMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "com.tom.storagemod.menu.StorageTerminalMenu",
                                false,
                                false,
                                false
                        ),
                        new ServerClassPolicy(
                                "fuzs.netherchested.world.inventory.NetherChestMenu",
                                false,
                                false,
                                false
                        )
                );
        public static final Supplier<Map<String, ServerClassPolicy>> classPoliciesDefault = () -> {
            Map<String, ServerClassPolicy> map = new LinkedHashMap<>();
            classPoliciesDefaultList.get().forEach((item) -> map.put(item.className, item));
            return map;
        };
        public Map<String, ServerClassPolicy> classPolicies = classPoliciesDefault.get();
        public static Validator<Map<String, ServerClassPolicy>> classPoliciesValidator = (val) -> {
            Map<String, ServerClassPolicy> validPolicies = new LinkedHashMap<>();
            if (val == null)
                return validPolicies;
            val.values().forEach((cp) -> {
                if (cp != null && cp.className != null && !cp.className.isBlank()) {
                    validPolicies.put(
                            cp.className,
                            new ServerClassPolicy(
                                    cp.className,
                                    cp.sortEnabled,
                                    cp.stackFillEnabled,
                                    cp.transferEnabled,
                                    cp.lastAutoEditTime,
                                    cp.lastAutoEditReason
                            )
                    );
                }
            });
            // Sort the policies by key for better UX
            Map<String, ServerClassPolicy> sortedPolicies = new LinkedHashMap<>();
            validPolicies.keySet().stream().sorted()
                    .forEach((k) -> sortedPolicies.put(k, validPolicies.get(k)));
            return sortedPolicies;
        };
    }

    // Validation

    // Validation

    @FunctionalInterface
    public interface Validator<T> {

        @NotNull T validate(@Nullable T obj);
    }

    /**
     * Ensures that all config values are valid.
     */
    private void validate() {
        options.classPolicies = Options.classPoliciesValidator.validate(options.classPolicies);
    }

    // Instance management

    private static ServerConfig instance = null;

    public static ServerConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static ServerConfig getAndSave() {
        get();
        save();
        return instance;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static ServerConfig reloadAndSave() {
        instance = load();
        save();
        return instance;
    }

    @SuppressWarnings("unused")
    public static ServerConfig resetAndSave() {
        instance = new ServerConfig();
        save();
        return instance;
    }

    // Load and save

    public static @NotNull ServerConfig load() {
        Path file = CONFIG_DIR.resolve(FILE_NAME);
        ServerConfig serverConfig = null;
        if (Files.exists(file)) {
            serverConfig = load(file, GSON);
            if (serverConfig == null) {
                backup();
                ClientSort.LOG.warn("Resetting config");
            }
        }
        return serverConfig != null ? serverConfig : new ServerConfig();
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable ServerConfig load(Path file, Gson gson) {
        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file.toFile()),
                        StandardCharsets.UTF_8
                )
        ) {
            return gson.fromJson(reader, ServerConfig.class);
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
            if (!Files.isDirectory(CONFIG_DIR))
                Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(
                    file,
                    backupFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null)
            return;
        instance.validate();
        try {
            if (!Files.isDirectory(CONFIG_DIR))
                Files.createDirectories(CONFIG_DIR);
            Path file = CONFIG_DIR.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(tempFile.toFile()),
                            StandardCharsets.UTF_8
                    )
            ) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
            ClientSort.onConfigSaved(instance);
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to save config", e);
        }
    }
}
