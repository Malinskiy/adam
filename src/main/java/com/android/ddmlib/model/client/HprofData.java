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

package com.android.ddmlib.model.client;

import com.android.ddmlib.ClientData;
import org.jetbrains.annotations.NotNull;

public class HprofData {
    public enum Type {
        FILE,
        DATA
    }

    public final Type type;
    public final String filename;
    public final byte[] data;

    public HprofData(@NotNull String filename) {
        type = Type.FILE;
        this.filename = filename;
        this.data = null;
    }

    public HprofData(@NotNull byte[] data) {
        type = Type.DATA;
        this.data = data;
        this.filename = null;
    }
}
