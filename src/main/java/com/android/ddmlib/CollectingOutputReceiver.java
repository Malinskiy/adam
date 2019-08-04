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


import com.google.common.base.Charsets;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A {@link IShellOutputReceiver} which collects the whole shell output into one
 * {@link String}.
 */
public class CollectingOutputReceiver implements IShellOutputReceiver {
    private CountDownLatch mCompletionLatch;
    private StringBuffer mOutputBuffer = new StringBuffer();
    private AtomicBoolean mIsCanceled = new AtomicBoolean(false);

    public CollectingOutputReceiver() {
    }

    public CollectingOutputReceiver(CountDownLatch commandCompleteLatch) {
        mCompletionLatch = commandCompleteLatch;
    }

    public String getOutput() {
        return mOutputBuffer.toString();
    }

    @Override
    public boolean isCancelled() {
        return mIsCanceled.get();
    }

    /**
     * Cancel the output collection
     */
    public void cancel() {
        mIsCanceled.set(true);
    }

    @Override
    public void addOutput(byte[] data, int offset, int length) {
        if (!isCancelled()) {
            String s;
            s = new String(data, offset, length, Charsets.UTF_8);
            mOutputBuffer.append(s);
        }
    }

    @Override
    public void flush() {
        if (mCompletionLatch != null) {
            mCompletionLatch.countDown();
        }
    }
}
