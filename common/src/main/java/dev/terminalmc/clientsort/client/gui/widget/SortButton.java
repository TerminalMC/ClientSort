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
import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Policy;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import dev.terminalmc.clientsort.client.order.SortOrder;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.TreeSet;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class SortButton extends TriggerButton {

    private static final WidgetSprites SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "widget/sort"),
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "widget/sort_disabled"),
            ResourceLocation.fromNamespaceAndPath(ClientSort.MOD_ID, "widget/sort_highlighted")
    );

    public SortButton(
            AbstractContainerScreen<?> screen,
            Container container,
            Slot referenceSlot,
            boolean isPlayerInv,
            @Nullable ClassPolicy policy,
            String lowestPolicyKey,
            Vec2i offset
    ) {
        super(
                screen,
                container,
                referenceSlot,
                isPlayerInv,
                SPRITES,
                policy == null ? null : policy.className(),
                lowestPolicyKey,
                offset,
                policy == null || policy.canSort(),
                policy != null && policy.showSortButton(),
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
                    @Nullable SingleUseOperator<?> controller = SingleUseOperator.getController(
                            screen,
                            ContainerScreenHelper.of(screen),
                            referenceSlot,
                            Operation.SORT
                    );
                    if (controller != null)
                        controller.trySort(sortOrder);
                }
        );
    }

    @Override
    public boolean getPolicyStatus(ClassPolicy policy) {
        return policy.showSortButton();
    }

    @Override
    public void savePolicy(@Nullable Vec2i offset, Collection<Integer> slots) {
        @Nullable ClassPolicy policy = null;
        if (activePolicyKey != null)
            policy = options().classPolicies.get(activePolicyKey);
        if (policy != null) {
            options().classPolicies.put(
                    activePolicyKey,
                    new ClassPolicy(
                            activePolicyKey,
                            offset,
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
                            policy.stackFillPolicy(),
                            policy.transferPolicy(),
                            new TreeSet<>(slots)
                    )
            );
        } else {
            options().classPolicies.put(
                    lowestPolicyKey,
                    new ClassPolicy(
                            lowestPolicyKey,
                            offset,
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
                            Policy.KEYBIND,
                            Policy.KEYBIND,
                            new TreeSet<>(slots)
                    )
            );
        }
        Config.save();
    }
}
