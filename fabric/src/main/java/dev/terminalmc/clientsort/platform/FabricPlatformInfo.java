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

package dev.terminalmc.clientsort.platform;

import dev.terminalmc.clientsort.platform.services.IPlatformInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Path;

public class FabricPlatformInfo implements IPlatformInfo {

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }
    
    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return ClientPlayNetworking.canSend(type);
    }
    
    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
