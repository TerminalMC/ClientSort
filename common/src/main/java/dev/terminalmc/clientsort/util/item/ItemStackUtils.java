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

package dev.terminalmc.clientsort.util.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.DyedItemColor;

import java.awt.*;
import java.util.Iterator;

public class ItemStackUtils {
    public static int compareEqualItems(ItemStack a, ItemStack b) {
        // compare counts
        int cmp = Integer.compare(b.getCount(), a.getCount());
        if (cmp != 0) {
            return cmp;
        }
        return compareEqualItems2(a, b);
    }

    private static int compareEqualItems2(ItemStack a, ItemStack b) {
        // compare names
        if (hasCustomHoverName(a)) {
            if (!hasCustomHoverName(b)) {
                return -1;
            }
            return compareEqualItems3(a, b);
        }
        if (hasCustomHoverName(b)) {
            return 1;
        }
        return compareEqualItems3(a, b);
    }

    private static boolean hasCustomHoverName(ItemStack itemStack) {
        return itemStack.get(DataComponents.CUSTOM_NAME) != null;
    }

    private static int compareEqualItems3(ItemStack a, ItemStack b) {
        // compare tooltips
        Iterator<Component> tooltipsA = a.getTooltipLines(
                Item.TooltipContext.of(Minecraft.getInstance().level),
                null, TooltipFlag.Default.NORMAL).iterator();
        Iterator<Component> tooltipsB = b.getTooltipLines(
                Item.TooltipContext.of(Minecraft.getInstance().level),
                null, TooltipFlag.Default.NORMAL).iterator();

        while (tooltipsA.hasNext()) {
            if (!tooltipsB.hasNext()) {
                return 1;
            }

            int cmp = tooltipsA.next().getString().compareToIgnoreCase(tooltipsB.next().getString());
            if (cmp != 0) {
                return cmp;
            }
        }
        if (tooltipsB.hasNext()) {
            return -1;
        }
        return compareEqualItems4(a, b);
    }

    private static int compareEqualItems4(ItemStack a, ItemStack b) {
        // compare special item properties
        Item item = a.getItem();
        if ((item.getDefaultInstance()).is(ItemTags.DYEABLE)) {
            int colorA = DyedItemColor.getOrDefault(a, -6265536);
            int colorB = DyedItemColor.getOrDefault(b, -6265536);
            float[] hsbA = Color.RGBtoHSB(colorA >> 16 & 0xFF, colorA >> 8 & 0xFF, colorA & 0xFF, null);
            float[] hsbB = Color.RGBtoHSB(colorB >> 16 & 0xFF, colorB >> 8 & 0xFF, colorB & 0xFF, null);
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
        return compareEqualItems5(a, b);
    }

    private static int compareEqualItems5(ItemStack a, ItemStack b) {
        // compare damage
        return Integer.compare(a.getDamageValue(), b.getDamageValue());
    }
}
