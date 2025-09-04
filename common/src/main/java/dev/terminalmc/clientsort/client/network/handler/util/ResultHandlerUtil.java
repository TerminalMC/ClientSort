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

package dev.terminalmc.clientsort.client.network.handler.util;

import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.config.ServerConfig;
import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import static dev.terminalmc.clientsort.client.ClientSort.debug;
import static dev.terminalmc.clientsort.client.config.Config.options;

public class ResultHandlerUtil {

    private ResultHandlerUtil() {
    }

    public static PayloadResult interpretResult(
            int code,
            String message,
            ResourceLocation payloadId
    ) {
        PayloadResult result = PayloadResult.get(code);
        if (result.isSuccess()) {
            if (debug()) {
                ClientSort.LOG.info("Received success result for payload '{}'", payloadId);
            }
        } else {
            ClientSort.LOG.warn(
                    "Received failure result with code {} ({}) for payload '{}': {}",
                    result.code,
                    result.name(),
                    payloadId,
                    message
            );
            switch (result) {
                case INCONSISTENT_STATE, UNSUPPORTED_OP -> {
                    if (options().useClientFallback) {
                        ClientSort.LOG.info(
                                "Client fallback is enabled: retrying without server acceleration."
                        );
                    } else {
                        if (Minecraft.getInstance().getSingleplayerServer() != null) {
                            ClientSort.LOG.info(
                                    "If you still want to perform this operation, you may either edit the server-side policy in the '{}' config file, disable server acceleration, or enable client fallback. Otherwise, you may edit your client-side policy to disable this operation in this inventory type.",
                                    ServerConfig.FILE_NAME
                            );
                        } else {
                            ClientSort.LOG.info(
                                    "If you still want to perform this operation, you may disable server acceleration or enable client fallback. Otherwise, you may edit your client-side policy to disable this operation in this inventory type."
                            );
                        }
                    }
                }
                case INVALID_DATA, FAILURE -> ClientSort.LOG.warn(
                        "Please make a note of the inventory type and report this to the developer."
                );
                case UNKNOWN -> ClientSort.LOG.error(
                        "Result code {} is not recognized. Are you using the same mod version as the server?",
                        code
                );
            }
        }
        return result;
    }

}
