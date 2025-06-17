/*
 * Copyright 2021 Evan Steinkerchner (Roundaround)
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

package dev.terminalmc.clientsort.client.gui.widget;

import dev.terminalmc.clientsort.client.inventory.control.SingleUseController;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

public class StackFillButton extends ControlButton {
    public StackFillButton(
            AbstractContainerScreen<?> screen,
            Container container,
            String layoutKey,
            boolean isPlayerInv,
            Slot referenceSlot,
            Vec2i offset,
            boolean active
    ) {
        super(
                screen,
                container,
                layoutKey,
                isPlayerInv,
                referenceSlot,
                new Vec2i(isPlayerInv ? 2 : 1, 0),
                offset,
                (button) -> SingleUseController.getController(
                        screen,
                        ContainerScreenHelper.of(screen),
                        referenceSlot,
                        StackFillPayload.ID
                ).fillStacks(),
                active
        );
    }
}
