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

package dev.terminalmc.clientsort.client;

import dev.terminalmc.clientsort.client.config.Config;
import dev.terminalmc.clientsort.client.interaction.InteractionManager;
import dev.terminalmc.clientsort.client.order.SortOrder;
import dev.terminalmc.clientsort.client.util.KeybindManager;
import dev.terminalmc.clientsort.client.util.PolicyManager;
import dev.terminalmc.clientsort.client.util.TaskManager;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.util.ModLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static dev.terminalmc.clientsort.client.config.Config.options;

public class ClientSort {

    public static final String MOD_ID = dev.terminalmc.clientsort.ClientSort.MOD_ID;
    public static final String MOD_NAME = dev.terminalmc.clientsort.ClientSort.MOD_NAME;
    public static final ModLogger LOG = dev.terminalmc.clientsort.ClientSort.LOG;

    public static final TaskManager taskManager = new TaskManager();

    public static @Nullable MultiLineTextWidget overlayMessage = null;
    private static Runnable clearOverlayMessage = null;

    public static boolean searchOrderUpdated;

    public static volatile boolean emiReloading = false;
    public static volatile boolean updateBlockedByEmi = false;

    public static volatile boolean operatingClient = false;
    public static BlockingQueue<Runnable> clientOpQueue = new ArrayBlockingQueue<>(2);

    public static boolean debug() {
        return dev.terminalmc.clientsort.ClientSort.debug();
    }

    public static void init() {
        Config.getAndSave();
    }

    public static void afterClientTick(Minecraft mc) {
        taskManager.tick();
    }

    public static void afterConfigSaved(Config config) {
        @Nullable Minecraft mc = Minecraft.getInstance();
        Config.Options options = config.options;
        // Convert config sort order strings into enum values
        options.sortOrder = SortOrder.SORT_ORDERS.get(options.sortOrderStr);
        options.shiftSortOrder = SortOrder.SORT_ORDERS.get(options.shiftSortOrderStr);
        options.ctrlSortOrder = SortOrder.SORT_ORDERS.get(options.ctrlSortOrderStr);
        options.altSortOrder = SortOrder.SORT_ORDERS.get(options.altSortOrderStr);
        // Parse sound location string
        options.sortSoundLoc = ResourceLocation.tryParse(options.interactionSound);
        // Update interaction manager tick rate
        InteractionManager.setTickRate(options.interactionInterval);
        // Update class cache
        PolicyManager.reloadPolicyClasses(options.classPolicies.keySet());
        //noinspection ConstantValue
        if (mc != null && mc.getConnection() != null && mc.getConnection().isAcceptingMessages()) {
            // Update item tags
            updateItemTags(options);
            // Update sorting item sets
            updateItemSets(options);
        }
        // Isolate keybinds
        if (options().isolateKeybinds)
            KeybindManager.isolateKeybinds();
    }

    public static void updateItemTags(Config.Options options) {
        options.typeMatchItemCache.clear();
        BuiltInRegistries.ITEM.getTags().forEach((pair) -> {
            if (options.typeMatchTags.contains(pair.getFirst().location().getPath())) {
                pair.getSecond().forEach((itemHolder) ->
                        options.typeMatchItemCache.add(itemHolder.value()));
            }
        });
    }

    public static void updateItemSets(Config.Options options) {
        options.startOverrideMap.clear();
        int i = 0;
        for (String s : options.startOverrideItems) {
            int idx = i++;
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(s))
                    .ifPresent((item) -> options.startOverrideMap.put(item, idx));
        }
        options.endOverrideMap.clear();
        i = 0;
        for (String s : options.endOverrideItems) {
            int idx = i++;
            BuiltInRegistries.ITEM.getOptional(ResourceLocation.tryParse(s))
                    .ifPresent((item) -> options.endOverrideMap.put(item, idx));
        }
    }

    public static void afterGameStart() {
        if (options().isolateKeybinds)
            KeybindManager.isolateKeybinds();
    }

    public static void setOverlayMessage(
            AbstractContainerScreen<?> screen,
            Component message,
            int clearAfterTicks
    ) {
        if (overlayMessage != null) {
            clearOverlayMessage.run();
        }

        Minecraft mc = Minecraft.getInstance();
        MultiLineTextWidget newMessage = new MultiLineTextWidget(message, mc.font);
        newMessage.setMaxWidth(((AbstractContainerScreenAccessor) screen).clientsort$getImageWidth());
        newMessage.setCentered(true);
        newMessage.setX(screen.width / 2 - newMessage.getWidth() / 2);
        newMessage.setY(screen.height / 2 - newMessage.getHeight() / 2);

        overlayMessage = newMessage;
        clearOverlayMessage = () -> {
            if (overlayMessage == newMessage) {
                overlayMessage = null;
                clearOverlayMessage = null;
            }
        };
        taskManager.schedule(clearAfterTicks, clearOverlayMessage);
    }
}
