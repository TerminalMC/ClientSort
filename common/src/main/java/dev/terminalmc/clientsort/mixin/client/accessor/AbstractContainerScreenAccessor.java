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

package dev.terminalmc.clientsort.mixin.client.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int clientsort$getLeftPos();

    @Accessor("topPos")
    int clientsort$getTopPos();

    @Accessor("imageWidth")
    int clientsort$getImageWidth();

    @Accessor("playerInventoryTitle")
    Component clientsort$getPlayerInventoryTitle();

    @Invoker("slotClicked")
    void clientsort$slotClicked(Slot slot, int slotId, int mouseButton, ContainerInput input);

    @Invoker("isHovering")
    boolean clientsort$isHovering(Slot slot, double mouseX, double mouseY);
}
