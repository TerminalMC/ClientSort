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
import dev.terminalmc.clientsort.client.config.Config.Options.ControlButtonType;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.config.ClassPolicy;
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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        public static final boolean deconflictKeybindsDefault = false;
        public boolean deconflictKeybinds = deconflictKeybindsDefault;

        public static final String editKeyDefault = InputConstants.UNKNOWN.getName();
        public String editKey = editKeyDefault;

        public static final String sortKeyDefault =
                Type.MOUSE.getOrCreate(InputConstants.MOUSE_BUTTON_MIDDLE).getName();
        public String sortKey = sortKeyDefault;

        public static final String stackFillKeyDefault = InputConstants.UNKNOWN.getName();
        public String stackFillKey = stackFillKeyDefault;

        public static final String transferKeyDefault = InputConstants.UNKNOWN.getName();
        public String transferKey = transferKeyDefault;

        // GUI options

        public static final boolean showButtonsDefault = false;
        public boolean showButtons = showButtonsDefault;

        public static final ControlButtonType firstButtonDefault = ControlButtonType.SORT;
        public ControlButtonType firstButton = firstButtonDefault;

        public static final ControlButtonType secondButtonDefault = ControlButtonType.STACK_FILL;
        public ControlButtonType secondButton = secondButtonDefault;

        public static final ControlButtonType thirdButtonDefault = ControlButtonType.TRANSFER;
        public ControlButtonType thirdButton = thirdButtonDefault;

        public enum ControlButtonType {
            SORT,
            STACK_FILL,
            TRANSFER
        }

        public static final Vec2i layoutOffsetDefault = new Vec2i(-4, 0);
        public Vec2i layoutOffset = layoutOffsetDefault;

        public static final boolean sortEnabledDefault = true;
        public boolean sortEnabled = sortEnabledDefault;

        public static final boolean stackFillEnabledDefault = true;
        public boolean stackFillEnabled = stackFillEnabledDefault;

        public static final boolean transferEnabledDefault = true;
        public boolean transferEnabled = transferEnabledDefault;

        public static final Supplier<List<ButtonLayout>> buttonLayoutsDefaultList = () -> List.of(
                new ButtonLayout(Inventory.class.getName(), null, true, true, true),
                new ButtonLayout(ChestMenu.class.getName(), null, null, null, null),
                new ButtonLayout(HopperMenu.class.getName(), null, false, false, null),
                new ButtonLayout(HorseInventoryMenu.class.getName(), null, null, null, null),
                new ButtonLayout(PlayerEnderChestContainer.class.getName(), null, null, null, null),
                new ButtonLayout(ShulkerBoxMenu.class.getName(), null, null, null, null),
                new ButtonLayout(
                        RandomizableContainerBlockEntity.class.getName(), null, null, null, null
                )
        );
        public static final Supplier<Map<String, ButtonLayout>> buttonLayoutsDefault = () -> {
            Map<String, ButtonLayout> map = new LinkedHashMap<>();
            buttonLayoutsDefaultList.get().forEach((layout) -> map.put(layout.className(), layout));
            return map;
        };
        public Map<String, ButtonLayout> buttonLayouts = buttonLayoutsDefault.get();

        // Policy options

        public static final boolean applyPoliciesDefault = true;
        public boolean applyPolicies = applyPoliciesDefault;

        public static final Supplier<List<ClassPolicy>> classPoliciesDefaultList = () -> List.of(
                new ClassPolicy(
                        "com.simibubi.create.content.equipment.toolbox.ToolboxMenu",
                        false, false, false
                )
        );
        public static final Supplier<Map<String, ClassPolicy>> classPoliciesDefault = () -> {
            Map<String, ClassPolicy> map = new LinkedHashMap<>();
            classPoliciesDefaultList.get().forEach((policy) -> map.put(policy.className, policy));
            return map;
        };
        public Map<String, ClassPolicy> classPolicies = classPoliciesDefault.get();
    }

    // Validation

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
        if (options.firstButton == null
                || options.firstButton.equals(options.secondButton)
                || options.firstButton.equals(options.thirdButton)
                || !Arrays.stream(ControlButtonType.values()).toList()
                .contains(options.firstButton)) {
            options.firstButton = Options.firstButtonDefault;
        }
        if (options.secondButton == null
                || options.secondButton.equals(options.firstButton)
                || options.secondButton.equals(options.thirdButton)
                || !Arrays.stream(ControlButtonType.values()).toList()
                .contains(options.secondButton)) {
            options.secondButton = Options.secondButtonDefault;
        }
        if (options.thirdButton == null
                || options.thirdButton.equals(options.firstButton)
                || options.thirdButton.equals(options.secondButton)
                || !Arrays.stream(ControlButtonType.values()).toList()
                .contains(options.thirdButton)) {
            options.thirdButton = Options.thirdButtonDefault;
        }
        // Sort the layouts by key for better UX
        Map<String, ButtonLayout> sortedLayouts = new LinkedHashMap<>();
        options.buttonLayouts.keySet()
                .stream()
                .sorted()
                .forEach((k) -> sortedLayouts.put(k, options.buttonLayouts.get(k)));
        options.buttonLayouts = sortedLayouts;
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
