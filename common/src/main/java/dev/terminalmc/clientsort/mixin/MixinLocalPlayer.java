/*
 * Copyright 2022 Siphalor
 * Copyright 2024 TerminalMC
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

import com.mojang.authlib.GameProfile;
import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.network.InteractionManager;
import dev.terminalmc.clientsort.util.item.CreativeSearchOrder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer extends AbstractClientPlayer {
	public MixinLocalPlayer(ClientLevel world, GameProfile profile) {
		super(world, profile);
	}

	@Inject(method = "clientSideCloseContainer", at = @At("HEAD"))
	public void onContainerClosed(CallbackInfo callbackInfo) {
		InteractionManager.clear();
	}

    @Inject(method = "setPermissionLevel", at = @At("RETURN"))
    public void onSetPermissionLevel(int level, CallbackInfo ci) {
        if (!ClientSort.searchOrderUpdated) {
            ClientSort.searchOrderUpdated = true;
            CreativeSearchOrder.tryRefreshItemSearchPositionLookup();
        }
    }
}
