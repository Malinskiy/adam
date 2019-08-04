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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a stack call. This is used to return all of the call
 * information as one object.
 */
public final class NativeStackCallInfo {
    private static final Pattern SOURCE_NAME_PATTERN = Pattern.compile("^(.+):(\\d+)(\\s+\\(discriminator\\s+\\d+\\))?$");

    /** address of this stack frame */
    private long mAddress;

    /** name of the library */
    private String mLibrary;

    /** name of the method */
    private String mMethod;

    /**
     * name of the source file + line number in the format<br>
     * &lt;sourcefile&gt;:&lt;linenumber&gt;
     */
    private String mSourceFile;

    private int mLineNumber = -1;

    /**
     * Basic constructor with library, method, and sourcefile information
     *
     * @param address address of this stack frame
     * @param lib The name of the library
     * @param method the name of the method
     * @param sourceFile the name of the source file and the line number
     * as "[sourcefile]:[fileNumber]"
     */
    public NativeStackCallInfo(long address, String lib, String method, String sourceFile) {
        mAddress = address;
        mLibrary = lib;
        mMethod = method;

        Matcher m = SOURCE_NAME_PATTERN.matcher(sourceFile);
        if (m.matches()) {
            mSourceFile = m.group(1);
            try {
                mLineNumber = Integer.parseInt(m.group(2));
            } catch (NumberFormatException e) {
                // do nothing, the line number will stay at -1
            }
            if (m.groupCount() == 3) {
                // A discriminator was found, add that in the source file name.
                mSourceFile += m.group(3);
            }
        } else {
            mSourceFile = sourceFile;
        }
    }

    /**
     * Returns the address of this stack frame.
     */
    public long getAddress() {
        return mAddress;
    }

    /**
     * Returns the name of the library name.
     */
    public String getLibraryName() {
        return mLibrary;
    }

    /**
     * Returns the name of the method.
     */
    public String getMethodName() {
        return mMethod;
    }

    /**
     * Returns the name of the source file.
     */
    public String getSourceFile() {
        return mSourceFile;
    }

    /**
     * Returns the line number, or -1 if unknown.
     */
    public int getLineNumber() {
        return mLineNumber;
    }

    @Override
    public String toString() {
        return String.format("\t%1$08x\t%2$s --- %3$s --- %4$s:%5$d",
                getAddress(), getLibraryName(), getMethodName(), getSourceFile(), getLineNumber());
    }
}
