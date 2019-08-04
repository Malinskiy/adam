/*
 * Copyright (C) 2007 The Android Open Source Project
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


import com.android.ddmlib.model.ThreadInfo;
import com.android.ddmlib.model.allocation.AllocationInfo;
import com.android.ddmlib.model.client.*;
import com.android.ddmlib.model.nativ.NativeAllocationInfo;
import com.android.ddmlib.model.nativ.NativeLibraryMapInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;


/**
 * Contains the data of a {@link Client}.
 */
public class ClientData {
    /* This is a place to stash data associated with a Client, such as thread
    * states or heap data.  ClientData maps 1:1 to Client, but it's a little
    * cleaner if we separate the data out.
    *
    * Message handlers are welcome to stash arbitrary data here.
    *
    * IMPORTANT: The data here is written by HandleFoo methods and read by
    * FooPanel methods, which run in different threads.  All non-trivial
    * access should be synchronized against the ClientData object.
    */


    /** Temporary name of VM to be ignored. */
    private static final String PRE_INITIALIZED = "<pre-initialized>"; //$NON-NLS-1$

    /**
     * String for feature enabling starting/stopping method profiling
     * @see #hasFeature(String)
     */
    public static final String FEATURE_PROFILING = "method-trace-profiling"; //$NON-NLS-1$

    /**
     * String for feature enabling direct streaming of method profiling data
     * @see #hasFeature(String)
     */
    public static final String FEATURE_PROFILING_STREAMING = "method-trace-profiling-streaming"; //$NON-NLS-1$

    /**
     * String for feature enabling sampling profiler.
     * @see #hasFeature(String)
     */
    public static final String FEATURE_SAMPLING_PROFILER = "method-sample-profiling"; //$NON-NLS-1$

    /**
     * String for feature indicating support for tracing OpenGL calls.
     * @see #hasFeature(String)
     */
    public static final String FEATURE_OPENGL_TRACING = "opengl-tracing"; //$NON-NLS-1$

    /**
     * String for feature indicating support for providing view hierarchy.
     * @see #hasFeature(String)
     */
    public static final String FEATURE_VIEW_HIERARCHY = "view-hierarchy"; //$NON-NLS-1$

    /**
     * String for feature allowing to dump hprof files
     * @see #hasFeature(String)
     */
    public static final String FEATURE_HPROF = "hprof-heap-dump"; //$NON-NLS-1$

    /**
     * String for feature allowing direct streaming of hprof dumps
     * @see #hasFeature(String)
     */
    public static final String FEATURE_HPROF_STREAMING = "hprof-heap-dump-streaming"; //$NON-NLS-1$

    @Deprecated
    private static IHprofDumpHandler sHprofDumpHandler;
    private static IMethodProfilingHandler sMethodProfilingHandler;
    private static IAllocationTrackingHandler sAllocationTrackingHandler;

    // is this a DDM-aware client?
    private boolean mIsDdmAware;

    // the client's process ID
    private final int mPid;

    // Java VM identification string
    private String mVmIdentifier;

    // client's self-description
    private String mClientDescription;

    // client's user id (on device in a multi user environment)
    private int mUserId;

    // client's user id is valid
    private boolean mValidUserId;

    // client's ABI
    private String mAbi;

    // jvm flag: currently only indicates whether checkJni is enabled
    private String mJvmFlags;

    // how interested are we in a debugger?
    private DebuggerStatus mDebuggerInterest;

    // List of supported features by the client.
    private final HashSet<String> mFeatures = new HashSet<String>();

    // Thread tracking (THCR, THDE).
    private TreeMap<Integer, ThreadInfo> mThreadMap;

    /** VM Heap data */
    private final HeapData mHeapData = new HeapData();
    /** Native Heap data */
    private final HeapData mNativeHeapData = new HeapData();

    /** Hprof data */
    private HprofData mHprofData = null;

    private HashMap<Integer, HeapInfo> mHeapInfoMap = new HashMap<Integer, HeapInfo>();

