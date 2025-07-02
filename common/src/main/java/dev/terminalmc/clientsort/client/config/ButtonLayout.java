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

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ButtonLayout {

    public static final String DATA_FORMAT = "%s,%s,%d,%d,%d";
    public static final String DATA_POS_FORMAT = "(%d,%d)";
    public static final String DATA_PATTERN_STRING =
            "^(.+),(?:null|\\((-?\\d+),(-?\\d+)\\)),([01]),([01]),([01])$";
    public static final Pattern DATA_PATTERN = Pattern.compile(DATA_PATTERN_STRING);

    public final String className;

    public @Nullable Vec2i offset;

    public boolean sortEnabled;
    public boolean stackFillEnabled;
    public boolean transferEnabled;

    public ButtonLayout(
            @Nullable String className,
            @Nullable Vec2i offset,
            boolean sortEnabled,
            boolean stackFillEnabled,
            boolean transferEnabled
    ) {
        this.className = className;
        this.offset = offset;
        this.sortEnabled = sortEnabled;
        this.stackFillEnabled = stackFillEnabled;
        this.transferEnabled = transferEnabled;
    }

    public String toDataString() {
        return String.format(
                DATA_FORMAT,
                className,
                offset == null ? "null" : String.format(DATA_POS_FORMAT, offset.x(), offset.y()),
                sortEnabled ? 1 : 0,
                stackFillEnabled ? 1 : 0,
                transferEnabled ? 1 : 0
        );
    }

    public static ButtonLayout fromDataString(String dataString) throws ParseException {
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

        String className = matcher.group(1);
        @Nullable Vec2i offset = null;
        boolean sortEnabled = matcher.group(4).equals("1");
        boolean stackFillEnabled = matcher.group(5).equals("1");
        boolean transferEnabled = matcher.group(6).equals("1");

        // Validate class name
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

        // Parse and validate offset
        try {
            if (matcher.group(2) != null) {
                offset = new Vec2i(
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3))
                );
            }
        } catch (NumberFormatException e) {
            throw new ParseException(
                    localized(
                            "error",
                            "buttonLayout.parseOffset",
                            matcher.group(2),
                            matcher.group(3)
                    ).getString(), 0
            );
        }

        return new ButtonLayout(className, offset, sortEnabled, stackFillEnabled, transferEnabled);
    }
}
