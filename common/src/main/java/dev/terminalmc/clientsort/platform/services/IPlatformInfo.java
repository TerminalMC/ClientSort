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

package dev.terminalmc.clientsort.platform.services;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

public interface IPlatformInfo {

    /**
     * @return the configuration directory of the instance.
     */
    Path getConfigDir();

    /**
     * @return {@code true} if the payload type can be sent from the client to
     * the server.
     */
    boolean canSendToServer(ResourceLocation channel);

    /**
     * Sends the payload to the server.
     */
    void sendToServer(ResourceLocation channel, Packet<ServerGamePacketListener> packet);
}
