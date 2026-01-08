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

package dev.terminalmc.clientsort.client.network.handler;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.network.handler.util.ResultHandlerUtil;
import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CollectResultHandler {

    private CollectResultHandler() {
    }

    /**
     * An operation to be run on receipt of a {@link CollectResultPayload}.
     */
    public static final Map<String, Consumer<PayloadResult>> onCompletion = new HashMap<>();

    /**
     * Handles a {@link CollectResultPayload} sent by a server.
     */
    public static void handle(
            CollectResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        mc.execute(() -> {
            PayloadResult result = ResultHandlerUtil.interpretResult(
                    payload.result(),
                    payload.message(),
                    CollectPayload.ID
            );

            Consumer<PayloadResult> callback = onCompletion.get(payload.id());
            onCompletion.remove(payload.id());
            if (callback != null) {
                try {
                    callback.accept(result);
                } catch (Exception e) {
                    ClientSort.LOG.error(
                            "Failed to run completion callback for payload '{}' with result '{}': {}",
                            CollectPayload.ID,
                            result.name(),
                            e
                    );
                }
            }
        });
    }
}
