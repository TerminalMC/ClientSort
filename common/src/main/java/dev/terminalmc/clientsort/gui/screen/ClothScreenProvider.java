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

package dev.terminalmc.clientsort.gui.screen;

import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.inventory.sort.SortOrder;
import dev.terminalmc.clientsort.util.item.CreativeSearchOrder;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ClothScreenProvider {
    /**
     * Builds and returns a Cloth Config options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not
     * available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.options();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("name"))
                .setSavingRunnable(Config::getAndSave);

        ConfigEntryBuilder eb = builder.entryBuilder();


        ConfigCategory general = builder.getOrCreateCategory(localized("option", "general"));

        general.addEntry(eb.startIntField(localized("option", "interactionRateServer"),
                        options.interactionRateServer)
                .setTooltip(localized("option", "interactionRate.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.interactionRateMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.interactionRateMax) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionRateServerDefault)
                .setSaveConsumer(val -> options.interactionRateServer = val)
                .build());

        general.addEntry(eb.startIntField(localized("option", "interactionRateClient"),
                        options.interactionRateClient)
                .setTooltip(localized("option", "interactionRate.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.interactionRateMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.interactionRateMax) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionRateClientDefault)
                .setSaveConsumer(val -> options.interactionRateClient = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "serverAcceleratedSorting"),
                        options.serverAcceleratedSorting)
                .setTooltip(localized("option", "serverAcceleratedSorting.tooltip"))
                .setDefaultValue(Config.Options.serverAcceleratedSortingDefault)
                .setSaveConsumer(val -> options.serverAcceleratedSorting = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "optimizedCreativeSorting"),
                        options.optimizedCreativeSorting)
                .setTooltip(localized("option", "optimizedCreativeSorting.tooltip"))
                .setDefaultValue(Config.Options.optimizedCreativeSortingDefault)
                .setSaveConsumer(val -> {
                    options.optimizedCreativeSorting = val;
                    if (val) CreativeSearchOrder.tryRefreshStackPositionMap();
                })
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "hotbarScope"),
                        Config.Options.HotbarScope.class, options.hotbarScope)
                .setEnumNameProvider(val -> localized("hotbarScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.hotbarScopeDefault)
                .setSaveConsumer(val -> options.hotbarScope = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "extraSlotScope"),
                        Config.Options.ExtraSlotScope.class, options.extraSlotScope)
                .setEnumNameProvider(val -> localized("extraSlotScope", val.name()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotScope", val + ".tooltip")
                }))
                .setDefaultValue(Config.Options.extraSlotScopeDefault)
                .setSaveConsumer(val -> options.extraSlotScope = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "rmbBundle"),
                        options.rmbBundle)
                .setTooltip(localized("option", "rmbBundle.tooltip"))
                .setDefaultValue(Config.Options.rmbBundleDefault)
                .setSaveConsumer(val -> {
                    options.rmbBundle = val;
                    if (val) CreativeSearchOrder.tryRefreshStackPositionMap();
                })
                .build());

        ConfigCategory sort = builder.getOrCreateCategory(localized("option", "sorting"));

        sort.addEntry(eb.startSelector(localized("option", "sortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(), options.sortOrderStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.sortOrderDefault)
                .setSaveConsumer(val -> options.sortOrderStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "shiftSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(), options.shiftSortOrderStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.shiftSortOrderDefault)
                .setSaveConsumer(val -> options.shiftSortOrderStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "ctrlSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(), options.ctrlSortOrderStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.ctrlSortOrderDefault)
                .setSaveConsumer(val -> options.ctrlSortOrderStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "altSortOrder"),
                        SortOrder.SORT_MODES.keySet().toArray(), options.altSortOrderStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.altSortOrderDefault)
                .setSaveConsumer(val -> options.altSortOrderStr = (String)val)
                .build());

        ConfigCategory sound = builder.getOrCreateCategory(localized("option", "sound"));

        sound.addEntry(eb.startBooleanToggle(localized("option", "soundEnabled"),
                        options.soundEnabled)
                .setDefaultValue(Config.Options.soundEnabledDefault)
                .setSaveConsumer(val -> options.soundEnabled = val)
                .build());

        sound.addEntry(eb.startStrField(localized("option", "sortSound"), 
                        options.sortSound)
                .setDefaultValue(Config.Options.sortSoundDefault)
                .setSaveConsumer(val -> options.sortSound = val)
                .setErrorSupplier(val -> {
                    if (ResourceLocation.tryParse(val) == null) return Optional.of(
                            localized("option", "error.resourceLocation.parse"));
                    else return Optional.empty();
                })
                .build());

        sound.addEntry(eb.startIntField(localized("option", "soundRate"), 
                        options.soundRate)
                .setTooltip(localized("option", "soundRate.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.interactionRateMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.interactionRateMax) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundRateDefault)
                .setSaveConsumer(val -> options.soundRate = val)
                .build());

        sound.addEntry(eb.startFloatField(localized("option", "soundMinPitch"),
                        options.soundMinPitch)
                .setTooltip(localized("option", "soundMinPitch.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < Config.Options.soundPitchMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > options.soundMaxPitch) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundMinPitchDefault)
                .setSaveConsumer(val -> options.soundMinPitch = val)
                .build());

        sound.addEntry(eb.startFloatField(localized("option", "soundMaxPitch"),
                        options.soundMaxPitch)
                .setTooltip(localized("option", "soundMaxPitch.tooltip"))
                .setErrorSupplier(val -> {
                    if (val < options.soundMinPitch) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.soundPitchMax) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundMaxPitchDefault)
                .setSaveConsumer(val -> options.soundMaxPitch = val)
                .build());

        sound.addEntry(eb.startFloatField(localized("option", "soundVolume"),
                        options.soundVolume)
                .setErrorSupplier(val -> {
                    if (val < Config.Options.soundVolumeMin) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > Config.Options.soundVolumeMax) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundVolumeDefault)
                .setSaveConsumer(val -> options.soundVolume = val)
                .build());

        sound.addEntry(eb.startBooleanToggle(localized("option", "soundAllowOverlap"),
                        options.soundAllowOverlap)
                .setTooltip(localized("option", "soundAllowOverlap.tooltip"))
                .setDefaultValue(Config.Options.soundAllowOverlapDefault)
                .setSaveConsumer(val -> options.soundAllowOverlap = val)
                .build());

        return builder.build();
    }
}
