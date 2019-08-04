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

package com.android.ddmlib.emulator;

/** Gsm Mode enum. */
public enum GsmMode {
    UNKNOWN((String)null),
    UNREGISTERED(new String[] { "unregistered", "off" }),
    HOME(new String[] { "home", "on" }),
    ROAMING("roaming"),
    SEARCHING("searching"),
    DENIED("denied");

    private final String[] tags;

    GsmMode(String tag) {
        if (tag != null) {
            this.tags = new String[] { tag };
        } else {
            this.tags = new String[0];
        }
    }

    GsmMode(String[] tags) {
        this.tags = tags;
    }

    public static GsmMode getEnum(String tag) {
        for (GsmMode mode : values()) {
            for (String t : mode.tags) {
                if (t.equals(tag)) {
                    return mode;
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * Returns the first tag of the enum.
     */
    public String getTag() {
        if (tags.length > 0) {
            return tags[0];
        }
        return null;
    }
}
