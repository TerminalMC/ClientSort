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

package dev.terminalmc.clientsort.util.inject;

public interface ISlot {

    /**
     * The index of the slot in its container.
     * <p>
     * As a menu may have more than one container (for example, opening a chest displays a menu with
     * both the chest container and the player inventory container), this value may be the same for
     * multiple slots in the menu.
     * <p>
     * This value does not necessarily follow the normal left-right, top-down order; for example,
     * the hotbar may be indexed 0-8 while the top-left slot of the main inventory is 9, despite the
     * slots all belonging to the same container and being stored in the same collection.
     *
     * @return the index within the container.
     * @see net.minecraft.world.inventory.Slot#slot
     */
    @SuppressWarnings("JavadocReference")
    int clientsort$getIndexInContainer();

    /**
     * The unique index of the slot within the menu.
     * <p>
     * This is unique within a {@link net.minecraft.world.inventory.AbstractContainerMenu}, and
     * therefore may differ from the value of {@link ISlot#clientsort$getIndexInContainer}.
     * <p>
     * This value can be safely assumed to be the same as the index of the slot in
     * {@link net.minecraft.world.inventory.AbstractContainerMenu#slots}.
     *
     * @return the unique index within the container.
     * @see net.minecraft.world.inventory.Slot#index
     */
    int clientsort$getIndexInMenu();
}
