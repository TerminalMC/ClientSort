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
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.mixin.client.accessor.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.translationKey;

/**
 * To allow optionally isolating mod keybinds from MC keybinds, the value of all keybinds must be
 * synced between mod config and MC config.
 * <p>
 * If {@link Config.Options#isolateKeybinds} is {@code true}, the keybinds will be set initially
 * by MC when MC config is loaded. The mod config values will not be read at all.
 * <p>
 * If {@link Config.Options#isolateKeybinds} is {@code false}, keybinds will be set initially by
 * the mod after MC has finished loading. The mod config values will not be read again.
 * <p>
 * The mod config values are kept up-to-date with mod config changes by the CC option handers, and
 * with MC config changes by the {@link KeyMapping#resetMapping} listener.
 */
public class Keybinds {

    public static final KeyMapping EDIT_KEY = new KeyMapping(
            translationKey("key", "edit"),
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
    public static final KeyMapping TRANSFER_KEY = new KeyMapping(
            translationKey("key", "op.transfer"),
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            translationKey("name")
    );
    public static final List<KeyMapping> KEYBINDS = List.of(
            EDIT_KEY,
            SORT_KEY,
            STACK_FILL_KEY,
            TRANSFER_KEY
    );

    /**
     * Saves the keybind values to mod config.
     */
    public static void keybindsToConfig() {
        options().editKey = toName(EDIT_KEY);
        options().sortKey = toName(SORT_KEY);
        options().stackFillKey = toName(STACK_FILL_KEY);
        options().transferKey = toName(TRANSFER_KEY);
        Config.save();
    }

    /**
     * Loads the keybind values from mod config.
     */
    public static void configToKeybinds() {
        EDIT_KEY.setKey(fromName(options().editKey));
        SORT_KEY.setKey(fromName(options().sortKey));
        STACK_FILL_KEY.setKey(fromName(options().stackFillKey));
        TRANSFER_KEY.setKey(fromName(options().transferKey));
    }

    /**
     * Sets and saves the keybind. Does not affect mod config.
     */
    public static void bindKey(KeyMapping keybind, InputConstants.Key key) {
        keybind.setKey(key);
        if (!options().isolateKeybinds) {
            KeyMapping.resetMapping();
            Minecraft.getInstance().options.save();
        }
    }

    /**
     * @return the name of the keybind's current key.
     */
    public static String toName(KeyMapping keybind) {
        return ((KeyMappingAccessor) keybind).clientsort$getKey().getName();
    }

    /**
     * @return the key identified by the name, or {@link InputConstants#UNKNOWN} if the name is
     * invalid.
     */
    public static InputConstants.Key fromName(@Nullable String name) {
        if (name == null)
            return InputConstants.UNKNOWN;
        try {
            return InputConstants.getKey(name);
        } catch (IllegalArgumentException e) {
            return InputConstants.UNKNOWN;
        }
    }
}