    /** library map info. Stored here since the backtrace data
     * is computed on a need to display basis.
     */
    private ArrayList<NativeLibraryMapInfo> mNativeLibMapInfo =
        new ArrayList<NativeLibraryMapInfo>();

    /** Native Alloc info list */
    private ArrayList<NativeAllocationInfo> mNativeAllocationList =
        new ArrayList<NativeAllocationInfo>();
    private int mNativeTotalMemory;

    private AllocationInfo[] mAllocations;
    private AllocationTrackingStatus mAllocationStatus = AllocationTrackingStatus.UNKNOWN;

    @Deprecated
    private String mPendingHprofDump;

    private MethodProfilingStatus mProfilingStatus = MethodProfilingStatus.UNKNOWN;
    private String mPendingMethodProfiling;

    public void setHprofData(byte[] data) {
        mHprofData = new HprofData(data);
    }

    public void setHprofData(String filename) {
        mHprofData = new HprofData(filename);
    }

    public void clearHprofData() {
        mHprofData = null;
    }

    public HprofData getHprofData() {
        return mHprofData;
    }

    /**
     * Sets the handler to receive notifications when an HPROF dump succeeded or failed.
     * This method is deprecated, please register a client listener and listen for CHANGE_HPROF.
     */
    @Deprecated
    public static void setHprofDumpHandler(IHprofDumpHandler handler) {
        sHprofDumpHandler = handler;
    }

    @Deprecated
    public static IHprofDumpHandler getHprofDumpHandler() {
        return sHprofDumpHandler;
    }

    /**
     * Sets the handler to receive notifications when an HPROF dump succeeded or failed.
     * This method is deprecated, please register a client listener and listen for CHANGE_HPROF.
     */
    public static void setMethodProfilingHandler(IMethodProfilingHandler handler) {
        sMethodProfilingHandler = handler;
    }

    public static IMethodProfilingHandler getMethodProfilingHandler() {
        return sMethodProfilingHandler;
    }

    public static void setAllocationTrackingHandler(@NotNull IAllocationTrackingHandler handler) {
      sAllocationTrackingHandler = handler;
    }

    @Nullable
    public static IAllocationTrackingHandler getAllocationTrackingHandler() {
      return sAllocationTrackingHandler;
    }

    /**
     * Generic constructor.
     */
    ClientData(int pid) {
        mPid = pid;

        mDebuggerInterest = DebuggerStatus.DEFAULT;
        mThreadMap = new TreeMap<Integer,ThreadInfo>();
    }

    /**
     * Returns whether the process is DDM-aware.
     */
    public boolean isDdmAware() {
        return mIsDdmAware;
    }

    /**
     * Sets DDM-aware status.
     */
    public void isDdmAware(boolean aware) {
        mIsDdmAware = aware;
    }

    /**
     * Returns the process ID.
     */
    public int getPid() {
        return mPid;
    }

    /**
     * Returns the Client's VM identifier.
     */
    public String getVmIdentifier() {
        return mVmIdentifier;
    }

    /**
     * Sets VM identifier.
     */
    public void setVmIdentifier(String ident) {
        mVmIdentifier = ident;
    }

    /**
     * Returns the client description.
     * <p/>This is generally the name of the package defined in the
     * <code>AndroidManifest.xml</code>.
     *
     * @return the client description or <code>null</code> if not the description was not yet
     * sent by the client.
     */
    public String getClientDescription() {
        return mClientDescription;
    }

    /**
     * Returns the client's user id.
     * @return user id if set, -1 otherwise
     */
    public int getUserId() {
        return mUserId;
    }

    /**
     * Returns true if the user id of this client was set. Only devices that support multiple
     * users will actually return the user id to ddms. For other/older devices, this will not
     * be set.
     */
    public boolean isValidUserId() {
        return mValidUserId;
    }

    /** Returns the abi flavor (32-bit or 64-bit) of the application, null if unknown or not set. */
    @Nullable
    public String getAbi() {
        return mAbi;
    }

