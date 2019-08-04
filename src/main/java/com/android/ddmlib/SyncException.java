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

import java.io.IOException;

/**
 * Exception thrown when a transfer using {@link SyncService} doesn't complete.
 * <p/>This is different from an {@link IOException} because it's not the underlying connection
 * that triggered the error, but the adb transfer protocol that didn't work somehow, or that the
 * targets (local and/or remote) were wrong.
 */
public class SyncException extends CanceledException {
    private static final long serialVersionUID = 1L;

    public enum SyncError {
        /** canceled transfer */
        CANCELED("Operation was canceled by the user."),
        /** Transfer error */
        TRANSFER_PROTOCOL_ERROR("Adb Transfer Protocol Error."),
        /** unknown remote object during a pull */
        NO_REMOTE_OBJECT("Remote object doesn't exist!"),
        /** Result code when attempting to pull multiple files into a file */
        TARGET_IS_FILE("Target object is a file."),
        /** Result code when attempting to pull multiple into a directory that does not exist. */
        NO_DIR_TARGET("Target directory doesn't exist."),
        /** wrong encoding on the remote path. */
        REMOTE_PATH_ENCODING("Remote Path encoding is not supported."),
        /** remote path that is too long. */
        REMOTE_PATH_LENGTH("Remote path is too long."),
        /** error while reading local file. */
        FILE_READ_ERROR("Reading local file failed!"),
        /** error while writing local file. */
        FILE_WRITE_ERROR("Writing local file failed!"),
        /** attempting to push a directory. */
        LOCAL_IS_DIRECTORY("Local path is a directory."),
        /** attempting to push a non-existent file. */
        NO_LOCAL_FILE("Local path doesn't exist."),
        /** when the target path of a multi file push is a file. */
        REMOTE_IS_FILE("Remote path is a file."),
        /** receiving too much data from the remove device at once */
        BUFFER_OVERRUN("Receiving too much data.");

        private final String mMessage;

        SyncError(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }

    private final SyncError mError;

    public SyncException(SyncError error) {
        super(error.getMessage());
        mError = error;
    }

    public SyncException(SyncError error, String message) {
        super(message);
        mError = error;
    }

    public SyncException(SyncError error, Throwable cause) {
        super(error.getMessage(), cause);
        mError = error;
    }

    public SyncError getErrorCode() {
        return mError;
    }

    /**
     * Returns true if the sync was canceled by user input.
     */
   @Override
   public boolean wasCanceled() {
        return mError == SyncError.CANCELED;
    }
}
