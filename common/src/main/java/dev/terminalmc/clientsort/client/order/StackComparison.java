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

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.awt.*;
import java.util.Iterator;

/**
 * Additional comparison methods for sorting {@link ItemStack} instances that could not be otherwise
 * differentiated.
 * <p>
 * The comparators below are checked in sequence until a difference is found:
 * <ol>
 *     <li>Size
 *     <li>Custom hover name
 *     <li>Tooltip
 *     <li>Dye color
 *     <li>Damage
 * </ol>
 */
public class StackComparison {

    public static int compareEqualItems(ItemStack a, ItemStack b, SortContext context) {
        // Compare counts
        int cmp = Integer.compare(b.getCount(), a.getCount());
        if (cmp != 0) {
            return cmp;
        }
        return compareEqualItems2(a, b, context);
    }

    private static int compareEqualItems2(ItemStack a, ItemStack b, SortContext context) {
        // Compare names
        if (a.hasCustomHoverName()) {
            if (!b.hasCustomHoverName()) {
                return -1;
            }
            return compareEqualItems3(a, b, context);
        }
        if (b.hasCustomHoverName()) {
            return 1;
        }
        return compareEqualItems3(a, b, context);
    }

    private static int compareEqualItems3(ItemStack a, ItemStack b, SortContext context) {
        // Compare tooltips
        Iterator<Component> tooltipsA = a.getTooltipLines(
                null,
                TooltipFlag.Default.NORMAL
        ).iterator();
        Iterator<Component> tooltipsB = b.getTooltipLines(
                null,
                TooltipFlag.Default.NORMAL
        ).iterator();

        while (tooltipsA.hasNext()) {
            if (!tooltipsB.hasNext()) {
                return 1;
            }

            int cmp =
                    tooltipsA.next().getString().compareToIgnoreCase(tooltipsB.next().getString());
            if (cmp != 0) {
                return cmp;
            }
        }
        if (tooltipsB.hasNext()) {
            return -1;
        }
        return compareEqualItems4(a, b, context);
    }

    private static int compareEqualItems4(ItemStack a, ItemStack b, SortContext context) {
        // Compare color
        Item item = a.getItem();
        if (item instanceof DyeableLeatherItem) {
            int colorA = ((DyeableLeatherItem) item).getColor(a);
            int colorB = ((DyeableLeatherItem) item).getColor(b);
            float[] hsbA =
                    Color.RGBtoHSB(colorA >> 16 & 0xFF, colorA >> 8 & 0xFF, colorA & 0xFF, null);
            float[] hsbB =
                    Color.RGBtoHSB(colorB >> 16 & 0xFF, colorB >> 8 & 0xFF, colorB & 0xFF, null);
            int cmp = Float.compare(hsbA[0], hsbB[0]);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Float.compare(hsbA[1], hsbB[1]);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Float.compare(hsbA[2], hsbB[2]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return compareEqualItems5(a, b, context);
    }

    @SuppressWarnings("unused")
    private static int compareEqualItems5(ItemStack a, ItemStack b, SortContext context) {
        // Compare damage
        return Integer.compare(a.getDamageValue(), b.getDamageValue());
    }
}
