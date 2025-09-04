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

import dev.terminalmc.clientsort.ClientSort;
import dev.terminalmc.clientsort.exception.PayloadHandlerException;
import dev.terminalmc.clientsort.exception.PayloadHandlerException.InvalidDataException;
import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;
import dev.terminalmc.clientsort.platform.Services;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

/**
 * Provides common methods to custom payload handlers.
 */
public abstract class PayloadHandler {

    @FunctionalInterface
    public interface ThrowingConsumer<T> {

        void accept(T t) throws Exception;
    }

    /**
     * Validates the data of a payload, performs an associated operation, catches any errors thrown
     * in the process and finally sends a result payload back to the player.
     */
    @SuppressWarnings("unused")
    public static void processPayload(
            MinecraftServer server,
            ServerPlayer player,
            int containerId,
            ThrowingConsumer<AbstractContainerMenu> contextValidator,
            ThrowingConsumer<AbstractContainerMenu> schemaValidator,
            ThrowingConsumer<AbstractContainerMenu> operator,
            CustomPacketPayload.Type<?> payloadType,
            CustomPacketPayload.Type<?> responseType,
            BiFunction<PayloadResult, String, CustomPacketPayload> responseProvider
    ) {
        @Nullable AbstractContainerMenu menu = null;
        PayloadResult result = PayloadResult.SUCCESS;
        String message = "";

        try {
            menu = getMenu(player, containerId);
            menu.suppressRemoteUpdates();

            // Validate context
            contextValidator.accept(menu);

            // Validate schema
            schemaValidator.accept(menu);

            // Operate
            operator.accept(menu);

        } catch (Exception e) {
            if (e instanceof PayloadHandlerException ex) {
                result = ex.result;
                message = ex.getMessage();
            } else {
                result = PayloadResult.FAILURE;
                message = PayloadHandlerException.GENERIC_MESSAGE;
                ClientSort.LOG.error(
                        "Encountered an unexpected exception while handling payload '{}' from player '{}': {}",
                        payloadType.id(),
                        player,
                        e
                );
            }
        } finally {
            if (menu != null) {
                menu.resumeRemoteUpdates();
                menu.broadcastChanges();
            }
            if (Services.PLATFORM.canSendToPlayer(player, responseType)) {
                Services.PLATFORM.sendToPlayer(player, responseProvider.apply(result, message));
            }
        }
    }

    /**
     * @return the menu belonging to the player and matching the container ID.
     * @throws InvalidDataException if no matching menu was found, or one was found but is not valid
     *                              for the player.
     */
    private static @NotNull AbstractContainerMenu getMenu(ServerPlayer player, int containerId)
            throws InvalidDataException {
        AbstractContainerMenu menu;

        // Retrieve the matching container menu
        if (containerId == player.inventoryMenu.containerId) {
            menu = player.inventoryMenu;
        } else if (containerId == player.containerMenu.containerId) {
            menu = player.containerMenu;
        } else {
            throw new InvalidDataException(String.format(
                    "Container ID '%d' does not match player inventory or container!",
                    containerId
            ));
        }

        // Check that the menu is valid
        if (!menu.stillValid(player)) {
            throw new InvalidDataException(String.format(
                    "Container ID '%d' is not valid for the player!",
                    containerId
            ));
        }

        return menu;
    }

    public static boolean notEqual(ItemStack a, ItemStack b) {
        return !ItemStack.isSameItemSameComponents(a, b)
                || a.getCount() != b.getCount();
    }
}
