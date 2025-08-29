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

package dev.terminalmc.clientsort.client.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Manages rate-limited transmission of interaction events for client-side inventory manipulation
 * operations.
 */
public class InteractionManager {

    public static final Waiter TICK_WAITER = (TriggerType type) -> type == TriggerType.TICK;

    private static final ArrayDeque<InteractionEvent> eventQueue = new ArrayDeque<>();
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

    private static ScheduledFuture<?> tickFuture;
    private static Waiter waiter = null;

    /**
     * Queues the event.
     */
    public static void push(InteractionEvent event) {
        synchronized (eventQueue) {
            eventQueue.add(event);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Adds the event to the front of the queue.
     */
    public static void now(InteractionEvent event) {
        synchronized (eventQueue) {
            eventQueue.addFirst(event);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Queues the events.
     */
    public static void pushAll(Collection<InteractionEvent> events) {
        synchronized (eventQueue) {
            eventQueue.addAll(events);
            if (waiter == null)
                triggerSend(TriggerType.INITIAL);
        }
    }

    /**
     * Clears the event queue.
     */
    public static void clear() {
        synchronized (eventQueue) {
            eventQueue.clear();
            waiter = null;
        }
    }

    /**
     * Initiates sending of all queued events.
     */
    public static void triggerSend(TriggerType type) {
        synchronized (eventQueue) {
            if (waiter == null || waiter.trigger(type)) {
                do {
                    InteractionEvent event = eventQueue.poll();
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
            synchronized (eventQueue) {
                if (waiter == blockingWaiter) {
                    waiter = event.send();
                }
            }
        });
    }

    /**
     * Sets the tick rate of the interaction manager.
     *
     * @param intervalMs the time, in milliseconds, between ticks.
     */
    public static void setTickRate(long intervalMs) {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        tickFuture = executor.scheduleAtFixedRate(
                InteractionManager::tick,
                intervalMs,
                intervalMs,
                TimeUnit.MILLISECONDS
        );
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

        boolean trigger(TriggerType type);

        @SuppressWarnings("unused")
        static Waiter equal(TriggerType type) {
            return type::equals;
        }
    }

    @Deprecated
    public static class GuiConfirmWaiter implements Waiter {

        int triggers;

        public GuiConfirmWaiter(int triggers) {
            this.triggers = triggers;
        }

        @Override
        public boolean trigger(TriggerType type) {
            return type == TriggerType.GUI_CONFIRM && --triggers == 0;
        }
    }

    public enum TriggerType {
        INITIAL,
        CONTAINER_SLOT_UPDATE,
        GUI_CONFIRM,
        HELD_ITEM_CHANGE,
        TICK
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
        InteractionEvent create(Slot slot, int button, ClickType clickType, boolean playSound);
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
