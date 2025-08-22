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

import dev.terminalmc.clientsort.client.config.ClassPolicy;
import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.config.Policy;
import dev.terminalmc.clientsort.client.config.Vec2i;
import dev.terminalmc.clientsort.client.inventory.operator.Operation;
import dev.terminalmc.clientsort.client.inventory.operator.SingleUseOperator;
import dev.terminalmc.clientsort.client.inventory.screen.ContainerScreenHelper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.TreeSet;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class MatchTransferButton extends TriggerButton {

    public MatchTransferButton(
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
                new Vec2i(isPlayerInv ? 4 : 3, 0),
                policy == null ? null : policy.className(),
                lowestPolicyKey,
                offset,
                policy == null || policy.canMatchTransfer(),
                policy != null && policy.showMatchTransferButton(),
                (button) -> {
                    SingleUseOperator<?> operator = SingleUseOperator.getOperator(
                            screen,
                            ContainerScreenHelper.of(screen),
                            referenceSlot,
                            Operation.MATCH_TRANSFER
                    );
                    if (operator != null)
                        operator.tryMatchTransfer();
                }
        );
    }

    @Override
    public boolean getPolicyStatus(ClassPolicy policy) {
        return policy.showMatchTransferButton();
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
                            policy.sortPolicy(),
                            policy.stackFillPolicy(),
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
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
                            Policy.KEYBIND,
                            Policy.KEYBIND,
                            operationAllowed
                                    ? active ? Policy.KEYBIND_BUTTON : Policy.KEYBIND
                                    : Policy.NONE,
                            Policy.KEYBIND,
                            new TreeSet<>(slots)
                    )
            );
        }
        Config.save();
    }
}
