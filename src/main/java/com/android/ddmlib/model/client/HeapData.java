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

import com.android.ddmlib.model.HeapSegment;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;

/**
 * Heap Information.
 * <p/>The heap is composed of several {@link HeapSegment} objects.
 * <p/>A call to {@link #isHeapDataComplete()} will indicate if the segments (available through
 * {@link #getHeapSegments()}) represent the full heap.
 */
public class HeapData {
    private TreeSet<HeapSegment> mHeapSegments = new TreeSet<HeapSegment>();
    private boolean mHeapDataComplete = false;
    private byte[] mProcessedHeapData;
    private Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> mProcessedHeapMap;

    /**
     * Abandon the current list of heap segments.
     */
    public synchronized void clearHeapData() {
        /* Abandon the old segments instead of just calling .clear().
         * This lets the user hold onto the old set if it wants to.
         */
        mHeapSegments = new TreeSet<HeapSegment>();
        mHeapDataComplete = false;
    }

    /**
     * Add raw HPSG chunk data to the list of heap segments.
     *
     * @param data The raw data from an HPSG chunk.
     */
    public synchronized void addHeapData(ByteBuffer data) {
        HeapSegment hs;

        if (mHeapDataComplete) {
            clearHeapData();
        }

        try {
            hs = new HeapSegment(data);
        } catch (BufferUnderflowException e) {
            System.err.println("Discarding short HPSG data (length " + data.limit() + ")");
            return;
        }

        mHeapSegments.add(hs);
    }

    /**
     * Called when all heap data has arrived.
     */
    public synchronized void sealHeapData() {
        mHeapDataComplete = true;
    }

    /**
     * Returns whether the heap data has been sealed.
     */
    public boolean isHeapDataComplete() {
        return mHeapDataComplete;
    }

    /**
     * Get the collected heap data, if sealed.
     *
     * @return The list of heap segments if the heap data has been sealed, or null if it hasn't.
     */
    public Collection<HeapSegment> getHeapSegments() {
        if (isHeapDataComplete()) {
            return mHeapSegments;
        }
        return null;
    }

    /**
     * Sets the processed heap data.
     *
     * @param heapData The new heap data (can be null)
     */
    public void setProcessedHeapData(byte[] heapData) {
        mProcessedHeapData = heapData;
    }

    /**
     * Get the processed heap data, if present.
     *
     * @return the processed heap data, or null.
     */
    public byte[] getProcessedHeapData() {
        return mProcessedHeapData;
    }

    public void setProcessedHeapMap(Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> heapMap) {
        mProcessedHeapMap = heapMap;
    }

    public Map<Integer, ArrayList<HeapSegment.HeapSegmentElement>> getProcessedHeapMap() {
        return mProcessedHeapMap;
    }
}
