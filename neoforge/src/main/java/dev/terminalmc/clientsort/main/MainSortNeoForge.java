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
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(MainSort.MOD_ID)
@EventBusSubscriber(modid = MainSort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class MainSortNeoForge {
    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(
                SortPayload.TYPE,
                SortPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) ->
                                LogicalServerNetworking.onSortPayload(
                                        payload,
                                        context.player().getServer(),
                                        ((ServerPlayer)context.player())
                                ),
                        (payload, context) ->
                                LogicalServerNetworking.onSortPayload(
                                        payload,
                                        context.player().getServer(),
                                        ((ServerPlayer)context.player())
                                )
                )
        );
    }
}
