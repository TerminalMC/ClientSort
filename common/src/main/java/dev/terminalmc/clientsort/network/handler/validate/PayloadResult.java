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

package dev.terminalmc.clientsort.network.handler.validate;

import static dev.terminalmc.clientsort.util.Localization.translationKey;

public enum PayloadResult {
    INCONSISTENT_STATE(4, translationKey("payloadResult", "failure.inconsistentState")),
    INVALID_DATA(3, translationKey("payloadResult", "failure.invalidData")),
    UNSUPPORTED_OP(2, translationKey("payloadResult", "failure.unsupportedOp")),
    FAILURE(1, translationKey("payloadResult", "failure")),
    SUCCESS(0, translationKey("payloadResult", "success")),
    UNKNOWN(-1, translationKey("payloadResult", "unknown"));

    public final int code;
    public final String translationKey;

    PayloadResult(int code, String translationKey) {
        this.code = code;
        this.translationKey = translationKey;
    }

    public boolean isSuccess() {
        return code == SUCCESS.code;
    }

    public boolean isUnknown() {
        return code == UNKNOWN.code;
    }

    public static PayloadResult get(int code) {
        for (PayloadResult err : values()) {
            if (code == err.code) {
                return err;
            }
        }
        return UNKNOWN;
    }
}
