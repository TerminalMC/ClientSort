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

package dev.terminalmc.clientsort.client.gui.screen.edit;

import dev.terminalmc.clientsort.client.gui.TriggerButtonManager;
import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.LinkedList;

/**
 * An implementation of {@link EditorScreen} for editing player inventory buttons.
 */
public class PlayerEditorScreen extends EditorScreen {

    public PlayerEditorScreen(AbstractContainerScreen<?> underlay, TriggerButton button) {
        super(underlay, true, button);
    }

    @Override
    protected LinkedList<TriggerButton> getButtons() {
        return TriggerButtonManager.getPlayerButtons();
    }
}
