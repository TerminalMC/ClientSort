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

package dev.terminalmc.clientsort.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.Config.Options.Operation;
import dev.terminalmc.clientsort.client.config.legacy.ButtonLayout;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Supplier;

public class Config {

    private static final Path CONFIG_DIR = Services.PLATFORM.getConfigDir();
    private static final String FILE_NAME = ClientSort.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = ClientSort.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {

        // General options

        public static final int INTERACTION_INTERVAL_MIN = 1;
        public static final int INTERACTION_INTERVAL_MAX = 100;
        public static final int interactionIntervalDefault = 10;
        public int interactionInterval = interactionIntervalDefault;

        public static final boolean useServerAccelerationDefault = true;
        public boolean useServerAcceleration = useServerAccelerationDefault;

        public static final boolean optimizeCreativeSortingDefault = true;
        public boolean optimizeCreativeSorting = optimizeCreativeSortingDefault;

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

        public static final boolean bundlesUseLeftClickDefault = false;
        public boolean bundlesUseLeftClick = bundlesUseLeftClickDefault;

        // Sort order options

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

        // Interaction sound options

        public static final boolean playSoundSortDefault = false;
        public boolean playSoundSort = playSoundSortDefault;

        public static final boolean playSoundOtherDefault = false;
        public boolean playSoundOther = playSoundOtherDefault;

        public static final String interactionSoundDefault = "minecraft:block.note_block.xylophone";
        public String interactionSound = interactionSoundDefault;
        public transient @Nullable ResourceLocation sortSoundLoc = null;

        public static final int SOUND_INTERVAL_MIN = 1;
        public static final int SOUND_INTERVAL_MAX = 100;
        public static final int soundIntervalDefault = 1;
        public int soundInterval = soundIntervalDefault;

        public static final float SOUND_PITCH_MIN = 0.5F;
        public static final float SOUND_PITCH_MAX = 2.0F;
        public static final float soundPitchMinDefault = 0.5F;
        public float soundPitchMin = soundPitchMinDefault;

        public static final float soundPitchMaxDefault = 2.0F;
        public float soundPitchMax = soundPitchMaxDefault;

        public static final float SOUND_VOLUME_MIN = 0.0F;
        public static final float SOUND_VOLUME_MAX = 1.0F;
        public static final float soundVolumeDefault = 0.2F;
        public float soundVolume = soundVolumeDefault;

        public static final boolean allowSoundOverlapDefault = true;
        public boolean allowSoundOverlap = allowSoundOverlapDefault;

        // Keybind options

        public static final boolean isolateKeybindsDefault = false;
        public boolean isolateKeybinds = isolateKeybindsDefault;

        public static final String editKeyDefault = InputConstants.UNKNOWN.getName();
        public String editKey = editKeyDefault;

        public static final String sortKeyDefault =
                Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_MIDDLE).getName();
        public String sortKey = sortKeyDefault;

        public static final String stackFillKeyDefault = InputConstants.UNKNOWN.getName();
        public String stackFillKey = stackFillKeyDefault;

        public static final String transferKeyDefault = InputConstants.UNKNOWN.getName();
        public String transferKey = transferKeyDefault;

        // Button options

        public static final boolean showButtonsDefault = false;
        public boolean showButtons = showButtonsDefault;

        public static final Operation firstButtonOpDefault = Operation.SORT;
        public Operation firstButtonOp = firstButtonOpDefault;

        public static final Operation secondButtonOpDefault = Operation.STACK_FILL;
        public Operation secondButtonOp = secondButtonOpDefault;

        public static final Operation thirdButtonOpDefault = Operation.TRANSFER;
        public Operation thirdButtonOp = thirdButtonOpDefault;

        public enum Operation {
            SORT,
            STACK_FILL,
            TRANSFER
        }

        public static final Vec2i layoutOffsetDefault = new Vec2i(-4, 0);
        public Vec2i layoutOffset = layoutOffsetDefault;

        // Policy options

        public static final Supplier<List<ClassPolicy>> classPoliciesDefaultList = () -> List.of(
                new ClassPolicy(
                        Inventory.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        ChestMenu.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        HopperMenu.class.getName(),
                        null,
                        Policy.KEYBIND,
                        Policy.KEYBIND,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        HorseInventoryMenu.class.getName(),
                        null,
                        Policy.NONE,
                        Policy.NONE,
                        Policy.NONE,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        PlayerEnderChestContainer.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        ShulkerBoxMenu.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        RandomizableContainerBlockEntity.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        "com.simibubi.create.content.equipment.toolbox.ToolboxMenu",
                        null,
                        Policy.NONE,
                        Policy.NONE,
                        Policy.NONE,
                        new TreeSet<>()
                )
        );
        public static final Supplier<Map<String, ClassPolicy>> classPoliciesDefault = () -> {
            Map<String, ClassPolicy> map = new LinkedHashMap<>();
            classPoliciesDefaultList.get().forEach((layout) -> map.put(layout.className(), layout));
            return map;
        };
        public Map<String, ClassPolicy> classPolicies = classPoliciesDefault.get();

