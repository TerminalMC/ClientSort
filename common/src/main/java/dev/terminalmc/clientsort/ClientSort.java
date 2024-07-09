/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
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
            translationKey("key", "sort"), InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, translationKey("key_group"));

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
