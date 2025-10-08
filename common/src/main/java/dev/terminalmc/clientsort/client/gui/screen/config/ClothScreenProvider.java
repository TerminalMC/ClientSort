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

package dev.terminalmc.clientsort.client.gui.screen.config;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Config.Options;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.order.CreativeSearchOrder;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import dev.terminalmc.clientsort.mixin.client.accessor.KeyMappingAccessor;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.EnumListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.text.ParseException;
import java.util.*;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ClothScreenProvider {

    // Mild shenanigans to allow cross-validation between selectors

    private static EnumListEntry<?> firstSelector = null;
    private static EnumListEntry<?> secondSelector = null;
    private static EnumListEntry<?> thirdSelector = null;
    private static EnumListEntry<?> fourthSelector = null;

    /**
     * Builds and returns a Cloth Config options screen.
     *
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.options();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("name"))
                .setSavingRunnable(Config::getAndSave);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(localized("option", "general"));

        general.addEntry(eb.startIntField(
                        localized("option", "interactionInterval"),
                        options.interactionInterval
                )
                .setTooltip(localized("option", "interactionInterval.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.INTERACTION_INTERVAL_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.INTERACTION_INTERVAL_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionIntervalDefault)
                .setSaveConsumer(val -> options.interactionInterval = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "useServerAcceleration"),
                        options.useServerAcceleration
                )
                .setTooltip(localized("option", "useServerAcceleration.tooltip"))
                .setDefaultValue(Config.Options.useServerAccelerationDefault)
                .setSaveConsumer(val -> options.useServerAcceleration = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "useClientFallback"),
                        options.useClientFallback
                )
                .setTooltip(localized("option", "useClientFallback.tooltip"))
                .setDefaultValue(Options.useClientFallbackDefault)
                .setSaveConsumer(val -> options.useClientFallback = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "optimizeCreativeSorting"),
                        options.optimizeCreativeSorting
                )
                .setTooltip(localized("option", "optimizeCreativeSorting.tooltip"))
                .setDefaultValue(Config.Options.optimizeCreativeSortingDefault)
                .setSaveConsumer(val -> {
                    options.optimizeCreativeSorting = val;
                    if (val)
                        CreativeSearchOrder.tryRefreshStackPositionMap();
                })
                .build());

        general.addEntry(eb.startEnumSelector(
                        localized("option", "hotbarScope"),
                        Config.Options.HotbarScope.class,
                        options.hotbarScope
                )
                .setEnumNameProvider(val -> localized("hotbarScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.hotbarScopeDefault)
                .setSaveConsumer(val -> options.hotbarScope = val)
                .build());

        general.addEntry(eb.startEnumSelector(
                        localized("option", "extraSlotScope"),
                        Config.Options.ExtraSlotScope.class,
                        options.extraSlotScope
                )
                .setEnumNameProvider(val -> localized("extraSlotScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.extraSlotScopeDefault)
                .setSaveConsumer(val -> options.extraSlotScope = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "bundlesUseRightClick"),
                        options.bundlesUseRightClick
                )
                .setTooltip(localized("option", "bundlesUseRightClick.tooltip"))
                .setDefaultValue(Config.Options.bundlesUseRightClickDefault)
                .setSaveConsumer(val -> {
                    options.bundlesUseRightClick = val;
                    if (val)
                        CreativeSearchOrder.tryRefreshStackPositionMap();
                })
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "showDebugInfo"),
                        dev.terminalmc.clientsort.ClientSort.debugEnabled
                )
                .setTooltip(localized("option", "showDebugInfo.tooltip"))
                .setDefaultValue(false)
                .setSaveConsumer(val -> dev.terminalmc.clientsort.ClientSort.debugEnabled = val)
                .build());

        ConfigCategory sort = builder.getOrCreateCategory(localized("option", "sorting"));

        sort.addEntry(eb.startSelector(
                        localized("option", "sortOrder"),
                        SortOrder.SORT_ORDERS.keySet().toArray(),
                        options.sortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.sortOrderStrDefault)
                .setSaveConsumer(val -> options.sortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "shiftSortOrder"),
                        SortOrder.SORT_ORDERS.keySet().toArray(),
                        options.shiftSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.shiftSortOrderStrDefault)
                .setSaveConsumer(val -> options.shiftSortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "ctrlSortOrder"),
                        SortOrder.SORT_ORDERS.keySet().toArray(),
                        options.ctrlSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.ctrlSortOrderStrDefault)
                .setSaveConsumer(val -> options.ctrlSortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "altSortOrder"),
                        SortOrder.SORT_ORDERS.keySet().toArray(),
                        options.altSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.altSortOrderStrDefault)
                .setSaveConsumer(val -> options.altSortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startTextDescription(localized("option", "orderOverrides")).build());

        sort.addEntry(eb.startBooleanToggle(
                        localized("option", "useStartOverrides"),
                        options.useStartOverrides
                )
                .setTooltip(localized("option", "useStartOverrides.tooltip"))
                .setDefaultValue(Options.useStartOverridesDefault)
                .setSaveConsumer(val -> options.useStartOverrides = val)
                .build());

        sort.addEntry(eb.startStrList(
                        localized("option", "startOverrideItems"),
                        options.startOverrideItems
                )
                .setTooltip(localized("option", "startOverrideItems.tooltip").append("\n")
                        .append(localized(
                                "option",
                                "overrideItems.tooltip.2",
                                localized("option", "overrideItems.tooltip.2.item").withStyle(
                                        ChatFormatting.GOLD)
                        )))
                .setDefaultValue(Options.startOverrideItemsDefault.get())
                .setSaveConsumer(val -> options.startOverrideItems =
                        val.stream().map(String::strip).filter((s) -> !s.isBlank()).toList())
                .setInsertInFront(true)
                .setExpanded(options.useStartOverrides)
                .build());

        sort.addEntry(eb.startBooleanToggle(
                        localized("option", "useEndOverrides"),
                        options.useEndOverrides
                )
                .setTooltip(localized("option", "useEndOverrides.tooltip"))
                .setDefaultValue(Options.useEndOverridesDefault)
                .setSaveConsumer(val -> options.useEndOverrides = val)
                .build());

        sort.addEntry(eb.startStrList(
                        localized("option", "endOverrideItems"),
                        options.endOverrideItems
                )
                .setTooltip(localized("option", "endOverrideItems.tooltip").append("\n")
                        .append(localized(
                                "option",
                                "overrideItems.tooltip.2",
                                localized("option", "overrideItems.tooltip.2.item").withStyle(
                                        ChatFormatting.GOLD)
                        )))
                .setDefaultValue(Options.endOverrideItemsDefault.get())
                .setSaveConsumer(val -> options.endOverrideItems =
                        val.stream().map(String::strip).filter((s) -> !s.isBlank()).toList())
                .setInsertInFront(true)
                .setExpanded(options.useEndOverrides)
                .build());

        ConfigCategory matching = builder.getOrCreateCategory(localized("option", "matching"));

        matching.addEntry(eb.startBooleanToggle(
                        localized("option", "alwaysMatchByType"),
                        options.alwaysMatchByType
                )
                .setTooltip(localized("option", "alwaysMatchByType.tooltip"))
                .setDefaultValue(Config.Options.alwaysMatchByTypeDefault)
                .setSaveConsumer(val -> options.alwaysMatchByType = val)
                .build());

        matching.addEntry(eb.startStrList(
                        localized("option", "typeMatchTags"),
                        options.typeMatchTags
                )
                .setTooltip(localized("option", "typeMatchTags.tooltip.1")
                        .append("\n")
                        .append(localized("option", "typeMatchTags.tooltip.2"))
                        .append("\n")
                        .append(localized(
                                "option",
                                "typeMatchTags.tooltip.3",
                                Component.literal("https://minecraft.wiki/w/Item_tag")
                                        .withStyle(ChatFormatting.GOLD)
                        )))
                .setDefaultValue(Config.Options.typeMatchTagsDefault.get())
                .setSaveConsumer(val -> options.typeMatchTags =
                        val.stream().map(String::strip).filter((s) -> !s.isBlank()).toList())
                .setInsertInFront(true)
                .setExpanded(!options.alwaysMatchByType)
                .build());

        ConfigCategory sounds = builder.getOrCreateCategory(localized("option", "sounds"));

        sounds.addEntry(eb.startBooleanToggle(
                        localized("option", "playSoundSort"),
                        options.playSoundSort
                )
                .setTooltip(localized("option", "playSoundSort.tooltip"))
                .setDefaultValue(Config.Options.playSoundSortDefault)
                .setSaveConsumer(val -> options.playSoundSort = val)
                .build());

        sounds.addEntry(eb.startBooleanToggle(
                        localized("option", "playSoundOther"),
                        options.playSoundOther
                )
                .setTooltip(localized("option", "playSoundOther.tooltip"))
                .setDefaultValue(Config.Options.playSoundOtherDefault)
                .setSaveConsumer(val -> options.playSoundOther = val)
                .build());

        sounds.addEntry(eb.startStrField(
                        localized("option", "interactionSound"),
                        options.interactionSound
                )
                .setDefaultValue(Config.Options.interactionSoundDefault)
                .setSaveConsumer(val -> options.interactionSound = val)
                .setErrorSupplier(val -> {
                    if (ResourceLocation.tryParse(val) == null)
                        return Optional.of(localized("error", "resourceLocation.parse"));
                    else
                        return Optional.empty();
                })
                .build());

        sounds.addEntry(eb.startIntField(
                        localized("option", "soundInterval"),
                        options.soundInterval
                )
                .setTooltip(localized("option", "soundInterval.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.SOUND_INTERVAL_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.SOUND_INTERVAL_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundIntervalDefault)
                .setSaveConsumer(val -> options.soundInterval = val)
                .build());

        sounds.addEntry(eb.startFloatField(
                        localized("option", "soundPitchMin"),
                        options.soundPitchMin
                )
                .setTooltip(localized("option", "soundPitchMin.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.SOUND_PITCH_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > options.soundPitchMax)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundPitchMinDefault)
                .setSaveConsumer(val -> options.soundPitchMin = val)
                .build());

        sounds.addEntry(eb.startFloatField(
                        localized("option", "soundPitchMax"),
                        options.soundPitchMax
                )
                .setTooltip(localized("option", "soundPitchMax.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < options.soundPitchMin)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.SOUND_PITCH_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundPitchMaxDefault)
                .setSaveConsumer(val -> options.soundPitchMax = val)
                .build());

        sounds.addEntry(eb.startFloatField(localized("option", "soundVolume"), options.soundVolume)
                .setErrorSupplier(val -> {
                    if (val < Config.Options.SOUND_VOLUME_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.SOUND_VOLUME_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundVolumeDefault)
                .setSaveConsumer(val -> options.soundVolume = val)
                .build());

        sounds.addEntry(eb.startBooleanToggle(
                        localized("option", "allowSoundOverlap"),
                        options.allowSoundOverlap
                )
                .setTooltip(localized("option", "allowSoundOverlap.tooltip"))
                .setDefaultValue(Config.Options.allowSoundOverlapDefault)
                .setSaveConsumer(val -> options.allowSoundOverlap = val)
                .build());

        ConfigCategory keybinds = builder.getOrCreateCategory(localized("option", "keybinds"));

        keybinds.addEntry(eb.startBooleanToggle(
                        localized("option", "isolateKeybinds"),
                        options.isolateKeybinds
                )
                .setTooltip(localized("option", "isolateKeybinds.tooltip"))
                .setDefaultValue(Config.Options.isolateKeybindsDefault)
                .setSaveConsumer(val -> options.isolateKeybinds = val)
                .build());

        keybinds.addEntry((eb.startKeyCodeField(
                        localized("key", "edit"),
                        ((KeyMappingAccessor) KeybindManager.EDIT_KEY).clientsort$getKey()
                )
                .setDefaultValue(KeybindManager.EDIT_KEY.getDefaultKey())
                .setKeySaveConsumer((key) ->
                        KeybindManager.bindKey(KeybindManager.EDIT_KEY, key))
                .build()));

        keybinds.addEntry((eb.startKeyCodeField(
                        localized("key", "op.sort"),
                        ((KeyMappingAccessor) KeybindManager.SORT_KEY).clientsort$getKey()
                )
                .setDefaultValue(KeybindManager.SORT_KEY.getDefaultKey())
                .setKeySaveConsumer((key) ->
                        KeybindManager.bindKey(KeybindManager.SORT_KEY, key))
                .build()));

        keybinds.addEntry((eb.startKeyCodeField(
                        localized("key", "op.stackFill"),
                        ((KeyMappingAccessor) KeybindManager.STACK_FILL_KEY).clientsort$getKey()
                )
                .setDefaultValue(KeybindManager.STACK_FILL_KEY.getDefaultKey())
                .setKeySaveConsumer((key) ->
                        KeybindManager.bindKey(KeybindManager.STACK_FILL_KEY, key))
                .build()));

        keybinds.addEntry((eb.startKeyCodeField(
                        localized("key", "op.matchTransfer"),
                        ((KeyMappingAccessor) KeybindManager.MATCH_TRANSFER_KEY).clientsort$getKey()
                )
                .setDefaultValue(KeybindManager.MATCH_TRANSFER_KEY.getDefaultKey())
                .setKeySaveConsumer((key) ->
                        KeybindManager.bindKey(KeybindManager.MATCH_TRANSFER_KEY, key))
                .build()));

        keybinds.addEntry((eb.startKeyCodeField(
                        localized("key", "op.transfer"),
                        ((KeyMappingAccessor) KeybindManager.TRANSFER_KEY).clientsort$getKey()
                )
                .setDefaultValue(KeybindManager.TRANSFER_KEY.getDefaultKey())
                .setKeySaveConsumer((key) ->
                        KeybindManager.bindKey(KeybindManager.TRANSFER_KEY, key))
                .build()));

        ConfigCategory buttons = builder.getOrCreateCategory(localized("option", "buttons"));

        buttons.addEntry(eb.startBooleanToggle(
                        localized("option", "showButtons"),
                        options.showButtons
                )
                .setTooltip(localized("option", "showButtons.tooltip"))
                .setDefaultValue(Config.Options.showButtonsDefault)
                .setSaveConsumer(val -> options.showButtons = val)
                .build());

        firstSelector = eb.startEnumSelector(
                        localized("option", "firstButtonOp"),
                        Operation.class,
                        options.firstButtonOp
                )
                .setErrorSupplier((val) ->
                        val.equals(getSecondSelector())
                                || val.equals(getThirdSelector())
                                || val.equals(getFourthSelector())
                                ? Optional.of(localized("error", "triggerButton.duplicate"))
                                : Optional.empty())
                .setEnumNameProvider(val -> localized("triggerButton", val.name()))
                .setDefaultValue(Config.Options.firstButtonOpDefault)
                .setSaveConsumer(val -> options.firstButtonOp = val)
                .build();
        buttons.addEntry(firstSelector);

        secondSelector = eb.startEnumSelector(
                        localized("option", "secondButtonOp"),
                        Operation.class,
                        options.secondButtonOp
                )
                .setEnumNameProvider(val -> localized("triggerButton", val.name()))
                .setErrorSupplier((val) ->
                        val.equals(getFirstSelector())
                                || val.equals(getThirdSelector())
                                || val.equals(getFourthSelector())
                                ? Optional.of(localized("error", "triggerButton.duplicate"))
                                : Optional.empty())
                .setDefaultValue(Config.Options.secondButtonOpDefault)
                .setSaveConsumer(val -> options.secondButtonOp = val)
                .build();
        buttons.addEntry(secondSelector);

        thirdSelector = eb.startEnumSelector(
                        localized("option", "thirdButtonOp"),
                        Operation.class,
                        options.thirdButtonOp
                )
                .setEnumNameProvider(val -> localized("triggerButton", val.name()))
                .setErrorSupplier((val) ->
                        val.equals(getFirstSelector())
                                || val.equals(getSecondSelector())
                                || val.equals(getFourthSelector())
                                ? Optional.of(localized("error", "triggerButton.duplicate"))
                                : Optional.empty())
                .setDefaultValue(Config.Options.thirdButtonOpDefault)
                .setSaveConsumer(val -> options.thirdButtonOp = val)
                .build();
        buttons.addEntry(thirdSelector);

        fourthSelector = eb.startEnumSelector(
                        localized("option", "fourthButtonOp"),
                        Operation.class,
                        options.fourthButtonOp
                )
                .setEnumNameProvider(val -> localized("triggerButton", val.name()))
                .setErrorSupplier((val) ->
                        val.equals(getFirstSelector())
                                || val.equals(getSecondSelector())
                                || val.equals(getThirdSelector())
                                ? Optional.of(localized("error", "triggerButton.duplicate"))
                                : Optional.empty())
                .setDefaultValue(Config.Options.fourthButtonOpDefault)
                .setSaveConsumer(val -> options.fourthButtonOp = val)
                .build();
        buttons.addEntry(fourthSelector);

        SubCategoryBuilder layoutDefaults = eb.startSubCategory(
                        localized("option", "layoutDefaults"))
                .setTooltip(localized("option", "layoutDefaults.tooltip"))
                .setExpanded(true);

        layoutDefaults.add(eb.startIntField(
                        localized("option", "layoutOffset.x"),
                        options.layoutOffset.x()
                )
                .setDefaultValue(Options.layoutOffsetDefault.x())
                .setSaveConsumer(val -> options.layoutOffset =
                        new Vec2i(val, options.layoutOffset.y()))
                .build());

        layoutDefaults.add(eb.startIntField(
                        localized("option", "layoutOffset.y"),
                        options.layoutOffset.y()
                )
                .setDefaultValue(Options.layoutOffsetDefault.y())
                .setSaveConsumer(val -> options.layoutOffset =
                        new Vec2i(options.layoutOffset.x(), val))
                .build());

        buttons.addEntry(layoutDefaults.build());

        ConfigCategory policies = builder.getOrCreateCategory(localized("option", "policies"));

        SubCategoryBuilder policiesInstructions =
                eb.startSubCategory(localized("option", "policies.instructions"));

        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.1")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.2")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.3")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.4")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.5")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.6")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.7")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.8")).build());
        policiesInstructions.add(
                eb.startTextDescription(localized("option", "policies.description.9")).build());

        policies.addEntry(policiesInstructions.build());

        policies.addEntry(eb.startStrList(
                        localized("option", "classPolicies"),
                        getPolicyStrings(options.classPolicies.values())
                )
                .setTooltip(localized("option", "classPolicies.tooltip"))
                .setExpanded(true)
                .setErrorSupplier((list) -> {
                    int i = 0;
                    for (String string : list) {
                        try {
                            ClassPolicy.fromDataString(string, options.classPolicies.keySet());
                        } catch (ParseException ex) {
                            return Optional.of(localized(
                                    "error",
                                    "classPolicy.parse",
                                    i + 1,
                                    ex.getMessage()
                            ));
                        }
                        i++;
                    }
                    return Optional.empty();
                })
                .setDefaultValue(getPolicyStrings(Config.Options.classPoliciesDefaultList.get()))
                .setSaveConsumer((list) -> {
                    Set<ClassPolicy> classPolicies = new HashSet<>();
                    for (String string : list) {
                        try {
                            ClassPolicy policy = ClassPolicy.fromDataString(
                                    string,
                                    options.classPolicies.keySet()
                            );
                            classPolicies.add(policy);
                        } catch (ParseException ex) {
                            ClientSort.LOG.error(
                                    "Encountered a class policy parsing error on string '{}' not caught by error checker: {}",
                                    string,
                                    ex.getMessage()
                            );
                        }
                    }
                    options.classPolicies.clear();
                    classPolicies.forEach((policy) -> options.classPolicies.put(
                            policy.className(),
                            policy
                    ));
                })
                .build());

        return builder.build();
    }

    private static List<String> getPolicyStrings(Collection<ClassPolicy> policies) {
        List<String> strings = new ArrayList<>();
        for (ClassPolicy policy : policies) {
            strings.add(policy.toDataString());
        }
        return strings;
    }

    // Mild shenanigans to allow cross-validation between selectors

    private static Operation getFirstSelector() {
        return firstSelector == null
                ? null
                : (Operation) firstSelector.getValue();
    }

    private static Operation getSecondSelector() {
        return secondSelector == null
                ? null
                : (Operation) secondSelector.getValue();
    }

    private static Operation getThirdSelector() {
        return thirdSelector == null
                ? null
                : (Operation) thirdSelector.getValue();
    }

    private static Operation getFourthSelector() {
        return fourthSelector == null
                ? null
                : (Operation) fourthSelector.getValue();
    }
}
