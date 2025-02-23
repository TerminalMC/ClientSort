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

package dev.terminalmc.clientsort.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages rate-limited transmission of interaction events for client-side
 * manual inventory operations.
 */
public class InteractionManager {
    public static final Waiter TICK_WAITER = 
            (TriggerType triggerType) -> triggerType == TriggerType.TICK;
    
    private static final Queue<InteractionEvent> interactionEventQueue = new ArrayDeque<>();
    private static final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);

    private static ScheduledFuture<?> tickFuture;
    private static Waiter waiter = null;

    /**
     * Queues the specified event.
     */
    public static void push(InteractionEvent interactionEvent) {
        if (interactionEvent == null) {
            return;
        }
        synchronized (interactionEventQueue) {
            interactionEventQueue.add(interactionEvent);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Queues the specified events.
     */
    public static void pushAll(Collection<InteractionEvent> interactionEvents) {
        if (interactionEvents == null) {
            return;
        }
        synchronized (interactionEventQueue) {
            interactionEventQueue.addAll(interactionEvents);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Clears the event queue.
     */
    public static void clear() {
        synchronized (interactionEventQueue) {
            interactionEventQueue.clear();
            waiter = null;
        }
    }

    /**
     * Initiates sending of all queued events.
     */
    public static void triggerSend(TriggerType triggerType) {
        synchronized (interactionEventQueue) {
            if (waiter == null || waiter.trigger(triggerType)) {
                do {
                    InteractionEvent event = interactionEventQueue.poll();
                    if (event == null) {
                        waiter = null;
                        break;
                    }

                    doSendEvent(event);
                } while (waiter.trigger(TriggerType.INITIAL));
            }
        }
    }

    /**
     * Sends the specified event.
     */
    private static void doSendEvent(InteractionEvent event) {
        Waiter blockingWaiter = tt -> false;
        waiter = blockingWaiter;
        Minecraft.getInstance().execute(() -> {
            synchronized (interactionEventQueue) {
                if (waiter == blockingWaiter) {
                    waiter = event.send();
                }
            }
        });
    }

    /**
     * Sets the tick rate of the interaction manager.
     * @param milliSeconds the time, in milliseconds, between ticks.
     */
    public static void setTickRate(long milliSeconds) {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        tickFuture = scheduledExecutor.scheduleAtFixedRate(InteractionManager::tick,
                milliSeconds, milliSeconds, TimeUnit.MILLISECONDS);
    }
    
    private static void tick() {
        try {
            triggerSend(TriggerType.TICK);
        } catch (Exception e) {
            LogUtils.getLogger().error("Error while ticking InteractionManager", e);
        }
    }

    @FunctionalInterface
    public interface Waiter {
        boolean trigger(TriggerType triggerType);

        static Waiter equal(TriggerType triggerType) {
            return triggerType::equals;
        }
    }

    @Deprecated
    public static class GuiConfirmWaiter implements Waiter {
        int triggers;

        public GuiConfirmWaiter(int triggers) {
            this.triggers = triggers;
        }

        @Override
        public boolean trigger(TriggerType triggerType) {
            return triggerType == TriggerType.GUI_CONFIRM && --triggers == 0;
        }
    }

    public enum TriggerType {
        INITIAL, CONTAINER_SLOT_UPDATE, GUI_CONFIRM, HELD_ITEM_CHANGE, TICK
    }

    @FunctionalInterface
    public interface InteractionEvent {
        /**
         * Sends the interaction to the server
         *
         * @return the number of inventory packets to wait for
         */
        Waiter send();
    }

    @FunctionalInterface
    public interface ClickEventFactory {
        /**
         * Creates an interaction event based on a click.
         */
        InteractionEvent create(Slot slot, int action, ClickType clickType, boolean playSound);
    }

    public static class CallbackEvent implements InteractionEvent {
        private final Supplier<Waiter> callback;

        public CallbackEvent(Supplier<Waiter> callback) {
            this.callback = callback;
        }

        @Override
        public Waiter send() {
            return callback.get();
        }
    }
}
