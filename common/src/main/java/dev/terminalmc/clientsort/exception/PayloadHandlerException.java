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

package dev.terminalmc.clientsort.exception;

import dev.terminalmc.clientsort.network.handler.validate.PayloadResult;

/**
 * A custom exception for mod payload handlers.
 */
public abstract class PayloadHandlerException extends Exception {

    public static String GENERIC_MESSAGE = "Unexpected exception, check server logs for more info.";

    public final PayloadResult result;

    public PayloadHandlerException(String message, PayloadResult result) {
        super(message);
        this.result = result;
    }

    public PayloadHandlerException(String message) {
        this(message, PayloadResult.FAILURE);
    }

    public static class UnsupportedOpException extends PayloadHandlerException {

        public UnsupportedOpException(String message) {
            super(message, PayloadResult.UNSUPPORTED_OP);
        }
    }

    public static class InvalidDataException extends PayloadHandlerException {

        public InvalidDataException(String message) {
            super(message, PayloadResult.INVALID_DATA);
        }
    }

    public static class InconsistentStateException extends PayloadHandlerException {

        public InconsistentStateException(String message) {
            super(message, PayloadResult.INCONSISTENT_STATE);
        }
    }
}
