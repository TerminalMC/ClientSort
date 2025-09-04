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

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.network.handler.util.ResultHandlerUtil;
import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class SortResultHandler {

    private SortResultHandler() {
    }

    /**
     * An operation to be run on receipt of a {@link SortResultPayload}.
     */
    public static @Nullable Consumer<PayloadResult> onCompletion;

    /**
     * Handles a {@link SortResultPayload} sent by a server.
     */
    public static void handle(
            SortResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        PayloadResult result = ResultHandlerUtil.interpretResult(
                payload.result(),
                payload.message(),
                SortPayload.ID
        );

        if (onCompletion != null) {
            try {
                onCompletion.accept(result);
            } catch (Exception e) {
                ClientSort.LOG.error(
                        "Failed to run completion callback for payload '{}' with result '{}': {}",
                        SortPayload.ID,
                        result.name(),
                        e
                );
            }
            onCompletion = null;
        }

        player.inventoryMenu.broadcastChanges();
    }
}
