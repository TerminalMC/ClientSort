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

package dev.terminalmc.clientsort;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.inventory.sort.SortOrder;
import dev.terminalmc.clientsort.network.InteractionManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class ClientSort {
    public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "group.sort"), InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, translationKey("key", "group"));
    
    public static boolean searchOrderUpdated = false;

    public static Lock emiReloadLock = new ReentrantLock();
    public static boolean updateBlockedByEmi = false;

    public static void init() {
        Config.getAndSave();
    }
    
    public static void onConfigSaved(Config config) {
        Config.Options options = config.options;
        options.sortOrder = SortOrder.SORT_MODES.get(options.sortOrderStr);
        options.shiftSortOrder = SortOrder.SORT_MODES.get(options.shiftSortOrderStr);
        options.ctrlSortOrder = SortOrder.SORT_MODES.get(options.ctrlSortOrderStr);
        options.altSortOrder = SortOrder.SORT_MODES.get(options.altSortOrderStr);
        options.sortSoundLoc = ResourceLocation.tryParse(options.sortSound);
        setInteractionManagerTickRate(config.options);
    }

    public static void setInteractionManagerTickRate(Config.Options options) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            InteractionManager.setTickRate(options.interactionRateServer);
        } else {
            InteractionManager.setTickRate(options.interactionRateClient);
        }
    }
}
