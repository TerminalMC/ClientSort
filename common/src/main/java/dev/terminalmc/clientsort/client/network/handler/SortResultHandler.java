/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.client.network.handler;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class SortResultHandler {
    private SortResultHandler() {}

    /**
     * Handles a {@link SortResultPayload} sent by a server.
     */
    public static void onSortResultPayload(
            SortResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        if (!payload.success()) {
            ClientSort.LOG.error("Failed to complete sort operation on server: {}",
                    payload.message());
        }

        player.inventoryMenu.broadcastChanges();
    }
}
