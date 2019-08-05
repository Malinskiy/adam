/*
 * Copyright (C) 2019 Anton Malinskiy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ddmlib.receiver;

import com.android.ddmlib.receiver.base.MultiLineReceiver;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link MultiLineReceiver} that can receive a
 * Success message from ADB followed by a session ID.
 */
public class MultiInstallReceiver extends MultiLineReceiver {

    private static final Pattern successPattern = Pattern.compile("Success: .*\\[(\\d*)\\]");

    @Nullable String sessionId = null;

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            Matcher matcher = successPattern.matcher(line);
            if (matcher.matches()) {
                sessionId = matcher.group(1);
            }
        }

    }

    @Nullable
    public String getSessionId() {
        return sessionId;
    }
}
