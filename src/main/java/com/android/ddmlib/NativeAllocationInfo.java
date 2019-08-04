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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stores native allocation information.
 * <p/>Contains number of allocations, their size and the stack trace.
 * <p/>Note: the ddmlib does not resolve the stack trace automatically. While this class provides
 * storage for resolved stack trace, this is merely for convenience.
 */
public class NativeAllocationInfo {
    /* Keywords used as delimiters in the string representation of a NativeAllocationInfo */
    public static final String END_STACKTRACE_KW = "EndStacktrace";
    public static final String BEGIN_STACKTRACE_KW = "BeginStacktrace:";
    public static final String TOTAL_SIZE_KW = "TotalSize:";
    public static final String SIZE_KW = "Size:";
    public static final String ALLOCATIONS_KW = "Allocations:";

    /* constants for flag bits */
    private static final int FLAG_ZYGOTE_CHILD  = (1<<31);
    private static final int FLAG_MASK          = (FLAG_ZYGOTE_CHILD);

    /** Libraries whose methods will be assumed to be not part of the user code. */
    private static final List<String> FILTERED_LIBRARIES = Arrays.asList(
            "libc.so",
            "libc_malloc_debug_leak.so"
    );

    /** Method names that should be assumed to be not part of the user code. */
    private static final List<Pattern> FILTERED_METHOD_NAME_PATTERNS = Arrays.asList(
            Pattern.compile("malloc", Pattern.CASE_INSENSITIVE),
            Pattern.compile("calloc", Pattern.CASE_INSENSITIVE),
            Pattern.compile("realloc", Pattern.CASE_INSENSITIVE),
            Pattern.compile("operator new", Pattern.CASE_INSENSITIVE),
            Pattern.compile("memalign", Pattern.CASE_INSENSITIVE)
    );

    private final int mSize;

    private final boolean mIsZygoteChild;

    private int mAllocations;
    private final ArrayList<Long> mStackCallAddresses = new ArrayList<Long>();

    private ArrayList<NativeStackCallInfo> mResolvedStackCall = null;

    private boolean mIsStackCallResolved = false;

    /**
     * Constructs a new {@link NativeAllocationInfo}.
     * @param size The size of the allocations.
     * @param allocations the allocation count
     */
    public NativeAllocationInfo(int size, int allocations) {
        this.mSize = size & ~FLAG_MASK;
        this.mIsZygoteChild = ((size & FLAG_ZYGOTE_CHILD) != 0);
        this.mAllocations = allocations;
    }

    /**
     * Adds a stack call address for this allocation.
     * @param address The address to add.
     */
    public void addStackCallAddress(long address) {
        mStackCallAddresses.add(address);
    }

    /**
     * Returns the size of this allocation.
     */
    public int getSize() {
        return mSize;
    }

    /**
     * Returns whether the allocation happened in a child of the zygote
     * process.
     */
    public boolean isZygoteChild() {
        return mIsZygoteChild;
    }

    /**
     * Returns the allocation count.
     */
    public int getAllocationCount() {
        return mAllocations;
    }

    /**
     * Returns whether the stack call addresses have been resolved into
     * {@link NativeStackCallInfo} objects.
     */
    public boolean isStackCallResolved() {
        return mIsStackCallResolved;
    }

    /**
     * Returns the stack call of this allocation as raw addresses.
     * @return the list of addresses where the allocation happened.
     */
    public List<Long> getStackCallAddresses() {
        return mStackCallAddresses;
    }

    /**
     * Sets the resolved stack call for this allocation.
     * <p/>
     * If <code>resolvedStackCall</code> is non <code>null</code> then
     * {@link #isStackCallResolved()} will return <code>true</code> after this call.
     * @param resolvedStackCall The list of {@link NativeStackCallInfo}.
     */
    public synchronized void setResolvedStackCall(List<NativeStackCallInfo> resolvedStackCall) {
        if (mResolvedStackCall == null) {
            mResolvedStackCall = new ArrayList<NativeStackCallInfo>();
        } else {
            mResolvedStackCall.clear();
        }
        mResolvedStackCall.addAll(resolvedStackCall);
        mIsStackCallResolved = !mResolvedStackCall.isEmpty();
    }

