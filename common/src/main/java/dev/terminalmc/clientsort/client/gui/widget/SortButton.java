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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.inventory.control.SingleUseController;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.network.payload.SortPayload;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class SortButton extends ControlButton {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "widget/sort"),
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "widget/sort_highlighted")
    );

    public SortButton(
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
                SPRITES,
                offset,
                (button) -> {
                    SortOrder sortOrder;
                    if (Screen.hasShiftDown()) {
                        sortOrder = options().shiftSortOrder;
                    } else if (Screen.hasControlDown()) {
                        sortOrder = options().ctrlSortOrder;
                    } else if (Screen.hasAltDown()) {
                        sortOrder = options().altSortOrder;
                    } else {
                        sortOrder = options().sortOrder;
                    }
                    SingleUseController.getController(
                            screen,
                            ContainerScreenHelper.of(screen),
                            referenceSlot,
                            SortPayload.TYPE
                    ).sort(sortOrder);
                },
                active
        );
    }
}
