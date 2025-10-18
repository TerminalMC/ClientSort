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

package dev.terminalmc.clientsort.client.config;

import joptsimple.internal.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public record ClassPolicy(
        @NotNull String className,
        @Nullable Vec2i buttonOffset,
        @NotNull Policy sortPolicy,
        @NotNull Policy stackFillPolicy,
        @NotNull Policy matchTransferPolicy,
        @NotNull Policy transferPolicy,
        @Nullable Operation autoOp,
        boolean autoOpOther,
        @NotNull TreeSet<Integer> ignoredSlots
) {

    public @NotNull Vec2i getButtonOffset() {
        return buttonOffset == null
                ? options().layoutOffset
                : buttonOffset;
    }

    public boolean canSort() {
        return !sortPolicy.equals(Policy.NONE);
    }

    public boolean canStackFill() {
        return !stackFillPolicy.equals(Policy.NONE);
    }

    public boolean canMatchTransfer() {
        return !matchTransferPolicy.equals(Policy.NONE);
    }

    public boolean canTransfer() {
        return !transferPolicy.equals(Policy.NONE);
    }

    public boolean useSortKeybind() {
        return sortPolicy.keybind;
    }

    public boolean useStackFillKeybind() {
        return stackFillPolicy.keybind;
    }

    public boolean useMatchTransferKeybind() {
        return matchTransferPolicy.keybind;
    }

    public boolean useTransferKeybind() {
        return transferPolicy.keybind;
    }

    public boolean showSortButton() {
        return sortPolicy.button;
    }

    public boolean showStackFillButton() {
        return stackFillPolicy.button;
    }

    public boolean showMatchTransferButton() {
        return matchTransferPolicy.button;
    }

    public boolean showTransferButton() {
        return transferPolicy.button;
    }

    public boolean autoSort() {
        return autoOp == Operation.SORT;
    }

    public boolean autoStackFill() {
        return autoOp == Operation.STACK_FILL;
    }

    public boolean autoMatchTransfer() {
        return autoOp == Operation.MATCH_TRANSFER;
    }

    public boolean autoTransfer() {
        return autoOp == Operation.TRANSFER;
    }

    // Config data-string serialization

    public static final String DATA_FORMAT = "%s,(%s),%s,%s,%s,%s,%d/%d,(%s)";
    public static final String DATA_PATTERN_STRING =
            "^(.+),\\((?:(-?\\d+),(-?\\d+))?\\),([012]),([012]),([012]),([012]),([01234])/([01]),\\(((?:\\d+(?:,\\d+)*)?)\\)$";
    public static final Pattern DATA_PATTERN = Pattern.compile(DATA_PATTERN_STRING);

    public String toDataString() {
        return String.format(
                DATA_FORMAT,
                className,
                buttonOffset == null ? "" : buttonOffset.x() + "," + buttonOffset.y(),
                sortPolicy.toSimpleString(),
                stackFillPolicy.toSimpleString(),
                matchTransferPolicy.toSimpleString(),
                transferPolicy.toSimpleString(),
                autoOp == null ? 0 : List.of(Operation.values()).indexOf(autoOp) + 1,
                autoOpOther ? 1 : 0,
                Strings.join(ignoredSlots.stream().map(String::valueOf).toList(), ",")
        );
    }

    public static ClassPolicy fromDataString(
            String dataString,
            Set<String> oldClassNames
    ) throws ParseException {
        dataString = dataString.strip();

        // Validate data-string format
        Matcher matcher = DATA_PATTERN.matcher(dataString);
        if (!matcher.matches()) {
            throw new ParseException(
                    localized(
                            "error",
                            "classPolicy.pattern",
                            DATA_PATTERN_STRING
                    ).getString(), 0
            );
        }

        // Validate class name if modified
        String className = matcher.group(1);
        if (!oldClassNames.contains(className)) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new ParseException(
                        localized(
                                "error",
                                "classPolicy.classNotFound",
                                className
                        ).getString(), 0
                );
            }
        }

        return new ClassPolicy(
                className,
                matcher.group(2) == null ? null
                        : new Vec2i(
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3))
                        ),
                Policy.fromSimpleString(matcher.group(4)),
                Policy.fromSimpleString(matcher.group(5)),
                Policy.fromSimpleString(matcher.group(6)),
                Policy.fromSimpleString(matcher.group(7)),
                matcher.group(8).equals("0")
                        ? null
                        : Operation.values()[Integer.parseInt(matcher.group(8)) - 1],
                matcher.group(9).equals("1"),
                new TreeSet<>(Arrays.stream(matcher.group(10).split(","))
                        .filter((s) -> !s.isBlank())
                        .map(Integer::parseInt).sorted().toList())
        );
    }
}
