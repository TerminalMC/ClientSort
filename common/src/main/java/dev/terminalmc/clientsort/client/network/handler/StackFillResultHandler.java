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
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.StackFillResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class StackFillResultHandler {

    private StackFillResultHandler() {
    }

    /**
     * An operation to be run on receipt of a {@link StackFillResultPayload}.
     */
    public static @Nullable Consumer<PayloadResult> onCompletion;

    /**
     * Handles a {@link StackFillResultPayload} sent by a server.
     */
    public static void handle(
            StackFillResultPayload payload,
            Minecraft mc,
            LocalPlayer player
    ) {
        mc.execute(() -> {
            PayloadResult result = ResultHandlerUtil.interpretResult(
                    payload.result(),
                    payload.message(),
                    StackFillPayload.ID
            );

            if (onCompletion != null) {
                try {
                    onCompletion.accept(result);
                } catch (Exception e) {
                    ClientSort.LOG.error(
                            "Failed to run completion callback for payload '{}' with result '{}': {}",
                            StackFillPayload.ID,
                            result.name(),
                            e
                    );
                }
                onCompletion = null;
            }
        });
    }
}
