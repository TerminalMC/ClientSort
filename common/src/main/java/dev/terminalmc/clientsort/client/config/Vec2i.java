/*
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

package dev.terminalmc.clientsort.client.config;

public record Vec2i(int x, int y) {

    public Vec2i add(Vec2i other) {
        return new Vec2i(this.x + other.x, this.y + other.y);
    }

    public Vec2i subtract(Vec2i other) {
        return new Vec2i(this.x - other.x, this.y - other.y);
    }

    public boolean equals(Vec2i other) {
        return this.x == other.x && this.y == other.y;
    }
}
