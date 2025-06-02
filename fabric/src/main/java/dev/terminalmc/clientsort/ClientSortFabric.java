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

package dev.terminalmc.clientsort;

import dev.terminalmc.clientsort.network.handler.CollectHandler;
import dev.terminalmc.clientsort.network.payload.CollectPayload;
import dev.terminalmc.clientsort.network.handler.SortHandler;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ClientSortFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Custom payloads
        PayloadTypeRegistry.playC2S().register(
                CollectPayload.TYPE,
                CollectPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                CollectPayload.TYPE,
                (payload, context) -> CollectHandler.onCollectPayload(
                        payload, context.server(), context.player())
        );
        PayloadTypeRegistry.playC2S().register(
                SortPayload.TYPE,
                SortPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SortPayload.TYPE,
                (payload, context) -> SortHandler.onSortPayload(
                        payload, context.server(), context.player())
        );
        PayloadTypeRegistry.playS2C().register(
                CollectResultPayload.TYPE,
                CollectResultPayload.STREAM_CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                SortResultPayload.TYPE,
                SortResultPayload.STREAM_CODEC
        );
    }
}
