/*
 * Copyright 2024 TerminalMC
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

package dev.terminalmc.clientsort;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.inventory.sort.SortMode;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.mod.ModLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import static dev.terminalmc.clientsort.util.mod.Localization.translationKey;

public class ClientSort {
    public static final String MOD_ID = "clientsort";
    public static final String MOD_NAME = "ClientSort";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
    public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "group.sort"), InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, translationKey("key", "group"));

    public static boolean searchOrderUpdated = false;

    public static boolean emiReloading = false;
    public static boolean updateBlockedByEmi = false;

    public static void init() {
        Config.getAndSave();
    }

    public static void onEndTick(Minecraft mc) {
    }

    public static void onConfigSaved(Config config) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            InteractionManager.setTickRate(config.options.interactionRateServer);
            config.options.sortMode = SortMode.SORT_MODES.get(config.options.sortModeStr);
            config.options.shiftSortMode = SortMode.SORT_MODES.get(config.options.shiftSortModeStr);
            config.options.ctrlSortMode = SortMode.SORT_MODES.get(config.options.ctrlSortModeStr);
            config.options.altSortMode = SortMode.SORT_MODES.get(config.options.altSortModeStr);
        } else {
            InteractionManager.setTickRate(config.options.interactionRateClient);
        }
    }
}
