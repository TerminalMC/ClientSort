package dev.terminalmc.framework.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.framework.Framework;
import dev.terminalmc.framework.config.Config;
import me.shedaniel.clothconfig2.api.*;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static dev.terminalmc.framework.util.Localization.localized;

public class ClothConfigScreenProvider {
    /**
     * Builds and returns a Cloth Config options screen.
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not
     * available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.get().options;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("screen", "options"))
                .setSavingRunnable(() -> Framework.onConfigSaved(Config.getAndSave()));

        ConfigEntryBuilder eb = builder.entryBuilder();
        ConfigCategory cat1 = builder.getOrCreateCategory(localized("option", "category.category1"));

        // Yes/No button
        cat1.addEntry(eb.startBooleanToggle(
                localized("option", "boolean_example"), options.booleanExample)
                .setTooltip(localized("option", "boolean_example.tooltip"))
                .setDefaultValue(Config.Options.defaultBooleanExample)
                .setSaveConsumer(val -> options.booleanExample = val)
                .build());

        // Integer slider with value text formatting
        cat1.addEntry(eb.startIntSlider(
                localized("option", "int_example"), options.intExample, 0, 10)
                .setTextGetter(val -> localized("option", "int_example.value", val))
                .setDefaultValue(Config.Options.defaultIntExample)
                .setSaveConsumer(val -> options.intExample = val)
                .build());

        // Double field with validation
        cat1.addEntry(eb.startDoubleField(
                localized("option", "double_example"), options.doubleExample)
                .setErrorSupplier(val -> {
                        if (val < 0) return Optional.of(
                                localized("option", "double_example.error.low"));
                        else if (val > 100) return Optional.of(
                                localized("option", "double_example.error.high"));
                        else return Optional.empty();
                    })
                .setDefaultValue(Config.Options.defaultDoubleExample)
                .setSaveConsumer(val -> options.doubleExample = val)
                .build());

        // Text field with suggestion provider and validation
        // Recommended for large option lists
        Set<String> items = new HashSet<>(BuiltInRegistries.ITEM.keySet()
                .stream().map(ResourceLocation::toString).toList());
        cat1.addEntry(eb.startStringDropdownMenu(
                localized("option", "item_example"), options.itemExample)
                .setSelections(items)
                .setDefaultValue(Config.Options.defaultItemExample)
                .setErrorSupplier(val -> {
                    if (items.contains(val)) return Optional.empty();
                    else return Optional.of(localized("option", "item_example.error"));
                })
                .setSaveConsumer(val -> options.itemExample = val)
                .build());

        // Drop-down selection list (no typing)
        // Recommended for small option lists
        cat1.addEntry(eb.startDropdownMenu(
                localized("option", "object_example_1"), options.objectExample1,
                        Config.Options.TriState::valueOf)
                .setSuggestionMode(false)
                .setSelections(List.of(Config.Options.TriState.values()))
                .setDefaultValue(Config.Options.defaultObjectExample1)
                .setSaveConsumer(val -> options.objectExample1 = val)
                .build());

        // Cycling button
        // See also `startSelector` if you aren't using an enum
        cat1.addEntry(eb.startEnumSelector(
                localized("option", "object_example_2"),
                        Config.Options.TriState.class, options.objectExample2)
                .setDefaultValue(Config.Options.defaultObjectExample2)
                .setSaveConsumer(val -> options.objectExample2 = val)
                .build());

        // Expandable sub-category on same page
        SubCategoryBuilder cat1Sub1b = eb.startSubCategory(
                localized("option", "subcategory1"));

        // Multiline description
        cat1Sub1b.add(eb.startTextDescription(
                localized("option", "subcategory1.description"))
                .build());

        // Dynamic list of strings
        cat1Sub1b.add(eb.startStrList(
                localized("option", "string_list_example"), options.stringListExample)
                .setDefaultValue(Config.Options.defaultStringListExample)
                .setSaveConsumer(val -> options.stringListExample = val)
                .build());

        cat1.addEntry(cat1Sub1b.build());

        // Distinct category on separate page
        ConfigCategory cat2 = builder.getOrCreateCategory(localized("option", "category.category2"));

        // Standard keybind
        cat2.addEntry(eb.startKeyCodeField(
                localized("option", "key_example"),
                        InputConstants.getKey(options.keyExample, options.keyExample))
                .setDefaultValue(InputConstants.getKey(
                        Config.Options.defaultKeyExample, Config.Options.defaultKeyExample))
                .setKeySaveConsumer(val -> options.keyExample = val.getValue())
                .build());

        return builder.build();
    }
}
