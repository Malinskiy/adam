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

import com.android.ddmlib.model.allocation.AllocationInfo;
import com.android.ddmlib.model.allocation.SortMode;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public final class AllocationSorter implements Comparator<AllocationInfo> {

    private SortMode mSortMode = SortMode.SIZE;
    private boolean mDescending = true;

    public AllocationSorter() {
    }

    public void setSortMode(@NotNull SortMode mode) {
        if (mSortMode == mode) {
            mDescending = !mDescending;
        } else {
            mSortMode = mode;
        }
    }

    public void setSortMode(@NotNull SortMode mode, boolean descending) {
      mSortMode = mode;
      mDescending = descending;
    }

    @NotNull
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
