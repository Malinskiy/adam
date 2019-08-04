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

package com.android.ddmlib.debug;

import com.android.ddmlib.Client;
import com.android.ddmlib.preferences.DdmPreferences;

/**
 * Centralized point to provide a {@link IDebugPortProvider} to ddmlib.
 *
 * <p/>When {@link Client} objects are created, they start listening for debuggers on a specific
 * port. The default behavior is to start with {@link DdmPreferences#getDebugPortBase()} and
 * increment this value for each new <code>Client</code>.
 *
 * <p/>This {@link DebugPortManager} allows applications using ddmlib to provide a custom
 * port provider on a per-<code>Client</code> basis, depending on the device/emulator they are
 * running on, and/or their names.
 */
public class DebugPortManager {

    private static IDebugPortProvider sProvider = null;

    /**
     * Sets the {@link IDebugPortProvider} that will be used when a new {@link Client} requests
     * a debugger port.
     * @param provider the <code>IDebugPortProvider</code> to use.
     */
    public static void setProvider(IDebugPortProvider provider) {
        sProvider = provider;
    }

    /**
     * Returns the
     * @return
     */
    public static IDebugPortProvider getProvider() {
        return sProvider;
    }
}
