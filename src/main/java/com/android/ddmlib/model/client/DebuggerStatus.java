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

public enum DebuggerStatus {
    /** Debugger connection status: not waiting on one, not connected to one, but accepting
     * new connections. This is the default value. */
    DEFAULT,
    /**
     * Debugger connection status: the application's VM is paused, waiting for a debugger to
     * connect to it before resuming. */
    WAITING,
    /** Debugger connection status : Debugger is connected */
    ATTACHED,
    /** Debugger connection status: The listening port for debugger connection failed to listen.
     * No debugger will be able to connect. */
    ERROR
}
