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
     * The index of the slot in its inventory.
     * <p>
     * As a container may have several inventories, this value may be the same for multiple slots.
     * <p>
     * This value does not necessarily follow the left-right, top-down order; for example, the
     * hotbar may be indexed 0-8 while the top-left inventory slot is 9.
     *
     * @return the index within the inventory.
     * @see net.minecraft.world.inventory.Slot#slot
     */
    @SuppressWarnings("JavadocReference")
    int clientsort$getIndexInInv();

    /**
     * The unique ID of the slot within its container.
     * <p>
     * This is unique within a container, and therefore may differ from the value of
     * {@link ISlot#clientsort$getIndexInInv}.
     * <p>
     * This value can be safely assumed to be the same as the index of the slot in
     * {@link net.minecraft.world.inventory.AbstractContainerMenu#slots}.
     *
     * @return the unique ID within the container.
     * @see net.minecraft.world.inventory.Slot#index
     */
    int clientsort$getIdInContainer();
}
