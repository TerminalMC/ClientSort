/*
 * Copyright 2026 TerminalMC
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
import dev.terminalmc.clientsort.mixin.client.accessor.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import org.lwjgl.sdl.SDLScancode;

import java.nio.ByteBuffer;
import java.util.List;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public class KeybindManager {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ClientSort.MOD_ID, "main")
    );

    public static final KeyMapping EDIT_KEY = new KeyMapping(
            translationKey("key", "edit"),
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping CANCEL_AUTO_KEY = new KeyMapping(
            translationKey("key", "cancelAuto"),
            InputConstants.Type.KEYBOARD,
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
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping MATCH_TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.matchTransfer"),
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
    );
    public static final KeyMapping TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.transfer"),
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
            CATEGORY
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
        if (key.equals(InputConstants.UNKNOWN))
            return false;
        if (key.getType().equals(InputConstants.Type.MOUSE)) {
            int state = SDLMouse.SDL_GetMouseState(null, null);
            return switch (key.getValue()) {
                case SDLMouse.SDL_BUTTON_LEFT -> (state & SDLMouse.SDL_BUTTON_LMASK) != 0;
                case SDLMouse.SDL_BUTTON_MIDDLE -> (state & SDLMouse.SDL_BUTTON_MMASK) != 0;
                case SDLMouse.SDL_BUTTON_RIGHT -> (state & SDLMouse.SDL_BUTTON_RMASK) != 0;
                case SDLMouse.SDL_BUTTON_X1 -> (state & SDLMouse.SDL_BUTTON_X1MASK) != 0;
                case SDLMouse.SDL_BUTTON_X2 -> (state & SDLMouse.SDL_BUTTON_X2MASK) != 0;
                default -> false;
            };
        } else {
            ByteBuffer keyboardState = SDLKeyboard.SDL_GetKeyboardState();
            return keyboardState != null && keyboardState.get(key.getValue()) != 0;
        }
    }

    public static boolean hasControlDown() {
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            return InputConstants.isKeyDown(SDLScancode.SDL_SCANCODE_LGUI)
                    || InputConstants.isKeyDown(SDLScancode.SDL_SCANCODE_RGUI);
        } else {
            return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
        }
    }

    public static boolean hasShiftDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
    }

    public static boolean hasAltDown() {
        return InputConstants.isKeyDown(InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(InputConstants.KEY_RALT);
    }
}
