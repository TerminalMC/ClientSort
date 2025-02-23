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

package dev.terminalmc.clientsort.inventory.sort;

import dev.terminalmc.clientsort.config.Config;
import dev.terminalmc.clientsort.util.item.CreativeSearchOrder;
import dev.terminalmc.clientsort.util.item.StackComparison;
import dev.terminalmc.clientsort.util.item.StackMatcher;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * Defines sorting comparators.
 */
public abstract class SortOrder {
    public static final Map<String, SortOrder> SORT_MODES = new HashMap<>();
    
    public static final SortOrder NONE;
    public static final SortOrder ALPHABET;
    public static final SortOrder CREATIVE;
    public static final SortOrder QUANTITY;
    public static final SortOrder RAW_ID;

    public static <T extends SortOrder> T register(String name, T sortOrder) {
        SORT_MODES.put(name, sortOrder);
        return sortOrder;
    }

    @SuppressWarnings("unused")
    public static void unregister(String name) {
        SORT_MODES.remove(name);
    }

    public final String name;

    protected SortOrder(String name) {
        this.name = name;
    }

    /**
     * Sorts the given slot ids using the given stacks in the slots. Sorting may
     * be done in place.
     * @param sortIds an array of the current slot indices
     * @param stacks the stacks in the respective slots
     * @param context additional context for the sorting
     * @return the sorted array of slot indices
     */
    public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
        return sortIds;
    }

    /**
     * Sorts {@code sortIds} by comparing the elements of {@code values},
     * falling back to comparing elements of {@code stacks} if necessary.
     */
    private static void sortByValues(int[] sortIds, int[] values, ItemStack[] stacks) {
        IntArrays.quickSort(sortIds, (a, b) -> {
            int cmp = Integer.compare(values[a], values[b]);
            if (cmp != 0) {
                return cmp;
            }
            return StackComparison.compareEqualItems(stacks[a], stacks[b]);
        });
    }

    static {
        // No action
        NONE = register("none", new SortOrder("none") {});
        
        // Alphabetical order of stack name
        ALPHABET = register("alphabet", new SortOrder("alphabet") {
            @Override
            public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
                String[] strings = new String[sortIds.length];
                for (int i = 0; i < sortIds.length; i++) {
                    ItemStack stack = stacks[i];
                    strings[i] = stack.isEmpty() ? "" : stack.getHoverName().getString();
                }

                IntArrays.quickSort(sortIds, (a, b) -> {
                    if (strings[a].isEmpty()) {
                        if (strings[b].isEmpty())
                            return 0;
                        return 1;
                    }
                    if (strings[b].isEmpty()) return -1;
                    int comp = strings[a].compareToIgnoreCase(strings[b]);
                    if (comp == 0) {
                        return StackComparison.compareEqualItems(stacks[a], stacks[b]);
                    }
                    return comp;
                });

                return sortIds;
            }
        });
        
        // Creative search order
        CREATIVE = register("creative", new SortOrder("creative") {
            @Override
            public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
                int[] sortValues = new int[sortIds.length];
                if (Config.options().optimizedCreativeSorting) {
                    Lock lock = CreativeSearchOrder.getReadLock();
                    lock.lock();
                    for (int i = 0; i < stacks.length; i++) {
                        sortValues[i] = CreativeSearchOrder.getPosition(stacks[i]);
                    }
                    lock.unlock();
                } else {
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
                        sortValues[i] = lookup.computeIfAbsent(StackMatcher.of(stack), matcher -> {
                            @SuppressWarnings("SuspiciousMethodCalls")
                            int index = displayStackList.indexOf(matcher);
                            if (index != -1) return index;
                            return lookup.computeIfAbsent(StackMatcher.ignoreNbt(stack), 
                                    altMatcher -> {
                                        @SuppressWarnings("SuspiciousMethodCalls")
                                        int plainIndex = displayStackList.indexOf(altMatcher);
                                        if (plainIndex == -1) return Integer.MAX_VALUE;
                                        return plainIndex;
                                    });
                        });
                    }
                }
                SortOrder.sortByValues(sortIds, sortValues, stacks);
                return sortIds;
            }
        });
        
        // Descending order of stack size
        QUANTITY = register("quantity", new SortOrder("quantity") {
            @Override
            public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
                HashMap<Item, Integer> itemToAmountMap = new HashMap<>();

                for (ItemStack stack : stacks) {
                    if (stack.isEmpty()) continue;
                    if (!itemToAmountMap.containsKey(stack.getItem())) {
                        itemToAmountMap.put(stack.getItem(), stack.getCount());
                    } else {
                        itemToAmountMap.put(stack.getItem(), 
                                itemToAmountMap.get(stack.getItem()) + stack.getCount());
                    }
                }

                IntArrays.quickSort(sortIds, (a, b) -> {
                    ItemStack stack = stacks[a];
                    ItemStack stack2 = stacks[b];
                    if (stack.isEmpty()) {
                        return stack2.isEmpty() ? 0 : 1;
                    }
                    if (stack2.isEmpty()) {
                        return -1;
                    }
                    Integer amountA = itemToAmountMap.get(stack.getItem());
                    Integer amountB = itemToAmountMap.get(stack2.getItem());
                    int cmp = Integer.compare(amountB, amountA);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return StackComparison.compareEqualItems(stack, stack2);
                });

                return sortIds;
            }
        });
        
        // Descending order of raw item ID
        RAW_ID = register("rawId", new SortOrder("rawId") {
            @Override
            public int[] sort(int[] sortIds, ItemStack[] stacks, SortContext context) {
                int[] rawIds = Arrays.stream(stacks).mapToInt(stack -> stack.isEmpty()
                        ? Integer.MAX_VALUE
                        : BuiltInRegistries.ITEM.getId(stack.getItem())).toArray();
                sortByValues(sortIds, rawIds, stacks);
                return sortIds;
            }
        });
    }
}