        // Legacy from pre v2.0.0-beta.11
        public @Nullable Map<String, ButtonLayout> buttonLayouts;
    }

    // Validation

    /**
     * Updates legacy config fields.
     */
    private void upgradeLegacy() {
        // Legacy from pre v2.0.0-beta.11
        if (options.buttonLayouts != null && !options.buttonLayouts.isEmpty()) {
            // Upgrade old ButtonLayouts to new ClassPolicies
            options.buttonLayouts.values().forEach((bl) -> options.classPolicies.put(
                    bl.className(),
                    new ClassPolicy(
                            bl.className(),
                            bl.offset(),
                            Boolean.TRUE.equals(bl.sortEnabled())
                                    ? Policy.KEYBIND_BUTTON : Policy.KEYBIND,
                            Boolean.TRUE.equals(bl.stackFillEnabled())
                                    ? Policy.KEYBIND_BUTTON : Policy.KEYBIND,
                            Boolean.TRUE.equals(bl.transferEnabled())
                                    ? Policy.KEYBIND_BUTTON : Policy.KEYBIND,
                            new TreeSet<>()
                    )
            ));
            // Validate everything, including upgrading old ClassPolicies
            Map<String, ClassPolicy> classPoliciesNew = new LinkedHashMap<>();
            options.classPolicies.values().forEach((cp) -> {
                if (!cp.className().isBlank()) {
                    //noinspection ConstantValue
                    classPoliciesNew.put(
                            cp.className(), new ClassPolicy(
                                    cp.className(),
                                    cp.buttonOffset(),
                                    cp.sortPolicy() == null ? Policy.NONE : cp.sortPolicy(),
                                    cp.stackFillPolicy() == null
                                            ? Policy.NONE
                                            : cp.stackFillPolicy(),
                                    cp.transferPolicy() == null ? Policy.NONE : cp.transferPolicy(),
                                    cp.ignoredSlots() == null ? new TreeSet<>() : cp.ignoredSlots()
                            )
                    );
                }
            });
            options.classPolicies = classPoliciesNew;
        }
        options.buttonLayouts = null;
    }

    /**
     * Ensures that all config values are valid.
     */
    private void validate() {
        // Clamp numbered values
        options.interactionInterval = Math.clamp(
                options.interactionInterval,
                Options.INTERACTION_INTERVAL_MIN,
                Options.INTERACTION_INTERVAL_MAX
        );
        options.soundInterval = Math.clamp(
                options.soundInterval,
                Options.SOUND_INTERVAL_MIN,
                Options.SOUND_INTERVAL_MAX
        );
        options.soundPitchMin = Math.clamp(
                options.soundPitchMin,
                Options.SOUND_PITCH_MIN,
                Options.SOUND_PITCH_MAX
        );
        options.soundPitchMax = Math.clamp(
                options.soundPitchMax,
                options.soundPitchMin, // Not less than configured min
                Options.SOUND_PITCH_MAX
        );
        options.soundVolume = Math.clamp(
                options.soundVolume,
                Options.SOUND_VOLUME_MIN,
                Options.SOUND_VOLUME_MAX
        );
        // Validate ordering enum options
        if (options.firstButtonOp == null
                || options.firstButtonOp.equals(options.secondButtonOp)
                || options.firstButtonOp.equals(options.thirdButtonOp)
                || !Arrays.stream(Operation.values()).toList()
                .contains(options.firstButtonOp)) {
            options.firstButtonOp = Options.firstButtonOpDefault;
        }
        if (options.secondButtonOp == null
                || options.secondButtonOp.equals(options.firstButtonOp)
                || options.secondButtonOp.equals(options.thirdButtonOp)
                || !Arrays.stream(Operation.values()).toList()
                .contains(options.secondButtonOp)) {
            options.secondButtonOp = Options.secondButtonOpDefault;
        }
        if (options.thirdButtonOp == null
                || options.thirdButtonOp.equals(options.firstButtonOp)
                || options.thirdButtonOp.equals(options.secondButtonOp)
                || !Arrays.stream(Operation.values()).toList()
                .contains(options.thirdButtonOp)) {
            options.thirdButtonOp = Options.thirdButtonOpDefault;
        }
        // Sort the policies by key for better UX
        Map<String, ClassPolicy> sortedPolicies = new LinkedHashMap<>();
        options.classPolicies.keySet()
                .stream()
                .sorted()
                .forEach((k) -> sortedPolicies.put(k, options.classPolicies.get(k)));
        options.classPolicies = sortedPolicies;
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
                ClientSort.LOG.warn("Resetting config");
            } else {
                config.upgradeLegacy();
            }
        }
        return config != null ? config : new Config();
    }

    @SuppressWarnings("SameParameterValue")
    private static @Nullable Config load(Path file, Gson gson) {
        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file.toFile()),
                        StandardCharsets.UTF_8
                )
        ) {
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
            ClientSort.afterConfigSaved(instance);
        } catch (IOException e) {
            ClientSort.LOG.error("Unable to save config", e);
        }
    }
}
