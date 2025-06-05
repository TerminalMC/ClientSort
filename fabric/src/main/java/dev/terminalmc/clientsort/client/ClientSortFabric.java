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

package dev.terminalmc.clientsort.client;

import dev.terminalmc.clientsort.client.network.handler.CollectResultHandler;
import dev.terminalmc.clientsort.client.network.handler.SortResultHandler;
import dev.terminalmc.clientsort.network.payload.CollectResultPayload;
import dev.terminalmc.clientsort.network.payload.SortResultPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientSortFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Keybindings
        ClientSort.KEYBINDS.forEach(KeyBindingHelper::registerKeyBinding);

        // Tick events
        ClientTickEvents.END_CLIENT_TICK.register(ClientSort::onEndTick);

        // Custom payloads
        ClientPlayNetworking.registerGlobalReceiver(
                CollectResultPayload.TYPE,
                (payload, context) -> CollectResultHandler.handle(
                        payload, context.client(), context.player())
        );
        ClientPlayNetworking.registerGlobalReceiver(
                SortResultPayload.TYPE,
                (payload, context) -> SortResultHandler.handle(
                        payload, context.client(), context.player())
        );

        // Client initialization
        ClientSort.init();
    }
}
