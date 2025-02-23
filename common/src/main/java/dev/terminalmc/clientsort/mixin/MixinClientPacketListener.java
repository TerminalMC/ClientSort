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

package dev.terminalmc.clientsort.mixin;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.network.InteractionManager;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.terminalmc.clientsort.config.Config.options;

/**
 * Network-related events.
 */
@Mixin(ClientPacketListener.class)
public abstract class MixinClientPacketListener {

    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void onLogin(ClientboundLoginPacket packet, CallbackInfo ci) {
        ClientSort.searchOrderUpdated = false;
        ClientSort.setInteractionManagerTickRate(options());
    }

    @Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
    public void onHeldItemChangeBegin(ClientboundSetCarriedItemPacket packet, CallbackInfo ci) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.HELD_ITEM_CHANGE);
    }

    @Inject(method = "handleContainerSetSlot", at = @At("RETURN"))
    public void onGuiSlotUpdateBegin(ClientboundContainerSetSlotPacket packet, CallbackInfo ci) {
        InteractionManager.triggerSend(InteractionManager.TriggerType.CONTAINER_SLOT_UPDATE);
    }
}
