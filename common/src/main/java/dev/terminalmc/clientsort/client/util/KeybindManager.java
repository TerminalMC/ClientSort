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

package dev.terminalmc.clientsort.client.util;

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.client.ClientSort;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class KeybindManager {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "main")
    );

    public static final KeyMapping EDIT_KEY = new KeyMapping(
            translationKey("key", "edit"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "op.sort"),
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE,
            CATEGORY
    );
    public static final KeyMapping STACK_FILL_KEY = new KeyMapping(
            translationKey("key", "op.stackFill"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping MATCH_TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.matchTransfer"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.transfer"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final List<KeyMapping> KEYBINDS = List.of(
            EDIT_KEY,
            SORT_KEY,
            STACK_FILL_KEY,
            MATCH_TRANSFER_KEY,
            TRANSFER_KEY
    );

    /**
     * Sets and saves the keybind. Does not affect mod config.
     */
    public static void bindKey(KeyMapping keybind, InputConstants.Key key) {
        keybind.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    public static boolean hasControlDown() {
        if (InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB) {
            return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 343)
                    || InputConstants.isKeyDown(
                    Minecraft.getInstance().getWindow(),
                    347
            );
        } else {
            return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 341)
                    || InputConstants.isKeyDown(
                    Minecraft.getInstance().getWindow(),
                    345
            );
        }
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 340)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 344);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 342)
                || InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 346);
    }
}
