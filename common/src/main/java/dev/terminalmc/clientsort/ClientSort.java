/*
 * Copyright 2024 NotRyken
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.terminalmc.clientsort;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.mixin.KeyMappingAccessor;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.inject.IContainerScreen;
import dev.terminalmc.clientsort.util.mod.ModLogger;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import static dev.terminalmc.clientsort.util.mod.Localization.translationKey;

public class ClientSort {
    public static final String MOD_ID = "clientsort";
    public static final String MOD_NAME = "ClientSort";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);
	public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "sort"), InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE, translationKey("key_group"));

    public static boolean searchOrderUpdated = false;

	public static int lastUpdatedSlot = -1;

	public static int cooldown = 0;

    public static void init() {
        Config.getAndSave();
    }

	public static void onEndTick(Minecraft mc) {
		if (cooldown == 0) {
            int key = ((KeyMappingAccessor) SORT_KEY).getKey().getValue();
            if (key != GLFW.GLFW_KEY_UNKNOWN) {
                boolean down;
                if (key <= 7) {
                    down = GLFW.glfwGetMouseButton(mc.getWindow().getWindow(), key) == GLFW.GLFW_PRESS;
                } else {
                    down = GLFW.glfwGetKey(mc.getWindow().getWindow(), key) == GLFW.GLFW_PRESS;
                }
                if (down) {
                    if (mc.screen instanceof IContainerScreen screen) {
                        screen.mouseWheelie_triggerSort();
                        cooldown = 11;
                    }
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
