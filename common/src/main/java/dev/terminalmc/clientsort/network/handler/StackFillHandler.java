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

package dev.terminalmc.clientsort.network.handler;

import dev.terminalmc.clientsort.config.ServerClassPolicy;
import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.InconsistentStateException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.UnsupportedOpException;
import dev.terminalmc.clientsort.network.handler.validate.PolicyManager;
import dev.terminalmc.clientsort.network.payload.StackFillPayload;
import dev.terminalmc.clientsort.network.payload.StackFillResultPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import static dev.terminalmc.clientsort.config.ServerConfig.serverOptions;
import static dev.terminalmc.clientsort.network.handler.validate.SchemaValidator.validateSlotArray;

/**
 * A handler for a {@link StackFillPayload}.
 */
public class StackFillHandler extends PayloadHandler {

    private StackFillHandler() {
    }

    public static void handle(
            StackFillPayload payload,
            MinecraftServer server,
            ServerPlayer player
    ) {
        // Execute on main server thread
        server.execute(() -> processPayload(
                server,
                player,
                payload.srcContainerId(),
                (menu) -> checkPolicy(menu, payload.srcSlotIds(), payload.dstSlotIds()),
                (menu) -> {
                    validateSlotArray(player, menu, payload.srcSlotIds());
                    validateSlotArray(player, menu, payload.dstSlotIds());
                },
                (menu) -> fillStacks(menu, payload.srcSlotIds(), payload.dstSlotIds()),
                StackFillPayload.ID,
                StackFillResultPayload.ID,
                (result, message) -> new StackFillResultPayload(result.code, message)
        ));
    }

    private static void fillStacks(AbstractContainerMenu menu, int[] srcSlotIds, int[] dstSlotIds)
            throws PayloadHandlerException {
        // Work backwards from the end of the source array, looking for a
        // nonempty stack
        for (int i = srcSlotIds.length - 1; i >= 0; i--) {
            int srcSlotId = srcSlotIds[i];
            Slot srcSlot = menu.slots.get(srcSlotId);
            ItemStack srcStack = srcSlot.getItem();
            ItemStack srcStackCopy = srcSlot.getItem().copy();

            if (srcStack.isEmpty())
                continue;

            // Nonempty stack found; work forwards from the start of the
            // destination array, looking for a partial stack of the same item
            for (int dstSlotId : dstSlotIds) {
                Slot dstSlot = menu.slots.get(dstSlotId);
                ItemStack dstStack = dstSlot.getItem();
                ItemStack dstStackCopy = dstSlot.getItem().copy();

                if (dstStack.isEmpty())
                    continue;
                if (dstStack.getCount() >= dstSlot.getMaxStackSize(dstStack))
                    continue;
                if (!ItemStack.isSameItemSameTags(srcStack, dstStack))
                    continue;

                // Matching partial stack found

                // Predict the result
                ItemStack expected = srcStack.copyWithCount(Math.min(
                        srcStack.getCount() + dstStack.getCount(),
                        dstSlot.getMaxStackSize(dstStack)
                ));

                // Place as much of the source stack as possible
                dstSlot.safeInsert(srcStack);

                if (serverOptions().validateOperationResults) {
                    // Check that the operation succeeded
                    if (notEqual(dstSlot.getItem(), expected)) {
                        String message = String.format(
                                "Stack Fill operation failed to safe-insert from slot %d with item '%s' to slot %d with item '%s': Expected '%s' in destination after set, got '%s'!",
                                srcSlotId,
                                srcStackCopy,
                                dstSlotId,
                                dstStackCopy,
                                expected,
                                dstSlot.getItem()
                        );
                        setPolicy(menu, dstSlotIds, message);
                        throw new InconsistentStateException(message);
                    }
                }

                // If no items remain in the source stack, stop looking
                if (srcStack.isEmpty())
                    break;
                // Otherwise keep looking for another matching partial stack
            }
        }
    }

    /**
     * @throws UnsupportedOpException if there is a policy for this context disallowing this
     *                                operation.
     */
    private static void checkPolicy(
            AbstractContainerMenu menu,
            int[] srcSlotIds,
            int[] dstSlotIds
    ) throws UnsupportedOpException {
        Container srcContainer = srcSlotIds.length > 0
                ? menu.slots.get(srcSlotIds[0]).container
                : null;
        Object srcObject = srcContainer instanceof SimpleContainer ? menu : srcContainer;

        Container dstContainer = dstSlotIds.length > 0
                ? menu.slots.get(dstSlotIds[0]).container
                : null;
        Object dstObject = dstContainer instanceof SimpleContainer ? menu : dstContainer;

        // Fail if there is a disallow policy for either reference object
        PolicyManager.checkPolicy(srcObject.getClass(), (bl) -> bl.stackFillEnabled);
        PolicyManager.checkPolicy(dstObject.getClass(), (bl) -> bl.stackFillEnabled);
    }

    /**
     * Creates or updates a policy for this context to disallow this operation.
     */
    private static void setPolicy(AbstractContainerMenu menu, int[] dstSlotIds, String message) {
        Container dstContainer = dstSlotIds.length > 0
                ? menu.slots.get(dstSlotIds[0]).container
                : null;
        Object object = dstContainer instanceof SimpleContainer ? menu : dstContainer;

        PolicyManager.setPolicy(
                new ServerClassPolicy(
                        object.getClass().getName(),
                        true,
                        false,
                        true
                ),
                StackFillPayload.ID.toString(),
                message
        );
    }
}
