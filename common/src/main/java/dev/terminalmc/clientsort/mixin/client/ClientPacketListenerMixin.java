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

package dev.terminalmc.clientsort.mixin.client;

import dev.terminalmc.clientsort.client.ClientSort;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.clientsort.client.config.Config.options;

/**
 * Network-related events.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(
            method = "handleLogin",
            at = @At("HEAD")
    )
    private void beforeLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        // Reset state on relog
        ClientSort.searchOrderUpdated = false;
        ClientSort.operatingClient = false;
        ClientSort.clientOpQueue.clear();
    }

    @Inject(
            method = "handleLogin",
            at = @At("RETURN")
    )
    private void afterLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        // Update tag cache
        ClientSort.updateItemTags(options());
        // Update item caches
        ClientSort.updateItemSets(options());
    }
}
