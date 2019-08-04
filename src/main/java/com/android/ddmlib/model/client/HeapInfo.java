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

public class HeapInfo {
    public long maxSizeInBytes;
    public long sizeInBytes;
    public long bytesAllocated;
    public long objectsAllocated;
    public long timeStamp;
    public byte reason;

    public HeapInfo(long maxSizeInBytes,
                    long sizeInBytes,
                    long bytesAllocated,
                    long objectsAllocated,
                    long timeStamp,
                    byte reason) {
        this.maxSizeInBytes = maxSizeInBytes;
        this.sizeInBytes = sizeInBytes;
        this.bytesAllocated = bytesAllocated;
        this.objectsAllocated = objectsAllocated;
        this.timeStamp = timeStamp;
        this.reason = reason;
    }
}
