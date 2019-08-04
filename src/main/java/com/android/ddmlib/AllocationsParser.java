/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.nio.ByteBuffer;

public class AllocationsParser {
  /**
   * Converts a VM class descriptor string ("Landroid/os/Debug;") to
   * a dot-notation class name ("android.os.Debug").
   */
  private static String descriptorToDot(String str) {
    // count the number of arrays.
    int array = 0;
    while (str.startsWith("[")) {
      str = str.substring(1);
      array++;
    }

    int len = str.length();

        /* strip off leading 'L' and trailing ';' if appropriate */
    if (len >= 2 && str.charAt(0) == 'L' && str.charAt(len - 1) == ';') {
      str = str.substring(1, len-1);
      str = str.replace('/', '.');
    } else {
      // convert the basic types
      if ("C".equals(str)) {
        str = "char";
      } else if ("B".equals(str)) {
        str = "byte";
      } else if ("Z".equals(str)) {
        str = "boolean";
      } else if ("S".equals(str)) {
        str = "short";
      } else if ("I".equals(str)) {
        str = "int";
      } else if ("J".equals(str)) {
        str = "long";
      } else if ("F".equals(str)) {
        str = "float";
      } else if ("D".equals(str)) {
        str = "double";
      }
    }

    // now add the array part
    for (int a = 0 ; a < array; a++) {
      str += "[]";
    }

    return str;
  }

  /**
   * Reads a string table out of "data".
   *
   * This is just a serial collection of strings, each of which is a
   * four-byte length followed by UTF-16 data.
   */
  private static void readStringTable(ByteBuffer data, String[] strings) {
    int count = strings.length;
    int i;

    for (i = 0; i < count; i++) {
      int nameLen = data.getInt();
      String descriptor = ByteBufferUtil.getString(data, nameLen);
      strings[i] = descriptorToDot(descriptor);
    }
  }

  /*
   * Message format:
   *   Message header (all values big-endian):
   *     (1b) message header len (to allow future expansion); includes itself
   *     (1b) entry header len
   *     (1b) stack frame len
   *     (2b) number of entries
   *     (4b) offset to string table from start of message
   *     (2b) number of class name strings
   *     (2b) number of method name strings
   *     (2b) number of source file name strings
   *   For each entry:
   *     (4b) total allocation size
   *     (2b) threadId
   *     (2b) allocated object's class name index
   *     (1b) stack depth
   *     For each stack frame:
   *       (2b) method's class name
   *       (2b) method name
   *       (2b) method source file
   *       (2b) line number, clipped to 32767; -2 if native; -1 if no source
   *   (xb) class name strings
   *   (xb) method name strings
   *   (xb) source file strings
   *
   *   As with other DDM traffic, strings are sent as a 4-byte length
   *   followed by UTF-16 data.
  */
  @NonNull
  public static AllocationInfo[] parse(@NonNull ByteBuffer data) {
    int messageHdrLen, entryHdrLen, stackFrameLen;
    int numEntries, offsetToStrings;
    int numClassNames, numMethodNames, numFileNames;

    /*
     * Read the header.
     */
    messageHdrLen = (data.get() & 0xff);
    entryHdrLen = (data.get() & 0xff);
    stackFrameLen = (data.get() & 0xff);
    numEntries = (data.getShort() & 0xffff);
    offsetToStrings = data.getInt();
    numClassNames = (data.getShort() & 0xffff);
    numMethodNames = (data.getShort() & 0xffff);
    numFileNames = (data.getShort() & 0xffff);


    /*
     * Skip forward to the strings and read them.
     */
    data.position(offsetToStrings);

    String[] classNames = new String[numClassNames];
    String[] methodNames = new String[numMethodNames];
    String[] fileNames = new String[numFileNames];

    readStringTable(data, classNames);
    readStringTable(data, methodNames);
    readStringTable(data, fileNames);

    /*
     * Skip back to a point just past the header and start reading
     * entries.
     */
    data.position(messageHdrLen);

    AllocationInfo[] allocations = new AllocationInfo[numEntries];
    for (int i = 0; i < numEntries; i++) {
      int totalSize;
      int threadId, classNameIndex, stackDepth;

      totalSize = data.getInt();
      threadId = (data.getShort() & 0xffff);
      classNameIndex = (data.getShort() & 0xffff);
      stackDepth = (data.get() & 0xff);
      /* we've consumed 9 bytes; gobble up any extra */
      for (int skip = 9; skip < entryHdrLen; skip++)
        data.get();

      StackTraceElement[] steArray = new StackTraceElement[stackDepth];

      /*
       * Pull out the stack trace.
       */
      for (int sti = 0; sti < stackDepth; sti++) {
        int methodClassNameIndex, methodNameIndex;
        int methodSourceFileIndex;
        short lineNumber;
        String methodClassName, methodName, methodSourceFile;

        methodClassNameIndex = (data.getShort() & 0xffff);
        methodNameIndex = (data.getShort() & 0xffff);
        methodSourceFileIndex = (data.getShort() & 0xffff);
        lineNumber = data.getShort();

        methodClassName = classNames[methodClassNameIndex];
        methodName = methodNames[methodNameIndex];
        methodSourceFile = fileNames[methodSourceFileIndex];

        steArray[sti] = new StackTraceElement(methodClassName,
                                              methodName, methodSourceFile, lineNumber);

        /* we've consumed 8 bytes; gobble up any extra */
        for (int skip = 8; skip < stackFrameLen; skip++)
          data.get();
      }

      allocations[i] = new AllocationInfo(numEntries - i, classNames[classNameIndex], totalSize, (short) threadId, steArray);
    }
    return allocations;
  }
}
