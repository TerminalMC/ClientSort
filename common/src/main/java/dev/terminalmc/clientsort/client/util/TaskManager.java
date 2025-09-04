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

package dev.terminalmc.clientsort.client.util;

import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private final List<Task> tasks = new ArrayList<>();

    public void schedule(int ticks, Runnable task) {
        this.tasks.add(new Task(ticks, task));
    }

    public void tick() {
        tasks.removeIf(Task::tick);
    }

    private static class Task {

        int ticks;
        final Runnable task;

        public Task(int ticks, Runnable task) {
            this.ticks = ticks;
            this.task = task;
        }

        public boolean tick() {
            if (ticks-- <= 0) {
                task.run();
                return true;
            }
            return false;
        }
    }
}
