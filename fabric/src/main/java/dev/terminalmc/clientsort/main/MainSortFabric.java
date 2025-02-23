/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.main;

import dev.terminalmc.clientsort.main.network.LogicalServerNetworking;
import dev.terminalmc.clientsort.main.network.SortPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class MainSortFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Server networking
        PayloadTypeRegistry.playC2S().register(
                SortPayload.TYPE,
                SortPayload.STREAM_CODEC
        );
        ServerPlayNetworking.registerGlobalReceiver(
                SortPayload.TYPE,
                (payload, context) -> LogicalServerNetworking.onSortPayload(
                        payload, context.server(), context.player())
        );
    }
}
