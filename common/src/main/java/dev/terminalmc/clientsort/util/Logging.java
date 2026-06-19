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

package dev.terminalmc.clientsort.util;

import dev.terminalmc.clientsort.platform.services.PlatformServices;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.AbstractMessageFactory;
import org.apache.logging.log4j.message.FormattedMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;

@SuppressWarnings("unused")
public class Logging {

    private Logging() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static Logger getLogger(String name) {
        if (PlatformServices.getInstance().isDevEnv()
                || PlatformServices.getInstance().hasNamedLogger()) {
            return LogManager.getLogger(name);
        } else {
            return LogManager.getLogger(name, new PrefixingMessageFactory("[" + name + "/]: "));
        }
    }

    private static final class PrefixingMessageFactory extends AbstractMessageFactory {

        private final String prefix;

        public PrefixingMessageFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Message newMessage(String message) {
            return new SimpleMessage(prefix + message);
        }

        @Override
        public Message newMessage(String message, Object... params) {
            return new FormattedMessage(prefix + message, params);
        }
    }
}
