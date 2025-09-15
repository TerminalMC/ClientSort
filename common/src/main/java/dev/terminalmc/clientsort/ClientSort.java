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

import dev.terminalmc.clientsort.config.ServerConfig;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.util.ModLogger;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.ticks.ContainerSingleItem;
import org.jetbrains.annotations.Nullable;

public class ClientSort {

    public static final String MOD_ID = "clientsort";
    public static final String MOD_NAME = "ClientSort";
    public static final ModLogger LOG = new ModLogger(MOD_NAME);

    /**
     * Whether to show debug info on the GUI and write debug logs.
     * <p>
     * While this value can be set via the config menu, it is intentionally non-persistent.
     */
    public static boolean debugEnabled;

    public static boolean debug() {
        return debugEnabled;
    }

    public static void init() {
        ServerConfig.getAndSave();
    }

    public static void onConfigSaved(ServerConfig config) {
        ServerConfig.Options options = config.options;
        // Update class cache
        PolicyManager.reloadPolicyClasses(options.classPolicies.keySet());
    }

    /**
     * @return the reference object for the slot.
     */
    public static @Nullable Object getObj(Slot slot, AbstractContainerMenu menu) {
        return getObj(slot.container, menu);
    }

    /**
     * @return the reference object for the slot.
     */
    public static @Nullable Object getObj(
            @Nullable Container container,
            AbstractContainerMenu menu
    ) {
        return switch (container) {
            case null -> null;
            case ContainerSingleItem ignored -> null;
            case SimpleContainer ignored -> menu;
            default -> container;
        };
    }
}
