/*
 * Copyright 2026 TerminalMC
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

import dev.terminalmc.clientsort.client.gui.TriggerButtonManager;
import dev.terminalmc.clientsort.mixin.client.accessor.ScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(
            method = "init(II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;init()V",
                    shift = Shift.AFTER
            )
    )
    private void afterInit(CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen)
            return;
        clientsort$afterInit();
    }

    @Inject(
            method = "rebuildWidgets",
            at = @At("RETURN")
    )
    private void afterRebuildWidgets(CallbackInfo ci) {
        clientsort$afterInit();
    }

    @Unique
    private void clientsort$afterInit() {
        TriggerButtonManager.getContainerButtons()
                .forEach((b) -> ((ScreenAccessor) this).clientsort$removeWidget(b));
        TriggerButtonManager.getPlayerButtons()
                .forEach((b) -> ((ScreenAccessor) this).clientsort$removeWidget(b));
        TriggerButtonManager.afterScreenInit((Screen) (Object) this);
    }
}
