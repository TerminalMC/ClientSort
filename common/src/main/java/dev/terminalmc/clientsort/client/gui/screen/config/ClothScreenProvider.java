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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.config.ButtonLayout;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.order.CreativeSearchOrder;
import dev.terminalmc.clientsort.client.order.SortOrder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.text.ParseException;
import java.util.*;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ClothScreenProvider {

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
                        ClientSort.debug
                )
                .setTooltip(localized("option", "showDebugInfo.tooltip"))
                .setDefaultValue(false)
                .setSaveConsumer(val -> ClientSort.debug = val)
                .build());

        ConfigCategory sort = builder.getOrCreateCategory(localized("option", "sorting"));

        sort.addEntry(eb.startSelector(
                        localized("option", "sortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(),
                        options.sortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.sortOrderDefault)
                .setSaveConsumer(val -> options.sortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "shiftSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(),
                        options.shiftSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.shiftSortOrderDefault)
                .setSaveConsumer(val -> options.shiftSortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "ctrlSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(),
                        options.ctrlSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.ctrlSortOrderDefault)
                .setSaveConsumer(val -> options.ctrlSortOrderStr = (String) val)
                .build());

        sort.addEntry(eb.startSelector(
                        localized("option", "altSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(),
                        options.altSortOrderStr
                )
                .setNameProvider(val -> localized("sortOrder", (String) val))
                .setDefaultValue(Config.Options.altSortOrderDefault)
                .setSaveConsumer(val -> options.altSortOrderStr = (String) val)
                .build());

        ConfigCategory sound = builder.getOrCreateCategory(localized("option", "sound"));

        sound.addEntry(eb.startBooleanToggle(
                        localized("option", "playSoundSort"),
                        options.playSoundSort
                )
                .setTooltip(localized("option", "playSoundSort.tooltip"))
                .setDefaultValue(Config.Options.playSoundSortDefault)
                .setSaveConsumer(val -> options.playSoundSort = val)
                .build());

        sound.addEntry(eb.startBooleanToggle(
                        localized("option", "playSoundOther"),
                        options.playSoundOther
                )
                .setTooltip(localized("option", "playSoundOther.tooltip"))
                .setDefaultValue(Config.Options.playSoundOtherDefault)
                .setSaveConsumer(val -> options.playSoundOther = val)
                .build());

        sound.addEntry(eb.startStrField(
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

        sound.addEntry(eb.startIntField(localized("option", "soundInterval"), options.soundInterval)
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

        sound.addEntry(eb.startFloatField(
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

        sound.addEntry(eb.startFloatField(
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

        sound.addEntry(eb.startFloatField(localized("option", "soundVolume"), options.soundVolume)
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

        sound.addEntry(eb.startBooleanToggle(
                        localized("option", "allowSoundOverlap"),
                        options.allowSoundOverlap
                )
                .setTooltip(localized("option", "allowSoundOverlap.tooltip"))
                .setDefaultValue(Config.Options.allowSoundOverlapDefault)
                .setSaveConsumer(val -> options.allowSoundOverlap = val)
                .build());

        ConfigCategory gui = builder.getOrCreateCategory(localized("option", "gui"));

        gui.addEntry(eb.startBooleanToggle(localized("option", "showButtons"), options.showButtons)
                .setTooltip(localized("option", "showButtons.tooltip"))
                .setDefaultValue(Config.Options.showButtonsDefault)
                .setSaveConsumer(val -> options.showButtons = val)
                .build());

        gui.addEntry(eb.startEnumSelector(
                        localized("option", "firstButton"),
                        Config.Options.CONTROL_BUTTON.class,
                        options.firstButton
                )
                .setEnumNameProvider(val -> localized("controlButton", val.name()))
                .setDefaultValue(Config.Options.firstButtonDefault)
                .setSaveConsumer(val -> options.firstButton = val)
                .build());

        gui.addEntry(eb.startEnumSelector(
                        localized("option", "secondButton"),
                        Config.Options.CONTROL_BUTTON.class,
                        options.secondButton
                )
                .setEnumNameProvider(val -> localized("controlButton", val.name()))
                .setDefaultValue(Config.Options.secondButtonDefault)
                .setSaveConsumer(val -> options.secondButton = val)
                .build());

        gui.addEntry(eb.startEnumSelector(
                        localized("option", "thirdButton"),
                        Config.Options.CONTROL_BUTTON.class,
                        options.thirdButton
                )
                .setEnumNameProvider(val -> localized("controlButton", val.name()))
                .setDefaultValue(Config.Options.thirdButtonDefault)
                .setSaveConsumer(val -> options.thirdButton = val)
                .build());

        gui.addEntry(eb.startIntField(
                        localized("option", "buttonDefaultOffset.x"),
                        options.buttonDefaultOffset.x()
                )
                .setTooltip(localized("option", "buttonDefaultOffset.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.BUTTON_DEFAULT_OFFSET_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.BUTTON_DEFAULT_OFFSET_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.buttonDefaultOffsetDefault.x())
                .setSaveConsumer(val -> options.buttonDefaultOffset =
                        new Vec2i(val, options.buttonDefaultOffset.y()))
                .build());

        gui.addEntry(eb.startIntField(
                        localized("option", "buttonDefaultOffset.y"),
                        options.buttonDefaultOffset.y()
                )
                .setTooltip(localized("option", "buttonDefaultOffset.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.BUTTON_DEFAULT_OFFSET_MIN)
                        return Optional.of(localized("error", "low"));
                    else if (val > Config.Options.BUTTON_DEFAULT_OFFSET_MAX)
                        return Optional.of(localized("error", "high"));
                    else
                        return Optional.empty();
                })
                .setDefaultValue(Config.Options.buttonDefaultOffsetDefault.y())
                .setSaveConsumer(val -> options.buttonDefaultOffset =
                        new Vec2i(options.buttonDefaultOffset.x(), val))
                .build());

        gui.addEntry(eb.startStrList(
                        localized("option", "buttonLayouts"),
                        getLayoutStrings(options.buttonLayouts.values())
                )
                .setTooltip(localized("option", "buttonLayouts.tooltip.1")
                        .append("\n")
                        .append(localized("option", "buttonLayouts.tooltip.2"))
                        .append("\n")
                        .append(localized("option", "buttonLayouts.tooltip.3")))
                .setExpanded(true)
                .setErrorSupplier((list) -> {
                    int i = 0;
                    for (String string : list) {
                        try {
                            ButtonLayout.fromDataString(string, options.buttonLayouts.keySet());
                        } catch (ParseException ex) {
                            return Optional.of(localized(
                                    "error",
                                    "buttonLayout.parse",
                                    i + 1,
                                    ex.getMessage()
                            ));
                        }
                        i++;
                    }
                    return Optional.empty();
                })
                .setDefaultValue(getLayoutStrings(Config.Options.buttonLayoutsDefaultList.get()))
                .setSaveConsumer((list) -> {
                    Set<ButtonLayout> layouts = new HashSet<>();
                    for (String string : list) {
                        try {
                            ButtonLayout layout = ButtonLayout.fromDataString(
                                    string,
                                    options.buttonLayouts.keySet()
                            );
                            layouts.add(layout);
                        } catch (ParseException ex) {
                            ClientSort.LOG.error(
                                    "Encountered a button layout parsing error on layout string '{}' not caught by error checker: {}",
                                    string,
                                    ex.getMessage()
                            );
                        }
                    }
                    options.buttonLayouts.clear();
                    layouts.forEach((layout) -> options.buttonLayouts.put(
                            layout.className,
                            layout
                    ));
                })
                .build());

        return builder.build();
    }

    private static List<String> getLayoutStrings(Collection<ButtonLayout> layouts) {
        List<String> strings = new ArrayList<>();
        for (ButtonLayout layout : layouts) {
            strings.add(layout.toDataString());
        }
        return strings;
    }
}
