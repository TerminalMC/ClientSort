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
import dev.terminalmc.clientsort.inventory.sort.SortMode;
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
                    if (val < 1) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 100) return Optional.of(
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
                    if (val < 1) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 100) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.interactionRateClientDefault)
                .setSaveConsumer(val -> options.interactionRateClient = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "hotbarMode"),
                        Config.Options.HotbarMode.class, options.hotbarMode)
                .setEnumNameProvider(val -> localized("hotbarMode",
                        ((Config.Options.HotbarMode)val).lowerName()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarMode", val.lowerName() + ".tooltip")
                }))
                .setDefaultValue(Config.Options.hotbarModeDefault)
                .setSaveConsumer(val -> options.hotbarMode = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "extraSlotMode"),
                        Config.Options.ExtraSlotMode.class, options.extraSlotMode)
                .setEnumNameProvider(val -> localized("extraSlotMode",
                        ((Config.Options.ExtraSlotMode)val).lowerName()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotMode", val.lowerName() + ".tooltip")
                }))
                .setDefaultValue(Config.Options.extraSlotModeDefault)
                .setSaveConsumer(val -> options.extraSlotMode = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "lmbBundle"),
                        options.lmbBundle)
                .setTooltip(localized("option", "lmbBundle.tooltip"))
                .setDefaultValue(Config.Options.lmbBundleDefault)
                .setSaveConsumer(val -> {
                    options.lmbBundle = val;
                    if (val) CreativeSearchOrder.tryRefreshItemSearchPositionLookup();
                })
                .build());

        ConfigCategory sort = builder.getOrCreateCategory(localized("option", "sorting"));

        sort.addEntry(eb.startSelector(localized("option", "sortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.sortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.sortModeDefault)
                .setSaveConsumer(val -> options.sortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "shiftSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.shiftSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.shiftSortModeDefault)
                .setSaveConsumer(val -> options.shiftSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "ctrlSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.ctrlSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.ctrlSortModeDefault)
                .setSaveConsumer(val -> options.ctrlSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "altSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.altSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.altSortModeDefault)
                .setSaveConsumer(val -> options.altSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startBooleanToggle(localized("option", "optimizedCreativeSorting"),
                        options.optimizedCreativeSorting)
                .setDefaultValue(Config.Options.optimizedCreativeSortingDefault)
                .setSaveConsumer(val -> {
                    options.optimizedCreativeSorting = val;
                    if (val) CreativeSearchOrder.tryRefreshItemSearchPositionLookup();
                })
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
                    if (val < 1) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 100) return Optional.of(
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
                    if (val < 0.5) return Optional.of(
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
                    else if (val > 2) return Optional.of(
                            localized("option", "error.high"));
                    else return Optional.empty();
                })
                .setDefaultValue(Config.Options.soundMaxPitchDefault)
                .setSaveConsumer(val -> options.soundMaxPitch = val)
                .build());

        sound.addEntry(eb.startFloatField(localized("option", "soundVolume"),
                        options.soundVolume)
                .setErrorSupplier(val -> {
                    if (val < 0.0F) return Optional.of(
                            localized("option", "error.low"));
                    else if (val > 1.0F) return Optional.of(
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