    /** Returns the VM flags in use, or null if unknown. */
    public String getJvmFlags() {
        return mJvmFlags;
    }

    /**
     * Sets client description.
     *
     * There may be a race between HELO and APNM.  Rather than try
     * to enforce ordering on the device, we just don't allow an empty
     * name to replace a specified one.
     */
    public void setClientDescription(String description) {
        if (mClientDescription == null && !description.isEmpty()) {
            /*
             * The application VM is first named <pre-initialized> before being assigned
             * its real name.
             * Depending on the timing, we can get an APNM chunk setting this name before
             * another one setting the final actual name. So if we get a SetClientDescription
             * with this value we ignore it.
             */
            if (!PRE_INITIALIZED.equals(description)) {
                mClientDescription = description;
            }
        }
    }

    public void setUserId(int id) {
        mUserId = id;
        mValidUserId = true;
    }

    public void setAbi(String abi) {
        mAbi = abi;
    }

    public void setJvmFlags(String jvmFlags) {
        mJvmFlags = jvmFlags;
    }

    /**
     * Returns the debugger connection status.
     */
    public DebuggerStatus getDebuggerConnectionStatus() {
        return mDebuggerInterest;
    }

    /**
     * Sets debugger connection status.
     */
    public void setDebuggerConnectionStatus(DebuggerStatus status) {
        mDebuggerInterest = status;
    }

    /**
     * Sets the current heap info values for the specified heap.
     *  @param heapId The heap whose info to update
     * @param sizeInBytes The size of the heap, in bytes
     * @param bytesAllocated The number of bytes currently allocated in the heap
     * @param objectsAllocated The number of objects currently allocated in
     * @param timeStamp
     * @param reason
     */
    public synchronized void setHeapInfo(int heapId,
                                  long maxSizeInBytes,
                                  long sizeInBytes,
                                  long bytesAllocated,
                                  long objectsAllocated,
                                  long timeStamp,
                                  byte reason) {
        mHeapInfoMap.put(heapId, new HeapInfo(maxSizeInBytes, sizeInBytes, bytesAllocated,
                objectsAllocated, timeStamp, reason));
    }

    /**
     * Returns the {@link HeapData} object for the VM.
     */
    public HeapData getVmHeapData() {
        return mHeapData;
    }

    /**
     * Returns the {@link HeapData} object for the native code.
     */
    public HeapData getNativeHeapData() {
        return mNativeHeapData;
    }

    /**
     * Returns an iterator over the list of known VM heap ids.
     * <p/>
     * The caller must synchronize on the {@link ClientData} object while iterating.
     *
     * @return an iterator over the list of heap ids
     */
    public synchronized Iterator<Integer> getVmHeapIds() {
        return mHeapInfoMap.keySet().iterator();
    }

    /**
     * Returns the most-recent info values for the specified VM heap.
     *
     * @param heapId The heap whose info should be returned
     * @return a map containing the info values for the specified heap.
     *         Returns <code>null</code> if the heap ID is unknown.
     */
    public synchronized HeapInfo getVmHeapInfo(int heapId) {
        return mHeapInfoMap.get(heapId);
    }

    /**
     * Adds a new thread to the list.
     */
    public synchronized void addThread(int threadId, String threadName) {
        ThreadInfo attr = new ThreadInfo(threadId, threadName);
        mThreadMap.put(threadId, attr);
    }

    /**
     * Removes a thread from the list.
     */
    public synchronized void removeThread(int threadId) {
        mThreadMap.remove(threadId);
    }

    /**
     * Returns the list of threads as {@link ThreadInfo} objects.
     * <p/>The list is empty until a thread update was requested with
     * {@link Client#requestThreadUpdate()}.
     */
    public synchronized ThreadInfo[] getThreads() {
        Collection<ThreadInfo> threads = mThreadMap.values();
        return threads.toArray(new ThreadInfo[threads.size()]);
    }

    /**
     * Returns the {@link ThreadInfo} by thread id.
     */
    public synchronized ThreadInfo getThread(int threadId) {
        return mThreadMap.get(threadId);
    }

