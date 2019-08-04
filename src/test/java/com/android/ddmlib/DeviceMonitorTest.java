/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.ddmlib;

import com.android.annotations.NonNull;

import junit.framework.TestCase;

import org.easymock.EasyMock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DeviceMonitorTest extends TestCase {
    public void testDeviceListMonitor() {
        Map<String, IDevice.DeviceState> map = DeviceMonitor.DeviceListMonitorTask
                .parseDeviceListResponse("R32C801BL5K\tdevice\n0079864fd1d150fd\tunauthorized\n");

        assertEquals(IDevice.DeviceState.ONLINE, map.get("R32C801BL5K"));
        assertEquals(IDevice.DeviceState.UNAUTHORIZED, map.get("0079864fd1d150fd"));
    }

    public void testDeviceListComparator() {
        List<IDevice> previous = Arrays.asList(
                mockDevice("1", IDevice.DeviceState.ONLINE),
                mockDevice("2", IDevice.DeviceState.BOOTLOADER)
        );
        List<IDevice> current = Arrays.asList(
                mockDevice("2", IDevice.DeviceState.ONLINE),
                mockDevice("3", IDevice.DeviceState.OFFLINE)
        );

        DeviceMonitor.DeviceListComparisonResult result = DeviceMonitor.DeviceListComparisonResult
                .compare(previous, current);

        assertEquals(1, result.updated.size());
        assertEquals(IDevice.DeviceState.ONLINE, result.updated.get(previous.get(1)));

        assertEquals(1, result.removed.size());
        assertEquals("1", result.removed.get(0).getSerialNumber());

        assertEquals(1, result.added.size());
        assertEquals("3", result.added.get(0).getSerialNumber());
    }

    private IDevice mockDevice(@NonNull String serial, @NonNull IDevice.DeviceState state) {
        IDevice device = EasyMock.createMock(IDevice.class);
        EasyMock.expect(device.getSerialNumber()).andStubReturn(serial);
        EasyMock.expect(device.getState()).andStubReturn(state);
        EasyMock.replay(device);
        return device;
    }
}
