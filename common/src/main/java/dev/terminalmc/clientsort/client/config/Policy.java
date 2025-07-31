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

package dev.terminalmc.clientsort.client.config;

public enum Policy {
    KEYBIND_BUTTON(true, true),
    KEYBIND(true, false),
    NONE(false, false);

    public final boolean keybind;
    public final boolean button;

    Policy(boolean keybind, boolean button) {
        this.keybind = keybind;
        this.button = button;
    }

    public String toSimpleString() {
        return switch (this) {
            case KEYBIND_BUTTON -> "2";
            case KEYBIND -> "1";
            case NONE -> "0";
        };
    }

    public static Policy fromSimpleString(String str) {
        return switch (str) {
            case "2" -> KEYBIND_BUTTON;
            case "1" -> KEYBIND;
            case "0" -> NONE;
            default -> throw new IllegalArgumentException();
        };
    }
}