    public synchronized void clearThreads() {
        mThreadMap.clear();
    }

    /**
     * Returns the list of {@link NativeAllocationInfo}.
     * @see Client#requestNativeHeapInformation()
     */
    public synchronized List<NativeAllocationInfo> getNativeAllocationList() {
        return Collections.unmodifiableList(mNativeAllocationList);
    }

    /**
     * adds a new {@link NativeAllocationInfo} to the {@link Client}
     * @param allocInfo The {@link NativeAllocationInfo} to add.
     */
    public synchronized void addNativeAllocation(NativeAllocationInfo allocInfo) {
        mNativeAllocationList.add(allocInfo);
    }

    /**
     * Clear the current malloc info.
     */
    public synchronized void clearNativeAllocationInfo() {
        mNativeAllocationList.clear();
    }

    /**
     * Returns the total native memory.
     * @see Client#requestNativeHeapInformation()
     */
    public synchronized int getTotalNativeMemory() {
        return mNativeTotalMemory;
    }

    public synchronized void setTotalNativeMemory(int totalMemory) {
        mNativeTotalMemory = totalMemory;
    }

    public synchronized void addNativeLibraryMapInfo(long startAddr, long endAddr, String library) {
        mNativeLibMapInfo.add(new NativeLibraryMapInfo(startAddr, endAddr, library));
    }

    /**
     * Returns the list of native libraries mapped in memory for this client.
     */
    public synchronized List<NativeLibraryMapInfo> getMappedNativeLibraries() {
        return Collections.unmodifiableList(mNativeLibMapInfo);
    }

    public synchronized void setAllocationStatus(AllocationTrackingStatus status) {
        mAllocationStatus = status;
    }

    /**
     * Returns the allocation tracking status.
     * @see Client#requestAllocationStatus()
     */
    public synchronized AllocationTrackingStatus getAllocationStatus() {
        return mAllocationStatus;
    }

    public synchronized void setAllocations(AllocationInfo[] allocs) {
        mAllocations = allocs;
    }

    /**
     * Returns the list of tracked allocations.
     * @see Client#requestAllocationDetails()
     */
    @Nullable
    public synchronized AllocationInfo[] getAllocations() {
      return mAllocations;
    }

    public void addFeature(String feature) {
        mFeatures.add(feature);
    }

    /**
     * Returns true if the {@link Client} supports the given <var>feature</var>
     * @param feature The feature to test.
     * @return true if the feature is supported
     *
     * @see ClientData#FEATURE_PROFILING
     * @see ClientData#FEATURE_HPROF
     */
    public boolean hasFeature(String feature) {
        return mFeatures.contains(feature);
    }

    /**
     * Sets the device-side path to the hprof file being written
     * @param pendingHprofDump the file to the hprof file
     */
    @Deprecated
    public void setPendingHprofDump(String pendingHprofDump) {
        mPendingHprofDump = pendingHprofDump;
    }

    /**
     * Returns the path to the device-side hprof file being written.
     */
    @Deprecated
    public String getPendingHprofDump() {
        return mPendingHprofDump;
    }

    @Deprecated
    public boolean hasPendingHprofDump() {
        return mPendingHprofDump != null;
    }

    public synchronized void setMethodProfilingStatus(MethodProfilingStatus status) {
        mProfilingStatus = status;
    }

    /**
     * Returns the method profiling status.
     * @see Client#requestMethodProfilingStatus()
     */
    public synchronized MethodProfilingStatus getMethodProfilingStatus() {
        return mProfilingStatus;
    }

    /**
     * Sets the device-side path to the method profile file being written
     * @param pendingMethodProfiling the file being written
     */
    public void setPendingMethodProfiling(String pendingMethodProfiling) {
        mPendingMethodProfiling = pendingMethodProfiling;
    }

    /**
     * Returns the path to the device-side method profiling file being written.
     */
    public String getPendingMethodProfiling() {
        return mPendingMethodProfiling;
    }
}

