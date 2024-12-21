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

import java.util.Optional;

import static dev.terminalmc.clientsort.util.mod.Localization.localized;

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
                .setDefaultValue(Config.Options.defaultInteractionRateServer)
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
                .setDefaultValue(Config.Options.defaultInteractionRateClient)
                .setSaveConsumer(val -> options.interactionRateClient = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "hotbarMode"),
                        Config.Options.HotbarMode.class, options.hotbarMode)
                .setEnumNameProvider(val -> localized("hotbarMode",
                        ((Config.Options.HotbarMode)val).lowerName()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("hotbarMode", val.lowerName() + ".tooltip")
                }))
                .setDefaultValue(Config.Options.defaultHotbarMode)
                .setSaveConsumer(val -> options.hotbarMode = val)
                .build());

        general.addEntry(eb.startEnumSelector(localized("option", "extraSlotMode"),
                        Config.Options.ExtraSlotMode.class, options.extraSlotMode)
                .setEnumNameProvider(val -> localized("extraSlotMode",
                        ((Config.Options.ExtraSlotMode)val).lowerName()))
                .setTooltipSupplier(val -> Optional.of(new Component[]{
                        localized("extraSlotMode", val.lowerName() + ".tooltip")
                }))
                .setDefaultValue(Config.Options.defaultExtraSlotMode)
                .setSaveConsumer(val -> options.extraSlotMode = val)
                .build());

        general.addEntry(eb.startBooleanToggle(localized("option", "rmbBundle"),
                        options.rmbBundle)
                .setTooltip(localized("option", "rmbBundle.tooltip"))
                .setDefaultValue(Config.Options.defaultRmbBundle)
                .setSaveConsumer(val -> {
                    options.rmbBundle = val;
                    if (val) CreativeSearchOrder.tryRefreshItemSearchPositionLookup();
                })
                .build());

        ConfigCategory sort = builder.getOrCreateCategory(localized("option", "sorting"));

        sort.addEntry(eb.startSelector(localized("option", "sortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.sortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.defaultSortMode)
                .setSaveConsumer(val -> options.sortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "shiftSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.shiftSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.defaultShiftSortMode)
                .setSaveConsumer(val -> options.shiftSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "ctrlSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.ctrlSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.defaultCtrlSortMode)
                .setSaveConsumer(val -> options.ctrlSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startSelector(localized("option", "altSortMode"),
                        SortMode.SORT_MODES.keySet().toArray(), options.altSortModeStr)
                .setNameProvider(val -> localized("sortOrder", (String)val))
                .setDefaultValue(Config.Options.defaultAltSortMode)
                .setSaveConsumer(val -> options.altSortModeStr = (String)val)
                .build());

        sort.addEntry(eb.startBooleanToggle(localized("option", "optimizedCreativeSorting"),
                        options.optimizedCreativeSorting)
                .setDefaultValue(Config.Options.defaultOptimizedCreativeSorting)
                .setSaveConsumer(val -> {
                    options.optimizedCreativeSorting = val;
                    if (val) CreativeSearchOrder.tryRefreshItemSearchPositionLookup();
                })
                .build());

        return builder.build();
    }
}
