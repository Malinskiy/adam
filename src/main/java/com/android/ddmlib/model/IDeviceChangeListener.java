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

package com.android.ddmlib.model;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.model.IDevice;

/**
 * Classes which implement this interface provide methods that deal
 * with {@link IDevice} addition, deletion, and changes.
 */
public interface IDeviceChangeListener {
    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     */
    void deviceConnected(IDevice device);

    /**
     * Sent when the a device is connected to the {@link AndroidDebugBridge}.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the new device.
     */
    void deviceDisconnected(IDevice device);

    /**
     * Sent when a device data changed, or when clients are started/terminated on the device.
     * <p/>
     * This is sent from a non UI thread.
     * @param device the device that was updated.
     * @param changeMask the mask describing what changed. It can contain any of the following
     * values: {@link IDevice#CHANGE_BUILD_INFO}, {@link IDevice#CHANGE_STATE},
     * {@link IDevice#CHANGE_CLIENT_LIST}
     */
    void deviceChanged(IDevice device, int changeMask);
}
