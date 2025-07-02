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
import dev.terminalmc.clientsort.client.network.InteractionManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        ClientSort.searchOrderUpdated = false;
    }

    @Inject(
            method = "handleSetCursorItem",
            at = @At("HEAD")
    )
    public void beforeHeldItemChange(ClientboundSetCursorItemPacket packet, CallbackInfo ci) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.HELD_ITEM_CHANGE);
    }

    @Inject(
            method = "handleContainerSetSlot",
            at = @At("RETURN")
    )
    public void beforeSlotItemChange(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.CONTAINER_SLOT_UPDATE);
    }
}
