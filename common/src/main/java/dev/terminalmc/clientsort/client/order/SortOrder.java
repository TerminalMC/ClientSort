/*
 * Copyright 2022 Siphalor
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

package dev.terminalmc.clientsort.client.order;

import dev.terminalmc.clientsort.client.config.Config;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.locks.Lock;

public abstract class SortOrder {

    public final String name;

    protected SortOrder(String name) {
        this.name = name;
    }

    /**
     * Sorts the given slot IDs using the given stacks in the slots. Sorting may be done in place.
     *
     * @param slotIds an array of slot IDs to sort.
     * @param stacks  the item stacks in the respective slots.
     * @param context additional context for the sort.
     * @return the sorted array of slot IDs.
     */
    public int[] sort(int[] slotIds, ItemStack[] stacks, SortContext context) {
        return slotIds;
    }

    /**
     * All registered sort orders, mapped by name.
     */
    public static final Map<String, SortOrder> SORT_MODES = new HashMap<>();
    /**
     * No-op.
     */
    public static final SortOrder NONE;
    /**
     * Slots: item hover name (case ignorant) (desc)
     */
    public static final SortOrder ALPHABET;
    /**
     * Slots: creative inventory item search order
     */
    public static final SortOrder CREATIVE;
    /**
     * Groups: total item quantity (desc)
     * <p>
     * Slots: stack size (desc)
     */
    public static final SortOrder QUANTITY;
    /**
     * Slots: raw item ID (desc)
     */
    public static final SortOrder RAW_ID;

    public static <T extends SortOrder> T register(T sortOrder) {
        SORT_MODES.put(sortOrder.name, sortOrder);
        return sortOrder;
    }

    /**
     * Sorts {@code sortIds} by comparing the elements of {@code values}, falling back to comparing
     * the elements of {@code stacks} if necessary.
     */
    private static void sortByValues(
            int[] sortIds,
            int[] values,
            ItemStack[] stacks,
            SortContext context
    ) {
        IntArrays.quickSort(
                sortIds, (a, b) -> {
                    int cmp = Integer.compare(values[a], values[b]);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return StackComparison.compareEqualItems(stacks[a], stacks[b], context);
                }
        );
    }

    static {
        NONE = register(new SortOrder("none") {
        });

        ALPHABET = register(new SortOrder("alphabet") {
            @Override
            public int[] sort(int[] slotIds, ItemStack[] stacks, SortContext context) {
                // Create a reference array of slot item names
                String[] strings = new String[slotIds.length];
                for (int i = 0; i < slotIds.length; i++) {
                    ItemStack stack = stacks[i];
                    strings[i] = stack.isEmpty() ? "" : stack.getHoverName().getString();
                }

                // Sort by reference array
                IntArrays.quickSort(
                        slotIds, (a, b) -> {
                            if (strings[a].isEmpty()) {
                                if (strings[b].isEmpty())
                                    return 0;
                                return 1;
                            }
                            if (strings[b].isEmpty())
                                return -1;
                            int cmp = strings[a].compareToIgnoreCase(strings[b]);
                            if (cmp == 0) {
                                return StackComparison.compareEqualItems(
                                        stacks[a],
                                        stacks[b],
                                        context
                                );
                            }
                            return cmp;
                        }
                );
                return slotIds;
            }
        });

        CREATIVE = register(new SortOrder("creative") {
            @Override
            public int[] sort(int[] slotIds, ItemStack[] stacks, SortContext context) {
                // Create a reference array of item positions
                int[] sortValues = new int[slotIds.length];

                // If using optimized sorting, read the stored search order
                if (Config.options().optimizeCreativeSorting) {
                    Lock lock = CreativeSearchOrder.getReadLock();
                    lock.lock();
                    for (int i = 0; i < stacks.length; i++) {
                        sortValues[i] = CreativeSearchOrder.getPosition(stacks[i]);
                    }
                    lock.unlock();
                }
                // Otherwise compare the items manually
                else {
                    Collection<ItemStack> displayStacks =
                            CreativeModeTabs.searchTab().getDisplayItems();
                    List<ItemStack> displayStackList;
                    if (displayStacks instanceof List) {
                        displayStackList = (List<ItemStack>) displayStacks;
                    } else {
                        displayStackList = new ArrayList<>(displayStacks);
                    }
                    Object2IntMap<StackMatcher> lookup = new Object2IntOpenHashMap<>(stacks.length);
                    for (int i = 0; i < stacks.length; i++) {
                        final ItemStack stack = stacks[i];
                        sortValues[i] = lookup.computeIfAbsent(
                                StackMatcher.of(stack), matcher -> {
                                    @SuppressWarnings("SuspiciousMethodCalls") int index =
                                            displayStackList.indexOf(matcher);
                                    if (index != -1)
                                        return index;
                                    return lookup.computeIfAbsent(
                                            StackMatcher.ignoreNbt(stack), altMatcher -> {
                                                @SuppressWarnings("SuspiciousMethodCalls") int
                                                        plainIndex =
                                                        displayStackList.indexOf(altMatcher);
                                                if (plainIndex == -1)
                                                    return Integer.MAX_VALUE;
                                                return plainIndex;
                                            }
                                    );
                                }
                        );
                    }
                }

                // Sort by reference array
                SortOrder.sortByValues(slotIds, sortValues, stacks, context);
                return slotIds;
            }
        });

        QUANTITY = register(new SortOrder("quantity") {
            @Override
            public int[] sort(int[] slotIds, ItemStack[] stacks, SortContext context) {
                // Record the total count of each item
                HashMap<Item, Integer> itemTotalAmountMap = new HashMap<>();
                for (ItemStack stack : stacks) {
                    if (stack.isEmpty())
                        continue;
                    if (!itemTotalAmountMap.containsKey(stack.getItem())) {
                        itemTotalAmountMap.put(stack.getItem(), stack.getCount());
                    } else {
                        itemTotalAmountMap.put(
                                stack.getItem(),
                                itemTotalAmountMap.get(stack.getItem()) + stack.getCount()
                        );
                    }
                }

                // Sort by descending order of total item count
                IntArrays.quickSort(
                        slotIds, (a, b) -> {
                            ItemStack stackA = stacks[a];
                            ItemStack stackB = stacks[b];
                            if (stackA.isEmpty()) {
                                return stackB.isEmpty() ? 0 : 1;
                            }
                            if (stackB.isEmpty()) {
                                return -1;
                            }
                            Integer amountA = itemTotalAmountMap.get(stackA.getItem());
                            Integer amountB = itemTotalAmountMap.get(stackB.getItem());
                            int cmp = Integer.compare(amountB, amountA);
                            if (cmp != 0) {
                                return cmp;
                            }
                            if (ItemStack.isSameItemSameTags(stackA, stackB)) {
                                return StackComparison.compareEqualItems(stackA, stackB, context);
                            } else {
                                return StackComparison.compareEqualItems(
                                        stackA.copyWithCount(1),
                                        stackB.copyWithCount(1),
                                        context
                                );
                            }
                        }
                );
                return slotIds;
            }
        });

        RAW_ID = register(new SortOrder("rawId") {
            @Override
            public int[] sort(int[] slotIds, ItemStack[] stacks, SortContext context) {
                // Create a reference array of item IDs
                int[] rawIds = Arrays.stream(stacks)
                        .mapToInt(stack -> stack.isEmpty()
                                ? Integer.MAX_VALUE
                                : BuiltInRegistries.ITEM.getId(stack.getItem()))
                        .toArray();
                // Sort by reference array
                sortByValues(slotIds, rawIds, stacks, context);
                return slotIds;
            }
        });
    }
}
