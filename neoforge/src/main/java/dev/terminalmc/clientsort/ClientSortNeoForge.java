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
import dev.terminalmc.clientsort.network.payload.SortPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(ClientSort.MOD_ID)
@EventBusSubscriber(modid = ClientSort.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ClientSortNeoForge {
    // Custom payloads
    @SubscribeEvent
    static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.playToServer(
                CollectPayload.TYPE,
                CollectPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> CollectHandler.onCollectPayload(
                                payload, context.player().getServer(), ((ServerPlayer)context.player())),
                        (payload, context) -> CollectHandler.onCollectPayload(
                                payload, context.player().getServer(), ((ServerPlayer)context.player()))
                )
        );
        registrar.playToServer(
                SortPayload.TYPE,
                SortPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (payload, context) -> SortHandler.onSortPayload(
                                payload, context.player().getServer(), ((ServerPlayer)context.player())),
                        (payload, context) -> SortHandler.onSortPayload(
                                payload, context.player().getServer(), ((ServerPlayer)context.player()))
                )
        );
    }
}
