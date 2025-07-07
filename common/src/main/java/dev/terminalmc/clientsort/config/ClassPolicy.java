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

package dev.terminalmc.clientsort.config;

public class ClassPolicy {

    public final String className;

    public boolean sort;
    public boolean stackFill;
    public boolean transfer;

    public ClassPolicy(String className, boolean sort, boolean stackFill, boolean transfer) {
        this.className = className;
        this.sort = sort;
        this.stackFill = stackFill;
        this.transfer = transfer;
    }

    public void setFrom(ClassPolicy classPolicy) {
        this.sort = sort && classPolicy.sort;
        this.stackFill = stackFill && classPolicy.stackFill;
        this.transfer = transfer && classPolicy.transfer;
    }
}
