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

package dev.terminalmc.clientsort.client.network.handler;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.StackFillResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class StackFillResultHandler {
    private StackFillResultHandler() {}

    /**
     * Handles a {@link StackFillResultPayload} sent by a server.
     */
    @SuppressWarnings("unused")
    public static void handle(
            StackFillResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        if (!payload.success()) {
            ClientSort.LOG.error(
                    "Received failure warning '{}': {}",
                    StackFillPayload.ID,
                    payload.message()
            );
        }

        player.inventoryMenu.broadcastChanges();
    }
}
