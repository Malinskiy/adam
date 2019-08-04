/*
 * Copyright (C) 2008 The Android Open Source Project
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
import com.android.annotations.Nullable;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Holds an Allocation information.
 */
public class AllocationInfo implements IStackTraceInfo {
    private final String mAllocatedClass;
    private final int mAllocNumber;
    private final int mAllocationSize;
    private final short mThreadId;
    private final StackTraceElement[] mStackTrace;

    public enum SortMode {
        NUMBER, SIZE, CLASS, THREAD, ALLOCATION_SITE, IN_CLASS, IN_METHOD
    }

    public static final class AllocationSorter implements Comparator<AllocationInfo> {

        private SortMode mSortMode = SortMode.SIZE;
        private boolean mDescending = true;

        public AllocationSorter() {
        }

        public void setSortMode(@NonNull SortMode mode) {
            if (mSortMode == mode) {
                mDescending = !mDescending;
            } else {
                mSortMode = mode;
            }
        }

        public void setSortMode(@NonNull SortMode mode, boolean descending) {
          mSortMode = mode;
          mDescending = descending;
        }

        @NonNull
        public SortMode getSortMode() {
            return mSortMode;
        }

        public boolean isDescending() {
            return mDescending;
        }

        @Override
        public int compare(AllocationInfo o1, AllocationInfo o2) {
            int diff = 0;
            switch (mSortMode) {
                case NUMBER:
                    diff = o1.mAllocNumber - o2.mAllocNumber;
                    break;
                case SIZE:
                    // pass, since diff is init with 0, we'll use SIZE compare below
                    // as a back up anyway.
                    break;
                case CLASS:
                    diff = o1.mAllocatedClass.compareTo(o2.mAllocatedClass);
                    break;
                case THREAD:
                    diff = o1.mThreadId - o2.mThreadId;
                    break;
                case IN_CLASS:
                    String class1 = o1.getFirstTraceClassName();
                    String class2 = o2.getFirstTraceClassName();
                    diff = compareOptionalString(class1, class2);
                    break;
                case IN_METHOD:
                    String method1 = o1.getFirstTraceMethodName();
                    String method2 = o2.getFirstTraceMethodName();
                    diff = compareOptionalString(method1, method2);
                    break;
                case ALLOCATION_SITE:
                    String desc1 = o1.getAllocationSite();
                    String desc2 = o2.getAllocationSite();
                    diff = compareOptionalString(desc1, desc2);
                    break;
            }

            if (diff == 0) {
                // same? compare on size
                diff = o1.mAllocationSize - o2.mAllocationSize;
            }

            if (mDescending) {
                diff = -diff;
            }

            return diff;
        }

        /** compares two strings that could be null */
        private static int compareOptionalString(String str1, String str2) {
            if (str1 != null) {
                if (str2 == null) {
                    return -1;
                } else {
                    return str1.compareTo(str2);
                }
            } else {
                if (str2 == null) {
                    return 0;
                } else {
                    return 1;
                }
            }
        }
    }

    /*
     * Simple constructor.
     */
    AllocationInfo(int allocNumber, String allocatedClass, int allocationSize,
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
     * @see com.android.ddmlib.IStackTraceInfo#getStackTrace()
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

    public boolean allocatedClassMatches(@NonNull String pattern, @NonNull Locale locale) {
      return mAllocatedClass.toLowerCase(locale).contains(pattern.toLowerCase(locale));
    }

    @NonNull
    public List<String> getMatchingStackFrames(@NonNull String filter, boolean fullTrace, @NonNull Locale locale) {
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
