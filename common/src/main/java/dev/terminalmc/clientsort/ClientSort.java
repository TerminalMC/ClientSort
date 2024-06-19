/*
 * Copyright 2020-2022 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 */

package dev.terminalmc.clientsort;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.mixin.KeyMappingAccessor;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.inject.IContainerScreen;
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

	public static int lastUpdatedSlot = -1;

	public static int cooldown = 0;

    public static void init() {
        Config.getAndSave();
    }

	public static void onEndTick(Minecraft mc) {
		if (cooldown == 0) {
			if (InputConstants.isKeyDown(mc.getWindow().getWindow(),
					((KeyMappingAccessor) SORT_KEY).getKey().getValue())) {
				if (mc.screen instanceof IContainerScreen screen) {
					LogUtils.getLogger().info("good screen");
					screen.mouseWheelie_triggerSort();
					cooldown = 11;
				}
			}
		}
		if (cooldown > 0) cooldown--;
	}

    public static void onConfigSaved(Config config) {
        if (Minecraft.getInstance().getSingleplayerServer() == null) {
            InteractionManager.setTickRate(config.options.interactionRateServer);
        } else {
            InteractionManager.setTickRate(config.options.interactionRateClient);
        }
    }
}
