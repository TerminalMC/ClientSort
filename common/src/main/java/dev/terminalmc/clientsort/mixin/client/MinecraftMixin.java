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

package dev.terminalmc.clientsort.mixin.client;


import dev.terminalmc.clientsort.client.ClientSort;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Game lifecycle events.
 */
@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(
            method = "run",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Minecraft;gameThread:Ljava/lang/Thread;",
                    shift = At.Shift.AFTER,
                    ordinal = 0
            )
    )
    private void onStart(CallbackInfo ci) {
        ClientSort.afterGameStart();
    }
}
