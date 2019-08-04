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
import com.android.ddmlib.Device;
import com.android.ddmlib.model.IDevice;

/**
 * Classes which implement this interface provide a method that provides a non random
 * debugger port for a newly created {@link Client}.
 */
public interface IDebugPortProvider {

    int NO_STATIC_PORT = -1;

    /**
     * Returns a non-random debugger port for the specified application running on the
     * specified {@link Device}.
     * @param device The device the application is running on.
     * @param appName The application name, as defined in the <code>AndroidManifest.xml</code>
     * <var>package</var> attribute of the <var>manifest</var> node.
     * @return The non-random debugger port or {@link #NO_STATIC_PORT} if the {@link Client}
     * should use the automatic debugger port provider.
     */
    int getPort(IDevice device, String appName);
}
