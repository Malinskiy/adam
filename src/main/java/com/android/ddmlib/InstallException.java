/*
 * Copyright (C) 2010 The Android Open Source Project
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

/**
 * Thrown if installation or uninstallation of application fails.
 */
public class InstallException extends CanceledException {
    private static final long serialVersionUID = 1L;

    public InstallException(Throwable cause) {
        super(cause.getMessage(), cause);
    }

    public InstallException(String message) {
        super(message);
    }

    public InstallException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns true if the installation was canceled by user input. This can typically only
     * happen in the sync phase.
     */
    @Override
    public boolean wasCanceled() {
        Throwable cause = getCause();
        return cause instanceof SyncException && ((SyncException)cause).wasCanceled();
    }
}
