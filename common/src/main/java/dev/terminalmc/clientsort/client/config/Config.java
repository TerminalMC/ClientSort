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
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.legacy.ButtonLayout;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
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
        public static Validator<Integer> interactionIntervalValidator = (val) ->
                Mth.clamp(unbox(val), INTERACTION_INTERVAL_MIN, INTERACTION_INTERVAL_MAX);

        public static final boolean useServerAccelerationDefault = true;
        public boolean useServerAcceleration = useServerAccelerationDefault;

        public static final boolean optimizeCreativeSortingDefault = true;
        public boolean optimizeCreativeSorting = optimizeCreativeSortingDefault;

        public enum HotbarScope {
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final HotbarScope hotbarScopeDefault = HotbarScope.HOTBAR;
        public HotbarScope hotbarScope = hotbarScopeDefault;
        public static Validator<HotbarScope> hotbarScopeValidator = (val) ->
                val != null && Arrays.stream(HotbarScope.values()).toList().contains(val)
                        ? val : hotbarScopeDefault;

        public enum ExtraSlotScope {
            EXTRA,
            HOTBAR,
            INVENTORY,
            NONE
        }

        public static final ExtraSlotScope extraSlotScopeDefault = ExtraSlotScope.EXTRA;
        public ExtraSlotScope extraSlotScope = extraSlotScopeDefault;
        public static Validator<ExtraSlotScope> extraSlotScopeValidator = (val) ->
                val != null && Arrays.stream(ExtraSlotScope.values()).toList().contains(val)
                        ? val : extraSlotScopeDefault;


        public static final boolean bundlesUseLeftClickDefault = false;
        public boolean bundlesUseLeftClick = bundlesUseLeftClickDefault;

        public static final boolean alwaysMatchByTypeDefault = false;
        public boolean alwaysMatchByType = alwaysMatchByTypeDefault;

        public static final Supplier<List<String>> typeMatchTagsDefault = () -> List.of(
                "enchantable/weapon",
                "enchantable/mining",
                "enchantable/armor"
        );
        public List<String> typeMatchTags = typeMatchTagsDefault.get();
        public static Validator<List<String>> typeMatchTagsValidator = (val) -> val != null
                ? val : typeMatchTagsDefault.get();
        public transient final HashSet<Item> typeMatchItems = new HashSet<>();

        // Sort order options

        public static final String sortOrderStrDefault = SortOrder.CREATIVE.name;
        public String sortOrderStr = sortOrderStrDefault;
        public static Validator<String> sortOrderStrValidator = (val) -> val != null
                && SortOrder.SORT_ORDERS.containsKey(val) ? val : sortOrderStrDefault;
        public transient SortOrder sortOrder;

        public static final String shiftSortOrderStrDefault = SortOrder.QUANTITY.name;
        public String shiftSortOrderStr = shiftSortOrderStrDefault;
        public static Validator<String> shiftSortOrderStrValidator = (val) -> val != null
                && SortOrder.SORT_ORDERS.containsKey(val) ? val : shiftSortOrderStrDefault;
        public transient SortOrder shiftSortOrder;

        public static final String ctrlSortOrderStrDefault = SortOrder.ALPHABET.name;
        public String ctrlSortOrderStr = ctrlSortOrderStrDefault;
        public static Validator<String> ctrlSortOrderStrValidator = (val) -> val != null
                && SortOrder.SORT_ORDERS.containsKey(val) ? val : ctrlSortOrderStrDefault;
        public transient SortOrder ctrlSortOrder;

        public static final String altSortOrderStrDefault = SortOrder.RAW_ID.name;
        public String altSortOrderStr = altSortOrderStrDefault;
        public static Validator<String> altSortOrderStrValidator = (val) -> val != null
                && SortOrder.SORT_ORDERS.containsKey(val) ? val : altSortOrderStrDefault;
        public transient SortOrder altSortOrder;

        // Interaction sound options

        public static final boolean playSoundSortDefault = false;
        public boolean playSoundSort = playSoundSortDefault;

        public static final boolean playSoundOtherDefault = false;
        public boolean playSoundOther = playSoundOtherDefault;

        public static final String interactionSoundDefault = "minecraft:block.note_block.xylophone";
        public String interactionSound = interactionSoundDefault;
        public static Validator<String> interactionSoundValidator = (val) -> val != null
                && ResourceLocation.tryParse(val) != null ? val : interactionSoundDefault;
        public transient @Nullable ResourceLocation sortSoundLoc = null;

        public static final int SOUND_INTERVAL_MIN = 1;
        public static final int SOUND_INTERVAL_MAX = 100;
        public static final int soundIntervalDefault = 1;
        public int soundInterval = soundIntervalDefault;
        public static Validator<Integer> soundIntervalValidator = (val) ->
                Mth.clamp(unbox(val), SOUND_INTERVAL_MIN, SOUND_INTERVAL_MAX);

        public static final float SOUND_PITCH_MIN = 0.5F;
        public static final float SOUND_PITCH_MAX = 2.0F;
        public static final float soundPitchMinDefault = 0.5F;
        public float soundPitchMin = soundPitchMinDefault;
        public static AwareValidator<Float> soundPitchMinValidator = (val, options) -> Mth.clamp(
                unbox(val),
                SOUND_PITCH_MIN,
                Mth.clamp(options.soundPitchMax, SOUND_PITCH_MIN, SOUND_PITCH_MAX)
        );

        public static final float soundPitchMaxDefault = 2.0F;
        public float soundPitchMax = soundPitchMaxDefault;
        public static AwareValidator<Float> soundPitchMaxValidator = (val, options) -> Mth.clamp(
                unbox(val),
                Mth.clamp(options.soundPitchMin, SOUND_PITCH_MIN, SOUND_PITCH_MAX),
                SOUND_PITCH_MAX
        );

        public static final float SOUND_VOLUME_MIN = 0.0F;
        public static final float SOUND_VOLUME_MAX = 1.0F;
        public static final float soundVolumeDefault = 0.2F;
        public float soundVolume = soundVolumeDefault;
        public static Validator<Float> soundVolumeValidator = (val) ->
                Mth.clamp(unbox(val), SOUND_VOLUME_MIN, SOUND_VOLUME_MAX);

        public static final boolean allowSoundOverlapDefault = true;
        public boolean allowSoundOverlap = allowSoundOverlapDefault;

        // Keybind options

        public static final boolean isolateKeybindsDefault = true;
        public boolean isolateKeybinds = isolateKeybindsDefault;

        // Button options

        public static final boolean showButtonsDefault = false;
        public boolean showButtons = showButtonsDefault;

        private static Operation validateUniqueOp(
                @Nullable Operation val,
                @Nullable Operation... others
        ) {
            if (others.length >= Operation.values().length)
                throw new IllegalArgumentException();
            Set<Operation> ops = new HashSet<>(Arrays.stream(Operation.values()).toList());
            if (val != null && ops.contains(val) && !Arrays.stream(others).toList().contains(val)) {
                return val;
            } else {
                Arrays.stream(others).forEach(ops::remove);
                //noinspection OptionalGetWithoutIsPresent
                return ops.stream().findAny().get();
            }
        }

        public static final Operation firstButtonOpDefault = Operation.SORT;
        public Operation firstButtonOp = firstButtonOpDefault;
        public static AwareValidator<Operation> firstButtonOpValidator = (val, options) ->
                validateUniqueOp(
                        val,
                        options.secondButtonOp,
                        options.thirdButtonOp,
                        options.fourthButtonOp
                );

        public static final Operation secondButtonOpDefault = Operation.STACK_FILL;
        public Operation secondButtonOp = secondButtonOpDefault;
        public static AwareValidator<Operation> secondButtonOpValidator = (val, options) ->
                validateUniqueOp(
                        val,
                        options.firstButtonOp,
                        options.thirdButtonOp,
                        options.fourthButtonOp
                );

        public static final Operation thirdButtonOpDefault = Operation.MATCH_TRANSFER;
        public Operation thirdButtonOp = thirdButtonOpDefault;
        public static AwareValidator<Operation> thirdButtonOpValidator = (val, options) ->
                validateUniqueOp(
                        val,
                        options.firstButtonOp,
                        options.secondButtonOp,
                        options.fourthButtonOp
                );

        public static final Operation fourthButtonOpDefault = Operation.TRANSFER;
        public Operation fourthButtonOp = fourthButtonOpDefault;
        public static AwareValidator<Operation> fourthButtonOpValidator = (val, options) ->
                validateUniqueOp(
                        val,
                        options.firstButtonOp,
                        options.secondButtonOp,
                        options.thirdButtonOp
                );

        public static final Vec2i layoutOffsetDefault = new Vec2i(-4, 0);
        public Vec2i layoutOffset = layoutOffsetDefault;
        public static Validator<Vec2i> layoutOffsetValidator = (val) ->
                val != null ? val : layoutOffsetDefault;

        // Policy options

        public static final Supplier<List<ClassPolicy>> classPoliciesDefaultList = () -> List.of(
                new ClassPolicy(
                        Inventory.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
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
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        HopperMenu.class.getName(),
                        null,
                        Policy.KEYBIND,
                        Policy.KEYBIND,
                        Policy.KEYBIND_BUTTON,
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        HorseInventoryMenu.class.getName(),
                        null,
                        Policy.NONE,
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
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        ShulkerBoxMenu.class.getName(),
                        null,
                        Policy.KEYBIND_BUTTON,
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
                        Policy.KEYBIND_BUTTON,
                        new TreeSet<>()
                ),
                new ClassPolicy(
                        "com.simibubi.create.content.equipment.toolbox.ToolboxMenu",
                        null,
                        Policy.NONE,
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
        @SuppressWarnings("ConstantValue")
        public static Validator<Map<String, ClassPolicy>> classPoliciesValidator = (val) -> {
            Map<String, ClassPolicy> validPolicies = new LinkedHashMap<>();
            if (val == null)
                return validPolicies;
            val.values().forEach((cp) -> {
                if (cp != null && cp.className() != null && !cp.className().isBlank()) {
                    validPolicies.put(
                            cp.className(),
                            new ClassPolicy(
                                    cp.className(),
                                    cp.buttonOffset(),
                                    Options.policyValidator.validate(cp.sortPolicy()),
                                    Options.policyValidator.validate(cp.stackFillPolicy()),
                                    Options.policyValidator.validate(cp.matchTransferPolicy()),
                                    Options.policyValidator.validate(cp.transferPolicy()),
                                    cp.ignoredSlots() == null ? new TreeSet<>() : cp.ignoredSlots()
                            )
                    );
                }
            });
            // Sort the policies by key for better UX
            Map<String, ClassPolicy> sortedPolicies = new LinkedHashMap<>();
            validPolicies.keySet().stream().sorted()
                    .forEach((k) -> sortedPolicies.put(k, validPolicies.get(k)));
            return sortedPolicies;
        };
        public static Validator<Policy> policyValidator = (val) ->
                val != null && Arrays.stream(Policy.values()).toList().contains(val)
                        ? val : Policy.NONE;

        // Legacy from pre v2.0.0-beta.11
        public @Nullable Map<String, ButtonLayout> buttonLayouts;
    }

    // Utils

    private static int unbox(@Nullable Integer val) {
        return val != null ? val : 0;
    }

    private static float unbox(@Nullable Float val) {
        return val != null ? val : 0F;
    }

    // Validation

    @FunctionalInterface
    public interface Validator<T> {

        @NotNull T validate(@Nullable T obj);
    }

    @FunctionalInterface
    public interface AwareValidator<T> {

        @NotNull T validate(@Nullable T obj, @NotNull Options options);
    }

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
                            Policy.KEYBIND,
                            Boolean.TRUE.equals(bl.transferEnabled())
                                    ? Policy.KEYBIND_BUTTON : Policy.KEYBIND,
                            new TreeSet<>()
                    )
            ));
            // Validate everything, including upgrading old ClassPolicies
            options.classPolicies = Options.classPoliciesValidator.validate(options.classPolicies);
        }
        options.buttonLayouts = null;
    }

    /**
     * Ensures that all config values are valid.
     */
    private void validate() {
        options.interactionInterval =
                Options.interactionIntervalValidator.validate(options.interactionInterval);
        options.hotbarScope =
                Options.hotbarScopeValidator.validate(options.hotbarScope);
        options.extraSlotScope =
                Options.extraSlotScopeValidator.validate(options.extraSlotScope);
        options.typeMatchTags =
                Options.typeMatchTagsValidator.validate(options.typeMatchTags);
        options.sortOrderStr =
                Options.sortOrderStrValidator.validate(options.sortOrderStr);
        options.shiftSortOrderStr =
                Options.shiftSortOrderStrValidator.validate(options.shiftSortOrderStr);
        options.ctrlSortOrderStr =
                Options.ctrlSortOrderStrValidator.validate(options.ctrlSortOrderStr);
        options.altSortOrderStr =
                Options.altSortOrderStrValidator.validate(options.altSortOrderStr);
        options.interactionSound =
                Options.interactionSoundValidator.validate(options.interactionSound);
        options.soundInterval =
                Options.soundIntervalValidator.validate(options.soundInterval);
        options.soundPitchMin =
                Options.soundPitchMinValidator.validate(options.soundPitchMin, options);
        options.soundPitchMax =
                Options.soundPitchMaxValidator.validate(options.soundPitchMax, options);
        options.soundVolume =
                Options.soundVolumeValidator.validate(options.soundVolume);
        options.firstButtonOp =
                Options.firstButtonOpValidator.validate(options.firstButtonOp, options);
        options.secondButtonOp =
                Options.secondButtonOpValidator.validate(options.secondButtonOp, options);
        options.thirdButtonOp =
                Options.thirdButtonOpValidator.validate(options.thirdButtonOp, options);
        options.fourthButtonOp =
                Options.fourthButtonOpValidator.validate(options.fourthButtonOp, options);
        options.layoutOffset =
                Options.layoutOffsetValidator.validate(options.layoutOffset);
        options.classPolicies =
                Options.classPoliciesValidator.validate(options.classPolicies);
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