    /**
     * Returns the resolved stack call.
     * @return An array of {@link NativeStackCallInfo} or <code>null</code> if the stack call
     * was not resolved.
     * @see #setResolvedStackCall(List)
     * @see #isStackCallResolved()
     */
    public synchronized List<NativeStackCallInfo> getResolvedStackCall() {
        if (mIsStackCallResolved) {
            return mResolvedStackCall;
        }

        return null;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * @param obj the reference object with which to compare.
     * @return <code>true</code> if this object is equal to the obj argument;
     * <code>false</code> otherwise.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj instanceof NativeAllocationInfo) {
            NativeAllocationInfo mi = (NativeAllocationInfo)obj;
            // compare of size and alloc
            if (mSize != mi.mSize || mAllocations != mi.mAllocations) {
                return false;
            }

            // compare stacks
            return stackEquals(mi);
        }
        return false;
    }

    public boolean stackEquals(NativeAllocationInfo mi) {
        if (mStackCallAddresses.size() != mi.mStackCallAddresses.size()) {
            return false;
        }

        int count = mStackCallAddresses.size();
        for (int i = 0 ; i < count ; i++) {
            long a = mStackCallAddresses.get(i);
            long b = mi.mStackCallAddresses.get(i);
            if (a != b) {
                return false;
            }
        }

        return true;
    }


    @Override
    public int hashCode() {
        // Follow Effective Java's recipe re hash codes.
        // Includes all the fields looked at by equals().

        int result = 17;    // arbitrary starting point

        result = 31 * result + mSize;
        result = 31 * result + mAllocations;
        result = 31 * result + mStackCallAddresses.size();

        for (long addr : mStackCallAddresses) {
            result = 31 * result + (int) (addr ^ (addr >>> 32));
        }

        return result;
    }

    /**
     * Returns a string representation of the object.
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(ALLOCATIONS_KW);
        buffer.append(' ');
        buffer.append(mAllocations);
        buffer.append('\n');

        buffer.append(SIZE_KW);
        buffer.append(' ');
        buffer.append(mSize);
        buffer.append('\n');

        buffer.append(TOTAL_SIZE_KW);
        buffer.append(' ');
        buffer.append(mSize * mAllocations);
        buffer.append('\n');

        if (mResolvedStackCall != null) {
            buffer.append(BEGIN_STACKTRACE_KW);
            buffer.append('\n');
            for (NativeStackCallInfo source : mResolvedStackCall) {
                long addr = source.getAddress();
                if (addr == 0) {
                    continue;
                }

                if (source.getLineNumber() != -1) {
                    buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s:%5$d\n", addr,
                            source.getLibraryName(), source.getMethodName(),
                            source.getSourceFile(), source.getLineNumber()));
                } else {
                    buffer.append(String.format("\t%1$08x\t%2$s --- %3$s --- %4$s\n", addr,
                            source.getLibraryName(), source.getMethodName(), source.getSourceFile()));
                }
            }
            buffer.append(END_STACKTRACE_KW);
            buffer.append('\n');
        }

        return buffer.toString();
    }

    /**
     * Returns the first {@link NativeStackCallInfo} that is relevant.
     * <p/>
     * A relevant <code>NativeStackCallInfo</code> is a stack call that is not deep in the
     * lower level of the libc, but the actual method that performed the allocation.
     * @return a <code>NativeStackCallInfo</code> or <code>null</code> if the stack call has not
     * been processed from the raw addresses.
     * @see #setResolvedStackCall(List)
     * @see #isStackCallResolved()
     */
    public synchronized NativeStackCallInfo getRelevantStackCallInfo() {
        if (mIsStackCallResolved && mResolvedStackCall != null) {
            for (NativeStackCallInfo info : mResolvedStackCall) {
                if (isRelevantLibrary(info.getLibraryName())
                        && isRelevantMethod(info.getMethodName())) {
                    return info;
                }
            }

            // couldn't find a relevant one, so we'll return the first one if it exists.
            if (!mResolvedStackCall.isEmpty())
                return mResolvedStackCall.get(0);
        }

        return null;
    }

    private boolean isRelevantLibrary(String libPath) {
        for (String l : FILTERED_LIBRARIES) {
            if (libPath.endsWith(l)) {
                return false;
            }
        }

        return true;
    }

    private boolean isRelevantMethod(String methodName) {
        for (Pattern p : FILTERED_METHOD_NAME_PATTERNS) {
            Matcher m = p.matcher(methodName);
            if (m.find()) {
                return false;
            }
        }

        return true;
    }
}
