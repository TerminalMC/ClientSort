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
import dev.terminalmc.clientsort.mixin.client.accessor.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class KeybindManager {

    public static final KeyMapping EDIT_KEY = new KeyMapping(
            translationKey("key", "edit"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final KeyMapping CANCEL_AUTO_KEY = new KeyMapping(
            translationKey("key", "cancelAuto"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final KeyMapping SORT_KEY = new KeyMapping(
            translationKey("key", "op.sort"),
            InputConstants.Type.MOUSE,
            InputConstants.MOUSE_BUTTON_MIDDLE,
            translationKey("name")
    );
    public static final KeyMapping STACK_FILL_KEY = new KeyMapping(
            translationKey("key", "op.stackFill"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final KeyMapping MATCH_TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.matchTransfer"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final KeyMapping TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.transfer"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final List<KeyMapping> KEYBINDS = List.of(
            EDIT_KEY,
            CANCEL_AUTO_KEY,
            SORT_KEY,
            STACK_FILL_KEY,
            MATCH_TRANSFER_KEY,
            TRANSFER_KEY
    );

    /**
     * Attempts to remove all mod keybinds from the MC keybind maps.
     * <p>
     * Does not affect the MC keybind options list, since that references
     * {@link net.minecraft.client.Options#keyMappings} which is (mostly) unrelated.
     */
    public static void isolateKeybinds() {
        KEYBINDS.forEach((kb) -> KeyMappingAccessor.clientsort$getAll().remove(kb.getName()));
        KeyMapping.resetMapping();
    }

    /**
     * Sets and saves the keybind. Does not affect mod config.
     */
    public static void bindKey(KeyMapping keybind, InputConstants.Key key) {
        keybind.setKey(key);
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    /**
     * @return {@code true} if the bound key is down (not only if the keybind is triggered).
     */
    public static boolean isDown(KeyMapping keybind) {
        return isKeyDown(((KeyMappingAccessor) keybind).clientsort$getKey());
    }

    public static boolean isKeyDown(InputConstants.Key key) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.equals(InputConstants.UNKNOWN))
            return false;
        if (key.getType().equals(InputConstants.Type.MOUSE)) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == 1;
        } else {
            return GLFW.glfwGetKey(window, key.getValue()) == 1;
        }
    }
}
