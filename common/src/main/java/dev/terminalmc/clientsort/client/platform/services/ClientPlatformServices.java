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

package dev.terminalmc.clientsort.client.platform.services;

import dev.terminalmc.clientsort.client.platform.ClientServices;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

@SuppressWarnings("unused")
public interface ClientPlatformServices {

    ClientPlatformServices INSTANCE = ClientServices.load(ClientPlatformServices.class);

    static ClientPlatformServices getInstance() {
        return INSTANCE;
    }

    /**
     * @return {@code true} if the payload type can be sent from the client to the server.
     */
    boolean canSendToServer(CustomPacketPayload.Type<?> type);

    /**
     * Sends the payload to the server.
     */
    void sendToServer(CustomPacketPayload payload);
}
