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

package com.android.ddmlib.model.allocation;


import com.android.ddmlib.model.IStackTraceInfo;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Holds an Allocation information.
 */
public class AllocationInfo implements IStackTraceInfo {
    public final String mAllocatedClass;
    public final int mAllocNumber;
    public final int mAllocationSize;
    public final short mThreadId;
    private final StackTraceElement[] mStackTrace;

    /*
     * Simple constructor.
     */
    public AllocationInfo(int allocNumber, String allocatedClass, int allocationSize,
        short threadId, StackTraceElement[] stackTrace) {
        mAllocNumber = allocNumber;
        mAllocatedClass = allocatedClass;
        mAllocationSize = allocationSize;
        mThreadId = threadId;
        mStackTrace = stackTrace;
    }

    /**
     * Returns the allocation number. Allocations are numbered as they happen with the most
     * recent one having the highest number
     */
    public int getAllocNumber() {
        return mAllocNumber;
    }

    /**
     * Returns the name of the allocated class.
     */
    public String getAllocatedClass() {
        return mAllocatedClass;
    }

    /**
     * Returns the size of the allocation.
     */
    public int getSize() {
        return mAllocationSize;
    }

    /**
     * Returns the id of the thread that performed the allocation.
     */
    public short getThreadId() {
        return mThreadId;
    }

    /*
     * (non-Javadoc)
     * @see com.android.ddmlib.model.IStackTraceInfo#getStackTrace()
     */
    @Override
    public StackTraceElement[] getStackTrace() {
        return mStackTrace;
    }

    public int compareTo(AllocationInfo otherAlloc) {
        return otherAlloc.mAllocationSize - mAllocationSize;
    }

    @Nullable
    public String getAllocationSite() {
      if (mStackTrace.length > 0) {
        return mStackTrace[0].toString();
      }
      return null;
    }

    public String getFirstTraceClassName() {
        if (mStackTrace.length > 0) {
            return mStackTrace[0].getClassName();
        }

        return null;
    }

    public String getFirstTraceMethodName() {
        if (mStackTrace.length > 0) {
            return mStackTrace[0].getMethodName();
        }

        return null;
    }

    /**
     * Returns true if the given filter matches case insensitively (according to
     * the given locale) this allocation info.
     */
    public boolean filter(String filter, boolean fullTrace, Locale locale) {
        return allocatedClassMatches(filter, locale) || !getMatchingStackFrames(filter, fullTrace, locale).isEmpty();
    }

    public boolean allocatedClassMatches(@NotNull String pattern, @NotNull Locale locale) {
      return mAllocatedClass.toLowerCase(locale).contains(pattern.toLowerCase(locale));
    }

    @NotNull
    public List<String> getMatchingStackFrames(@NotNull String filter, boolean fullTrace, @NotNull Locale locale) {
      filter = filter.toLowerCase(locale);
      // check the top of the stack trace always
      if (mStackTrace.length > 0) {
        final int length = fullTrace ? mStackTrace.length : 1;
        List<String> matchingFrames = Lists.newArrayListWithExpectedSize(length);
        for (int i = 0; i < length; ++i) {
          String frameString = mStackTrace[i].toString();
          if (frameString.toLowerCase(locale).contains(filter)) {
            matchingFrames.add(frameString);
          }
        }
        return matchingFrames;
      } else {
        return Collections.emptyList();
      }
    }
}
