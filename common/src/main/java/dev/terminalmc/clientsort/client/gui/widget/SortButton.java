/*
 * Copyright 2021 Evan Steinkerchner (Roundaround)
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

package dev.terminalmc.clientsort.client.gui.widget;

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.client.config.*;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.order.SortOrder;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
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
            boolean referenceLeft,
            boolean isPlayerInv,
            @Nullable ClassPolicy policy,
            String lowestPolicyKey,
            Vec2i offset,
            Component name
    ) {
        super(
                screen,
                container,
                referenceSlot,
                referenceLeft,
                isPlayerInv,
                SPRITES,
                name,
                policy == null ? null : policy.getKey(),
                lowestPolicyKey,
                offset,
                policy == null ? true : policy.offsetFromSlot(),
                policy == null || policy.canSort(),
                policy != null && policy.showSortButton(),
                (button) -> {
                    SortOrder sortOrder = options().sortOrder;
                    if (Screen.hasShiftDown()) {
                        sortOrder = options().shiftSortOrder;
                    } else if (Screen.hasControlDown()) {
                        sortOrder = options().ctrlSortOrder;
                    } else if (Screen.hasAltDown()) {
                        sortOrder = options().altSortOrder;
                    }

                    SingleUseOperator.sort(screen, referenceSlot, false, sortOrder);
                }
        );
    }

    @Override
    public boolean getPolicyStatus(ClassPolicy policy) {
        return policy.showSortButton();
    }

    @Override
    public void savePolicy(
            @Nullable Vec2i offset,
            boolean offsetFromSlot,
            @Nullable Operation autoOp,
            boolean autoOpOther,
            Collection<Integer> slots
    ) {
        String key = activePolicyKey != null ? activePolicyKey : lowestPolicyKey;
        @Nullable ClassPolicy policy = options().classPolicies.get(key);
        if (policy == null) {
            key = lowestPolicyKey;
            policy = options().classPolicies.get(key);
        }

        if (policy == null) {
            options().classPolicies.put(
                    key,
                    ClassPolicy.create(
                            key,
                            offset,
                            offsetFromSlot,
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
                            Policy.KEYBIND,
                            Policy.KEYBIND,
                            Policy.KEYBIND,
                            autoOp,
                            autoOpOther,
                            new TreeSet<>(slots)
                    )
            );
        } else {
            options().classPolicies.put(
                    key,
                    ClassPolicy.create(
                            key,
                            offset,
                            offsetFromSlot,
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
                            policy.stackFillPolicy(),
                            policy.matchTransferPolicy(),
                            policy.transferPolicy(),
                            autoOp,
                            autoOpOther,
                            new TreeSet<>(slots)
                    )
            );
        }

        Config.save();
    }
}
