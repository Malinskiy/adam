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

import com.android.ddmlib.interactor.PropertyFetcher;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.regex.Matcher;

/**
 * Shell output parser for a getprop command
 */
@VisibleForTesting
public class GetPropReceiver extends MultiLineReceiver {

    private final Map<String, String> mCollectedProperties =
            Maps.newHashMapWithExpectedSize(PropertyFetcher.EXPECTED_PROP_COUNT);

    @Override
    public void processNewLines(String[] lines) {
        // We receive an array of lines. We're expecting
        // to have the build info in the first line, and the build
        // date in the 2nd line. There seems to be an empty line
        // after all that.

        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            Matcher m = PropertyFetcher.GETPROP_PATTERN.matcher(line);
            if (m.matches()) {
                String label = m.group(1);
                String value = m.group(2);

                if (!label.isEmpty()) {
                    mCollectedProperties.put(label, value);
                }
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    public Map<String, String> getCollectedProperties() {
        return mCollectedProperties;
    }
}
