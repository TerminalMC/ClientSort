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

package dev.terminalmc.clientsort.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.gui.ControlButtonManager;
import dev.terminalmc.clientsort.client.network.InteractionManager;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.util.ModLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class ClientSort {

    public static final String MOD_ID = dev.terminalmc.clientsort.ClientSort.MOD_ID;
    public static final String MOD_NAME = dev.terminalmc.clientsort.ClientSort.MOD_NAME;
    public static final ModLogger LOG = dev.terminalmc.clientsort.ClientSort.LOG;

    public static final KeyMapping EDIT_KEY = new KeyMapping(
            translationKey("key", "group.edit"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("key", "group")
    );
    public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "group.op.sort"),
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE,
            translationKey("key", "group")
    );
    public static final KeyMapping STACK_FILL_KEY =
            new KeyMapping(
                    translationKey("key", "group.op.stackFill"),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    translationKey("key", "group")
            );
    public static final KeyMapping TRANSFER_KEY =
            new KeyMapping(
                    translationKey("key", "group.op.transfer"),
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    translationKey("key", "group")
            );
    public static final List<KeyMapping> KEYBINDS = List.of(
            EDIT_KEY,
            SORT_KEY,
            STACK_FILL_KEY,
            TRANSFER_KEY
    );

    public static boolean searchOrderUpdated = false;

    public static volatile boolean emiReloading = false;
    public static volatile boolean updateBlockedByEmi = false;

    public static volatile boolean operatingClient = false;

    public static boolean debug() {
        return dev.terminalmc.clientsort.ClientSort.debug();
    }

    public static void init() {
        Config.getAndSave();
    }

    public static void afterClientTick(Minecraft mc) {
    }

    public static void afterScreenInit(Screen screen) {
        ControlButtonManager.afterScreenInit(screen);
    }

    public static void onConfigSaved(Config config) {
        Config.Options options = config.options;
        // Convert config sort order strings into enum values
        options.sortOrder = SortOrder.SORT_MODES.get(options.sortOrderStr);
        options.shiftSortOrder = SortOrder.SORT_MODES.get(options.shiftSortOrderStr);
        options.ctrlSortOrder = SortOrder.SORT_MODES.get(options.ctrlSortOrderStr);
        options.altSortOrder = SortOrder.SORT_MODES.get(options.altSortOrderStr);
        // Parse sound location string
        options.sortSoundLoc = ResourceLocation.tryParse(options.interactionSound);
        // Update interaction manager tick rate
        InteractionManager.setTickRate(options.interactionInterval);
        // Update class caches
        ControlButtonManager.reloadLayoutClasses(options.buttonLayouts.keySet());
        ControlButtonManager.reloadPolicyClasses(options.classPolicies.keySet());
    }
}
