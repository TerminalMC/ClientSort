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

package dev.terminalmc.clientsort.client.order;

import com.google.common.base.Objects;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link ItemStack} matching.
 */
public class StackMatcher {

    public final @NotNull Item item;
    private final @Nullable DataComponentMap components;

    private StackMatcher(@NotNull ItemStack stack, @Nullable DataComponentMap components) {
        this.item = stack.isEmpty() ? Items.AIR : stack.getItem();
        this.components = components;
    }

    /**
     * Creates a component-aware matcher.
     */
    public static StackMatcher of(@NotNull ItemStack stack) {
        return new StackMatcher(stack, stack.getComponents());
    }

    /**
     * Creates a component-agnostic matcher.
     */
    public static StackMatcher plain(@NotNull ItemStack stack) {
        return new StackMatcher(stack, null);
    }

    /**
     * This violates the contract of {@link Object#equals} in that it is not symmetric: given two
     * instances {@code a} and {@code b} constructed from the same {@link ItemStack}, where
     * {@code a} is constructed using {@link #plain} and {@code b} is constructed using {@link #of},
     * {@code a.matches(b)} will return {@code true} because {@code b} has all components of
     * {@code a}, but {@code b.matches(a)} may return {@code false} because the inverse will not be
     * true if the {@link ItemStack} has components.
     * <p>
     * This is intentional, to allow map lookups with a full instance to fall back to a plain
     * instance if no matches are found. However, it relies on the map implementation performing the
     * comparison in the order {@code a.equals(b)}, where {@code a} is the value passed into the
     * lookup and {@code b} is the value already existing in the map.
     */
    @Override
    public boolean equals(Object pObj) {
        if (pObj instanceof StackMatcher pMatcher) {
            // comparing matcher to matcher; p must have same item and must have all and equal
            // components of this.
            return isSameItem(pMatcher.item) && hasAllEqualComponents(pMatcher.components);
        } else if (pObj instanceof ItemStack pStack) {
            // comparing matcher to stack; p must have same item and must have all and equal
            // components of this.
            return isSameItem(pStack) && hasAllEqualComponents(pStack);
        } else if (pObj instanceof Item pItem) {
            // comparing matcher to item; p must be same item.
            return isSameItem(pItem);
        }
        return false;
    }

    /**
     * @return {@code true} iff this and that both represent 'empty' or are the same item.
     */
    private boolean isSameItem(ItemStack stack) {
        return isSameItem(stack.getItem()) || (this.item == Items.AIR && stack.isEmpty());
    }

    /**
     * @return {@code true} iff this and that are the same item.
     */
    private boolean isSameItem(Item item) {
        return this.item == item;
    }

    /**
     * @return {@code true} iff this and that both represent 'empty', or that has an equal component
     * for each component of this.
     */
    private boolean hasAllEqualComponents(ItemStack stack) {
        if (this.components == null)
            return true;

        if (this.item == Items.AIR && stack.isEmpty())
            return true;

        return hasAllEqualComponents(stack.getComponents());
    }

    /**
     * @return {@code true} iff that has an equal component for each component of this.
     */
    private boolean hasAllEqualComponents(DataComponentMap components) {
        if (this.components == null)
            return true;

        for (TypedDataComponent<?> component : this.components) {
            Object corresponding = components.get(component.type());
            if (!component.value().equals(corresponding)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(item, components);
    }
}
