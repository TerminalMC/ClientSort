/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.mixin.client.slot;

import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$SlotWrapper")
public abstract class CreativeSlotMixin implements ISlot {

    @Shadow
    @Final
    private Slot target;

    @Override
    public int clientsort$getIndexInContainer() {
        return ((ISlot) target).clientsort$getIndexInContainer();
    }

    @Override
    public int clientsort$getIndexInMenu() {
        return ((ISlot) target).clientsort$getIndexInMenu();
    }
}
