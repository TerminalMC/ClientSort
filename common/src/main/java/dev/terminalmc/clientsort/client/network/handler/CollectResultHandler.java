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
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public class CollectResultHandler {
    private CollectResultHandler() {}

    /**
     * An operation to be run on receipt of a {@link CollectResultPayload}
     * indicating success.
     */
    public static @Nullable Runnable onSuccess;

    /**
     * Handles a {@link CollectResultPayload} sent by a server.
     */
    @SuppressWarnings("unused")
    public static void handle(
            CollectResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        if (!payload.success()) {
            ClientSort.LOG.error(
                    "Received failure warning '{}': {}",
                    CollectResultPayload.ID,
                    payload.message()
            );
        }

        if (payload.success() && onSuccess != null) {
            try {
                onSuccess.run();
            } catch (Exception e) {
                // Whatever goes wrong should not crash the game
                ClientSort.LOG.error(
                        "Failed to run onSuccess runnable for payload {}: {}",
                        CollectPayload.ID,
                        e
                );
            }
        }

        onSuccess = null;
    }
}
