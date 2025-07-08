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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.terminalmc.clientsort.client.config.Config.options;
import static dev.terminalmc.clientsort.util.Localization.localized;

public record ButtonLayout(
        String className,
        @Nullable Vec2i offset,
        @Nullable Boolean sortEnabled,
        @Nullable Boolean stackFillEnabled,
        @Nullable Boolean transferEnabled
) {

    public static final String DATA_FORMAT = "%s,%s,%s,%s,%s";
    public static final String DATA_POS_FORMAT = "(%d,%d)";
    public static final String DATA_PATTERN_STRING =
            "^(.+),(?:-|\\((-?\\d+),(-?\\d+)\\)),(?:-|([01])),(?:-|([01])),(?:-|([01]))$";
    public static final Pattern DATA_PATTERN = Pattern.compile(DATA_PATTERN_STRING);

    public String toDataString() {
        return String.format(
                DATA_FORMAT,
                className,
                offset == null ? "-" : String.format(DATA_POS_FORMAT, offset.x(), offset.y()),
                sortEnabled == null ? "-" : sortEnabled ? 1 : 0,
                stackFillEnabled == null ? "-" : stackFillEnabled ? 1 : 0,
                transferEnabled == null ? "-" : transferEnabled ? 1 : 0
        );
    }

    public static ButtonLayout fromDataString(
            String dataString,
            Set<String> originalClassNames
    ) throws ParseException {
        dataString = dataString.strip();

        Matcher matcher = DATA_PATTERN.matcher(dataString);
        if (!matcher.matches()) {
            throw new ParseException(
                    localized(
                            "error",
                            "buttonLayout.pattern",
                            DATA_PATTERN_STRING
                    ).getString(), 0
            );
        }

        // Validate class name if modified
        String className = matcher.group(1);
        if (!originalClassNames.contains(className)) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new ParseException(
                        localized(
                                "error",
                                "buttonLayout.classNotFound",
                                className
                        ).getString(), 0
                );
            }
        }

        return new ButtonLayout(
                className,
                matcher.group(2) == null ? null
                        : new Vec2i(
                                Integer.parseInt(matcher.group(2)),
                                Integer.parseInt(matcher.group(3))
                        ),
                matcher.group(4) == null ? null
                        : matcher.group(4).equals("1"),
                matcher.group(5) == null ? null
                        : matcher.group(5).equals("1"),
                matcher.group(6) == null ? null
                        : matcher.group(6).equals("1")
        );
    }

    public @NotNull Vec2i offset() {
        return offset == null ? options().layoutOffset : offset;
    }

    public @NotNull Boolean sortEnabled() {
        return sortEnabled == null ? options().sortEnabled : sortEnabled;
    }

    public @NotNull Boolean stackFillEnabled() {
        return stackFillEnabled == null ? options().stackFillEnabled : stackFillEnabled;
    }

    public @NotNull Boolean transferEnabled() {
        return transferEnabled == null ? options().transferEnabled : transferEnabled;
    }
}
