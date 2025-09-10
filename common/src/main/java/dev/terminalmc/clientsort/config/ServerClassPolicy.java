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

import org.jetbrains.annotations.Nullable;

import java.text.ParseException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.terminalmc.clientsort.util.Localization.localized;

public class ServerClassPolicy {

    public static final String DATA_FORMAT = "%s,%d,%d,%d";
    public static final String DATA_PATTERN_STRING =
            "^(.+),([01]),([01]),([01])$";
    public static final Pattern DATA_PATTERN = Pattern.compile(DATA_PATTERN_STRING);

    public final String className;

    public boolean sortEnabled;
    public boolean stackFillEnabled;
    public boolean transferEnabled;

    public @Nullable String lastAutoEditTime;
    public @Nullable String lastAutoEditReason;

    public ServerClassPolicy(
            String className,
            boolean sortEnabled,
            boolean stackFillEnabled,
            boolean transferEnabled
    ) {
        this(className, sortEnabled, stackFillEnabled, transferEnabled, null, null);
    }

    public ServerClassPolicy(
            String className,
            boolean sortEnabled,
            boolean stackFillEnabled,
            boolean transferEnabled,
            @Nullable String lastAutoEditTime,
            @Nullable String lastEditReason
    ) {
        this.className = className;
        this.sortEnabled = sortEnabled;
        this.stackFillEnabled = stackFillEnabled;
        this.transferEnabled = transferEnabled;
        this.lastAutoEditTime = lastAutoEditTime;
        this.lastAutoEditReason = lastEditReason;
    }

    public void setFrom(ServerClassPolicy classPolicy) {
        this.sortEnabled = sortEnabled && classPolicy.sortEnabled;
        this.stackFillEnabled = stackFillEnabled && classPolicy.stackFillEnabled;
        this.transferEnabled = transferEnabled && classPolicy.transferEnabled;
    }

    public String toDataString() {
        return String.format(
                DATA_FORMAT,
                className,
                sortEnabled ? 1 : 0,
                stackFillEnabled ? 1 : 0,
                transferEnabled ? 1 : 0
        );
    }

    public static ServerClassPolicy fromDataString(
            String dataString,
            Set<String> originalClassNames
    ) throws ParseException {
        dataString = dataString.strip();

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
        if (!originalClassNames.contains(className)) {
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

        return new ServerClassPolicy(
                className,
                matcher.group(2).equals("1"),
                matcher.group(3).equals("1"),
                matcher.group(4).equals("1")
        );
    }
}
