/*
 * Copyright 2021 Evan Steinkerchner (Roundaround)
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

import com.mojang.blaze3d.platform.InputConstants;
import dev.terminalmc.clientsort.client.ClientSort;
import dev.terminalmc.clientsort.client.config.*;
import dev.terminalmc.clientsort.client.gui.screen.config.ConfigScreenProvider;
import dev.terminalmc.clientsort.client.gui.widget.TriggerButton;
import dev.terminalmc.clientsort.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.terminalmc.clientsort.mixin.client.accessor.GuiGraphicsExtractorAccessor;
import dev.terminalmc.clientsort.mixin.client.accessor.GuiRenderStateAccessor;
import dev.terminalmc.clientsort.util.inject.ISlot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static dev.terminalmc.clientsort.ClientSort.debug;
import static dev.terminalmc.clientsort.ClientSort.getObj;
import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public abstract class EditorScreen extends Screen {

    private final Screen lastScreen;
    private final AbstractContainerScreen<?> underlay;
    private final boolean isPlayerInv;
    private final LinkedList<TriggerButton> buttons = new LinkedList<>();
    private boolean offsetFromSlot = false;
    private @Nullable Operation autoOp = null;
    private boolean autoOpOther = false;
    public final Set<Integer> ignoredSlots = new TreeSet<>();

    /**
     * An element of {@link EditorScreen#buttons} which 'represents' the whole set of buttons.
     * <p>
     * This can be any element, and the specific choice is only relevant when repositioning via
     * mouse drag.
     */
    private TriggerButton rep;

    /**
     * The class name of either {@link EditorScreen#rep}'s {@link TriggerButton#container}, or
     * {@link EditorScreen#underlay}'s {@link AbstractContainerScreen#getMenu} if the former is
     * {@code null}.
     * <p>
     * This value represents the lowest-level key on which a {@link ClassPolicy} can be created, and
     * may differ from {@link EditorScreen#rep}'s {@link TriggerButton#activePolicyKey}.
     */
    private String lowestPolicyClassName;

    /**
     * A flag to assist repositioning buttons via click-drag.
     */
    private boolean dragging;

    public EditorScreen(
            AbstractContainerScreen<?> underlay,
            boolean isPlayerInv,
            TriggerButton button
    ) {
        this(underlay, isPlayerInv, button, underlay);
    }

    public EditorScreen(
            AbstractContainerScreen<?> underlay,
            boolean isPlayerInv,
            TriggerButton button,
            Screen lastScreen
    ) {
        super(localized("title", "positionEditor"));
        this.lastScreen = lastScreen;
        this.underlay = underlay;
        this.isPlayerInv = isPlayerInv;
        this.rep = button;
        this.buttons.add(button);
    }

    /**
     * Re-initializes {@link EditorScreen#underlay}, then this screen's GUI elements.
     */
    @Override
    public void init() {
        super.init();

        // Resize the underlay
        underlay.init(width, height);

        // Reload buttons from the manager
        if (!reloadButtonsAndIgnoredSlots()) {
            // Failure
            clearWidgets();
            return;
        }

        // Populate the GUI
        rebuildGui();
    }

    /**
     * Reloads the lists of editable buttons and ignored slots.
     */
    private boolean reloadButtonsAndIgnoredSlots() {
        buttons.clear();
        ignoredSlots.clear();

        // Retrieve the buttons from the manager
        buttons.addAll(options().justifyButtonsTopLeft ? getButtons() : getButtons().reversed());

        if (buttons.size() != 4) {
            if (debug()) {
                ClientSort.LOG.error(
                        "Failed to reload buttons on PositionEditScreen: Button list is too small (expected: {}, actual: {})",
                        4,
                        buttons.size()
                );
            }
            return false;
        }

        // Pick an arbitrary representative
        rep = buttons.getFirst();

        // Activate buttons that are enabled in config but inactive due to GUI state, such as
        // transfer buttons when no second inventory is open
        ClassPolicy policy = options().classPolicies.get(rep.activePolicyKey);
        if (policy != null) {
            buttons.forEach((button) -> button.active = button.getPolicyStatus(policy));
            offsetFromSlot = policy.offsetFromSlot();
            autoOp = policy.autoOp();
            autoOpOther = policy.autoOpOther();
            ignoredSlots.addAll(policy.ignoredSlots());
        }

        // Retrieve the policy key in the same way as the manager
        Object keyObject = rep.container instanceof SimpleContainer
                ? underlay.getMenu()
                : rep.container;
        lowestPolicyClassName = keyObject.getClass().getName();

        return true;
    }

    /**
     * Retrieves the list of editable buttons from the source.
     */
    protected abstract LinkedList<TriggerButton> getButtons();

    /**
     * Clears and re-populates this screen's GUI.
     */
    private void rebuildGui() {
        clearWidgets();

        Minecraft mc = Minecraft.getInstance();

        int numButtons = 14;
        int x = 2;
        int movingY = height - 21 * numButtons;
        int width = 100;
        int height = 20;

        // Instructions tooltip button
        Button instructionsButton = Button.builder(
                        localized("editor", "instructions"), (button) -> {
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "instructions.tooltip.1")
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.2"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.3"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.4"))
                        .append("\n\n")
                        .append(localized("editor", "instructions.tooltip.5"))))
                .pos(x, movingY)
                .size(width, height)
                .build();
        instructionsButton.active = false;
        addRenderableWidget(instructionsButton);
        movingY += 21;

        Button copyPolicyKeyButton = Button.builder(
                        localized("editor", "copyPolicyKey"),
                        (button) -> mc.keyboardHandler.setClipboard(
                                rep.activePolicyKey == null
                                        ? "null"
                                        : rep.activePolicyKey
                        )
                )
                .pos(x, movingY)
                .size(width, height)
                .build();
        copyPolicyKeyButton.active = rep.activePolicyKey != null;
        addRenderableWidget(copyPolicyKeyButton);
        movingY += 21;

        // Split the current config off the parent class key
        Button splitPolicyClassButton = Button.builder(
                        localized("editor", "splitPolicyClass"),
                        (button) -> Minecraft.getInstance().gui.setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().classPolicies.put(
                                                ClassPolicy.getKey(lowestPolicyClassName, null),
                                                new ClassPolicy(
                                                        lowestPolicyClassName,
                                                        null,
                                                        buttons.getFirst().offset,
                                                        offsetFromSlot,
                                                        buttons.getFirst().operationAllowed
                                                                ? buttons.getFirst().active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(1).operationAllowed
                                                                ? buttons.get(1).active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(2).operationAllowed
                                                                ? buttons.get(2).active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        buttons.get(3).operationAllowed
                                                                ? buttons.get(3).active
                                                                ? Policy.KEYBIND_BUTTON
                                                                : Policy.KEYBIND
                                                                : Policy.NONE,
                                                        autoOp,
                                                        autoOpOther,
                                                        new TreeSet<>(ignoredSlots)
                                                )
                                        );
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().gui.setScreen(this);
                                },
                                localized("title", "confirm.splitPolicyClass"),
                                localized(
                                        "message",
                                        "confirm.splitPolicyClass",
                                        Component.literal(rep.activePolicyKey == null
                                                ? lowestPolicyClassName
                                                : rep.activePolicyKey
                                        ).withStyle(ChatFormatting.GOLD),
                                        Component.literal(lowestPolicyClassName)
                                                .withStyle(ChatFormatting.GOLD)
                                )
                        ))
                )
                .tooltip(Tooltip.create(localized("editor", "splitPolicyClass.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        splitPolicyClassButton.active = rep.activePolicyKey != null
                && !ClassPolicy.parseKey(rep.activePolicyKey)
                .getFirst()
                .equals(lowestPolicyClassName);
        addRenderableWidget(splitPolicyClassButton);
        movingY += 21;

        // Split the current config off the parent class key
        Button splitPolicyTitleButton = Button.builder(
                        localized("editor", "splitPolicyTitle"),
                        (button) -> {
                            Component invTitle = isPlayerInv
                                    ? ((AbstractContainerScreenAccessor) underlay).clientsort$getPlayerInventoryTitle()
                                    : underlay.getTitle();
                            Minecraft.getInstance().gui.setScreen(new ConfirmScreen(
                                    (confirm) -> {
                                        if (confirm) {
                                            if (ClassPolicy.hasInvTitle(rep.activePolicyKey)) {
                                                ClientSort.LOG.error(
                                                        "Cannot split policy with title: activePolicyKey '{}' already has title.",
                                                        rep.activePolicyKey
                                                );
                                                return;
                                            }

                                            options().classPolicies.put(
                                                    ClassPolicy.getKey(
                                                            rep.activePolicyKey,
                                                            invTitle.getString()
                                                    ),
                                                    new ClassPolicy(
                                                            rep.activePolicyKey,
                                                            invTitle.getString(),
                                                            buttons.getFirst().offset,
                                                            offsetFromSlot,
                                                            buttons.getFirst().operationAllowed
                                                                    ? buttons.getFirst().active
                                                                    ? Policy.KEYBIND_BUTTON
                                                                    : Policy.KEYBIND
                                                                    : Policy.NONE,
                                                            buttons.get(1).operationAllowed
                                                                    ? buttons.get(1).active
                                                                    ? Policy.KEYBIND_BUTTON
                                                                    : Policy.KEYBIND
                                                                    : Policy.NONE,
                                                            buttons.get(2).operationAllowed
                                                                    ? buttons.get(2).active
                                                                    ? Policy.KEYBIND_BUTTON
                                                                    : Policy.KEYBIND
                                                                    : Policy.NONE,
                                                            buttons.get(3).operationAllowed
                                                                    ? buttons.get(3).active
                                                                    ? Policy.KEYBIND_BUTTON
                                                                    : Policy.KEYBIND
                                                                    : Policy.NONE,
                                                            autoOp,
                                                            autoOpOther,
                                                            new TreeSet<>(ignoredSlots)
                                                    )
                                            );
                                            Config.save();
                                            init();
                                        }
                                        Minecraft.getInstance().gui.setScreen(this);
                                    },
                                    localized("title", "confirm.splitPolicyTitle"),
                                    localized(
                                            "message",
                                            "confirm.splitPolicyTitle",
                                            Component.literal(rep.activePolicyKey == null
                                                    ? lowestPolicyClassName
                                                    : rep.activePolicyKey
                                            ).withStyle(ChatFormatting.GOLD),
                                            Component.literal(invTitle.getString())
                                                    .withStyle(ChatFormatting.GOLD)
                                    )
                            ));
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "splitPolicyTitle.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        splitPolicyTitleButton.active = rep.activePolicyKey != null
                && !ClassPolicy.hasInvTitle(rep.activePolicyKey);
        addRenderableWidget(splitPolicyTitleButton);
        movingY += 21;

        // Switch between offset types
        CycleButton<@NotNull Boolean> switchOffsetTypeButton = CycleButton.booleanBuilder(
                        localized("editor", "switchOffsetType.slot"),
                        localized("editor", "switchOffsetType.edge"),
                        offsetFromSlot
                )
                .withTooltip((v) -> Tooltip.create(localized(
                        "editor",
                        "switchOffsetType.tooltip." + (v ? "slot" : "edge")
                )))
                .create(
                        x,
                        movingY,
                        width,
                        height,
                        localized("editor", "switchOffsetType"),
                        (button, v) -> {
                            this.offsetFromSlot = v;
                            this.buttons.forEach((b) -> b.offsetFromSlot = v);
                        }
                );
        addRenderableWidget(switchOffsetTypeButton);
        movingY += 21;

        // Move the button to the default position
        Button moveToDefaultButton = Button.builder(
                        localized("editor", "moveToDefault"),
                        (button) -> {
                            Vec2i before = buttons.getFirst().offset;
                            buttons.getFirst().offset = options().layoutOffset;
                            repositionButtons(buttons.getFirst(), before);
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "moveToDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(moveToDefaultButton);
        movingY += 21;

        // Save the current position as default
        Button saveAsDefaultButton = Button.builder(
                        localized("editor", "saveAsDefault"),
                        (button) -> Minecraft.getInstance().gui.setScreen(new ConfirmScreen(
                                (confirm) -> {
                                    if (confirm) {
                                        options().layoutOffset = buttons.getFirst().offset;
                                        Config.save();
                                        init();
                                    }
                                    Minecraft.getInstance().gui.setScreen(this);
                                },
                                localized("title", "confirm.saveAsDefault"),
                                localized("message", "confirm.saveAsDefault")
                        ))
                )
                .tooltip(Tooltip.create(localized("editor", "saveAsDefault.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(saveAsDefaultButton);
        movingY += 21;

        // Change the auto trigger behavior
        CycleButton<@NotNull Boolean> autoOpOtherButton = CycleButton.booleanBuilder(
                        Component.literal("1").withStyle(ChatFormatting.RED),
                        Component.literal("0").withStyle(ChatFormatting.GREEN),
                        autoOpOther
                )
                .withTooltip((v) -> Tooltip.create(localized("editor", "autoOp.other.tooltip")))
                .displayOnlyValue()
                .create(
                        x + width - 10,
                        movingY,
                        10,
                        height,
                        Component.empty(),
                        (b, v) -> autoOpOther = v
                );
        addRenderableWidget(autoOpOtherButton);
        CycleButton<@NotNull Integer> autoOpButton = CycleButton.builder(
                        (v) -> v == 0
                                ? localized("editor", "autoOp.none")
                                : localized("key", "op." + Operation.values()[v - 1].translationKey),
                        autoOp == null
                                ? 0
                                : List.of(Operation.values()).indexOf(autoOp) + 1
                )
                .withTooltip((v) -> Tooltip.create(localized("editor", "autoOp.tooltip")))
                .withValues(0, 1, 2, 3, 4)
                .create(
                        x,
                        movingY,
                        width - 10,
                        height,
                        localized("editor", "autoOp"),
                        (b, v) -> autoOp = (v == 0 ? null : Operation.values()[v - 1])
                );
        addRenderableWidget(autoOpButton);
        movingY += 21;

        // Toggle the visibility of all buttons
        Button toggleButtonsVisibleButton = Button.builder(
                        localized("editor", "toggleVisibility"),
                        (button) -> {
                            boolean status = buttons.stream().noneMatch((b) -> b.active);
                            buttons.forEach((b) -> b.active = status);
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "toggleVisibility.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(toggleButtonsVisibleButton);
        movingY += 21;

        // Ignore/unignore all slots
        Button toggleSlotsIgnoredButton = Button.builder(
                        localized("editor", "toggleIgnoreSlots"),
                        (button) -> {
                            if (ignoredSlots.isEmpty()) {
                                for (Slot slot : underlay.getMenu().slots) {
                                    Object object = getObj(slot, underlay.getMenu());
                                    if (object != null && object.getClass()
                                            .getName()
                                            .equals(lowestPolicyClassName)) {
                                        ignoredSlots.add(((ISlot) slot).clientsort$getIndexInContainer());
                                    }
                                }
                            } else {
                                ignoredSlots.clear();
                            }
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "toggleIgnoreSlots.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(toggleSlotsIgnoredButton);
        movingY += 21;

        // Re-generates the screen to undo all changes made since opening
        Button undoChangesButton = Button.builder(
                        localized("editor", "undoChanges"),
                        (button) -> init()
                )
                .tooltip(Tooltip.create(localized("editor", "undoChanges.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(undoChangesButton);
        movingY += 21;

        // Open group selector screen
        Button reselectButton = Button.builder(
                        localized("editor", "reselect"),
                        (button) -> {
                            onClose();
                            Minecraft.getInstance().gui.setScreen(
                                    new SelectorScreen(underlay, this)
                            );
                        }
                )
                .tooltip(Tooltip.create(localized("editor", "reselect.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(reselectButton);
        movingY += 21;

        // Open the main config screen
        Button configButton = Button.builder(
                        localized("editor", "openConfig"),
                        (button) -> Minecraft.getInstance().gui.setScreen(
                                ConfigScreenProvider.getConfigScreen(this)
                        )
                )
                .tooltip(Tooltip.create(localized("editor", "openConfig.tooltip")))
                .pos(x, movingY)
                .size(width, height)
                .build();
        addRenderableWidget(configButton);
        movingY += 21;

        // Close this screen without saving
        Button cancelButton = Button.builder(CommonComponents.GUI_CANCEL, (button) -> onClose())
                .pos(x, movingY)
                .size(width / 2, height)
                .build();
        addRenderableWidget(cancelButton);
        // Save all changes then close this screen
        Button doneButton = Button.builder(CommonComponents.GUI_DONE, (button) -> saveAndClose())
                .pos(x + cancelButton.getWidth(), movingY)
                .size(width - cancelButton.getWidth(), height)
                .build();
        addRenderableWidget(doneButton);
    }

    /**
     * Renders the underlay, then this screen with its GUI.
     */
    @Override
    public void extractRenderState(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        underlay.extractBackground(graphics, mouseX, mouseY, partialTick);
        underlay.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Workaround for other mods adding blur when rendering the underlay
        ((GuiRenderStateAccessor) ((GuiGraphicsExtractorAccessor) graphics).clientsort$getGuiRenderState())
                .clientsort$setFirstStratumAfterBlur(Integer.MAX_VALUE);
        graphics.nextStratum();
        extractBlurredBackground(graphics);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 2, 0xFFFFFFFF);

        // Render disabled-slot indicators
        for (Slot slot : underlay.getMenu().slots) {
            Object object = getObj(slot, underlay.getMenu());
            if (object != null && object.getClass().getName().equals(lowestPolicyClassName)) {
                if (ignoredSlots.contains(((ISlot) slot).clientsort$getIndexInContainer())) {
                    // Draw lock icon, top left
                    //noinspection UnnecessaryUnicodeEscape
                    graphics.text(
                            Minecraft.getInstance().font,
                            "\u274C",
                            ((AbstractContainerScreenAccessor) (underlay)).clientsort$getLeftPos()
                                    + slot.x,
                            ((AbstractContainerScreenAccessor) (underlay)).clientsort$getTopPos()
                                    + slot.y,
                            0xFFFF0000
                    );
                }
            }
        }

        // Safety net
        if (buttons.isEmpty())
            return;

        // Render trace lines
        drawLineFor(graphics, buttons.getFirst());

        // Render info lines
        Vec2i offset = buttons.getFirst().offset;
        graphics.text(
                font,
                localized("editor", "offset", offset.x(), offset.y()).getString(),
                105,
                height - (font.lineHeight + 1) * 4,
                0xFFFFFFFF
        );
        graphics.text(
                font,
                localized(
                        "editor",
                        "policyKey.current",
                        rep.activePolicyKey
                                == null
                                ? localized("editor", "policyKey.unset")
                                : rep.activePolicyKey
                ),
                105,
                height - (font.lineHeight + 1) * 3,
                0xFFFFFFFF
        );
        graphics.text(
                font,
                localized("editor", "policyKey.menu", lowestPolicyClassName),
                105,
                height - (font.lineHeight + 1) * 2,
                0xFFFFFFFF
        );
        graphics.text(
                font,
                localized("editor", "policyKey.screen", underlay.getClass().getName()),
                105,
                height - (font.lineHeight + 1),
                0xFFFFFFFF
        );

        // Render editable widgets again, above background blur
        for (TriggerButton cb : buttons) {
            cb.extractContents(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Removes the call to {@link Screen#extractBlurredBackground}, since we add a call in
     * {@link EditorScreen#extractRenderState} and the method can only be called once.
     */
    @Override
    public void extractBackground(
            @NotNull GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        if (Minecraft.getInstance().level == null) {
            extractPanorama(graphics, partialTick);
        }
        extractMenuBackground(graphics);
    }

    /**
     * Modifies the background blur to be constant irrespective of the configured value.
     * <p>
     * Minimal blur is used to prevent the editable widgets disappearing under underlay items on a
     * higher render layer, while still keeping the underlay detail discernible.
     */
    @Override
    protected void extractBlurredBackground(@NotNull GuiGraphicsExtractor graphics) {
        int original = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(1);
        super.extractBlurredBackground(graphics);
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(original);
    }

    /**
     * Draws a horizontal and a vertical line to trace this widget back to its positional origin
     * point.
     */
    private void drawLineFor(GuiGraphicsExtractor graphics, TriggerButton button) {
        graphics.horizontalLine(
                button.getX() - button.offset.x(),
                button.getX(),
                button.getY(),
                0xFFBBBBBB
        );
        graphics.verticalLine(
                button.getX() - button.offset.x(),
                button.getY() - button.offset.y(),
                button.getY(),
                0xFFBBBBBB
        );
    }

    /**
     * Closes this screen and shows {@link EditorScreen#lastScreen} instead.
     */
    @Override
    public void onClose() {
        super.onClose();
        lastScreen.init(width, height);
        Minecraft.getInstance().gui.setScreen(lastScreen);
    }

    /**
     * Saves any altered values, then calls {@link EditorScreen#onClose}.
     */
    public void saveAndClose() {
        @Nullable Vec2i offset = buttons.getFirst().offset.equals(options().layoutOffset)
                ? null
                : buttons.getFirst().offset;
        buttons.forEach((b) -> b.savePolicy(
                offset,
                offsetFromSlot,
                autoOp,
                autoOpOther,
                ignoredSlots
        ));
        Config.save();
        onClose();
    }

    /**
     * Allows pressing the arrow keys to reposition the set of buttons.
     */
    @Override
    public boolean keyPressed(KeyEvent event) {
        int distance = event.hasShiftDown() ? 6 : 1;
        @Nullable Vec2i movement = switch (event.key()) {
            case InputConstants.KEY_LEFT -> new Vec2i(-distance, 0);
            case InputConstants.KEY_RIGHT -> new Vec2i(distance, 0);
            case InputConstants.KEY_UP -> new Vec2i(0, -distance);
            case InputConstants.KEY_DOWN -> new Vec2i(0, distance);
            default -> null;
        };
        if (movement != null) {
            Vec2i before = rep.offset;
            // Move the rep button first
            rep.offset = rep.offset.add(movement);
            // Then move the others to match
            repositionButtons(rep, before);
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            dragging = false;
            return true;
        } else {
            for (TriggerButton cb : buttons) {
                if (cb.isMouseOver(event.x(), event.y())) {
                    cb.mouseClicked(event, doubleClick);
                    rep = cb;
                    dragging = true;
                    return true;
                }
            }
            for (Slot slot : underlay.getMenu().slots) {
                if (((AbstractContainerScreenAccessor) underlay)
                        .clientsort$isHovering(slot, event.x(), event.y())) {
                    Object object = getObj(slot, underlay.getMenu());
                    if (object != null && object.getClass().getName().equals(
                            lowestPolicyClassName)) {
                        int slotId = ((ISlot) slot).clientsort$getIndexInContainer();
                        if (ignoredSlots.contains(slotId))
                            ignoredSlots.remove(slotId);
                        else
                            ignoredSlots.add(slotId);
                    }
                }
            }

            return false;
        }
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            Vec2i before = rep.offset;
            if (rep.mouseDragged(event, dragX, dragY)) {
                // Move the other buttons to match the rep's movement
                repositionButtons(rep, before);
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    /**
     * Allows dragging the selected widget to reposition it.
     */
    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    /**
     * If {@code before} does not match the current buttonOffset of {@code button}, moves all other
     * widgets to match how {@code button} was moved.
     */
    private void repositionButtons(TriggerButton button, Vec2i before) {
        if (!button.offset.equals(before)) {
            Vec2i diff = button.offset.subtract(before);
            for (TriggerButton cb : buttons) {
                if (cb != button) {
                    cb.offset = cb.offset.add(diff);
                }
            }
        }
    }
}
