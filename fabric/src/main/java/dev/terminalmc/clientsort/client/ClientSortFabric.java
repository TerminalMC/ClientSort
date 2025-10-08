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

import dev.terminalmc.clientsort.client.network.ClientRegistration;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ClientSortFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register all keybinds
        KeybindManager.KEYBINDS.forEach(KeyBindingHelper::registerKeyBinding);

        // Register after-tick event
        ClientTickEvents.END_CLIENT_TICK.register(ClientSort::afterClientTick);

        // Register all custom S2C payload handlers
        ClientRegistration.PAYLOADS_S2C.forEach(ClientSortFabric::registerS2C);

        // Register built-in resource packs
        FabricLoader.getInstance().getModContainer(ClientSort.MOD_ID)
                .ifPresent((container) -> ResourceManagerHelper.registerBuiltinResourcePack(
                        new ResourceLocation(
                                ClientSort.MOD_ID,
                                "clientsort-dark-mode"
                        ),
                        container,
                        localized("resourcepack", "dark-mode"),
                        ResourcePackActivationType.NORMAL
                ));

        // Initialize client
        ClientSort.init();
    }

    /**
     * Registers a S2C payload and its handler.
     */
    private static <T extends Packet<ClientGamePacketListener>> void registerS2C(
            ClientRegistration.RegisterablePayloadS2C<T> rp
    ) {
        ClientPlayNetworking.registerGlobalReceiver(
                rp.channel,
                (mc, listener, byteBuf, sender) -> rp.handler.accept(
                        rp.decoder.apply(byteBuf),
                        mc,
                        mc.player
                )
        );
    }
}
