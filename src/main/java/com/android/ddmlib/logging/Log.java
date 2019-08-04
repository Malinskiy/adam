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

package com.android.ddmlib.logging;

import com.android.ddmlib.preferences.DdmPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Log class that mirrors the API in main Android sources.
 * <p/>Default behavior outputs the log to {@link System#out}. Use
 * {@link #setLogOutput(ILogOutput)} to redirect the log somewhere else.
 */
public final class Log {

    private static LogLevel sLevel = DdmPreferences.getLogLevel();

    private static ILogOutput sLogOutput;

    private static final char[] mSpaceLine = new char[72];
    private static final char[] mHexDigit = new char[]
        { '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f' };
    static {
        /* prep for hex dump */
        int i = mSpaceLine.length-1;
        while (i >= 0)
            mSpaceLine[i--] = ' ';
        mSpaceLine[0] = mSpaceLine[1] = mSpaceLine[2] = mSpaceLine[3] = '0';
        mSpaceLine[4] = '-';
    }

    private Log() {}

    /**
     * Outputs a {@link LogLevel#VERBOSE} level message.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void v(String tag, String message) {
        println(LogLevel.VERBOSE, tag, message);
    }

    /**
     * Outputs a {@link LogLevel#DEBUG} level message.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void d(String tag, String message) {
        println(LogLevel.DEBUG, tag, message);
    }

    /**
     * Outputs a {@link LogLevel#INFO} level message.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void i(String tag, String message) {
        println(LogLevel.INFO, tag, message);
    }

    /**
     * Outputs a {@link LogLevel#WARN} level message.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void w(String tag, String message) {
        println(LogLevel.WARN, tag, message);
    }

    /**
     * Outputs a {@link LogLevel#ERROR} level message.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void e(String tag, String message) {
        println(LogLevel.ERROR, tag, message);
    }

    /**
     * Outputs a log message and attempts to display it in a dialog.
     * @param tag The tag associated with the message.
     * @param message The message to output.
     */
    public static void logAndDisplay(LogLevel logLevel, String tag, String message) {
        if (sLogOutput != null) {
            sLogOutput.printAndPromptLog(logLevel, tag, message);
        } else {
            println(logLevel, tag, message);
        }
    }

    /**
     * Outputs a {@link LogLevel#ERROR} level {@link Throwable} information.
     * @param tag The tag associated with the message.
     * @param throwable The {@link Throwable} to output.
     */
    public static void e(String tag, Throwable throwable) {
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            throwable.printStackTrace(pw);
            println(LogLevel.ERROR, tag, throwable.getMessage() + '\n' + sw.toString());
        }
    }

    public static void setLevel(LogLevel logLevel) {
        sLevel = logLevel;
    }

    /**
     * Sets the {@link ILogOutput} to use to print the logs. If not set, {@link System#out}
     * will be used.
     * @param logOutput The {@link ILogOutput} to use to print the log.
     */
    public static void setLogOutput(ILogOutput logOutput) {
        sLogOutput = logOutput;
    }

    /**
     * Show hex dump.
     * <p/>
     * Local addition.  Output looks like:
     * 1230- 00 11 22 33 44 55 66 77 88 99 aa bb cc dd ee ff  0123456789abcdef
     * <p/>
     * Uses no string concatenation; creates one String object per line.
     */
    public static void hexDump(String tag, LogLevel level, byte[] data, int offset, int length) {

        int kHexOffset = 6;
        int kAscOffset = 55;
        char[] line = new char[mSpaceLine.length];
        int addr, baseAddr, count;
        int i, ch;
        boolean needErase = true;

        //Log.w(tag, "HEX DUMP: off=" + offset + ", length=" + length);

        baseAddr = 0;
        while (length != 0) {
            if (length > 16) {
                // full line
                count = 16;
            } else {
                // partial line; re-copy blanks to clear end
                count = length;
                needErase = true;
            }

            if (needErase) {
                System.arraycopy(mSpaceLine, 0, line, 0, mSpaceLine.length);
                needErase = false;
            }

            // output the address (currently limited to 4 hex digits)
            addr = baseAddr;
            addr &= 0xffff;
            ch = 3;
            while (addr != 0) {
                line[ch] = mHexDigit[addr & 0x0f];
                ch--;
                addr >>>= 4;
            }

            // output hex digits and ASCII chars
            ch = kHexOffset;
            for (i = 0; i < count; i++) {
                byte val = data[offset + i];

                line[ch++] = mHexDigit[(val >>> 4) & 0x0f];
                line[ch++] = mHexDigit[val & 0x0f];
                ch++;

                if (val >= 0x20 && val < 0x7f)
                    line[kAscOffset + i] = (char) val;
                else
                    line[kAscOffset + i] = '.';
            }

            println(level, tag, new String(line));

            // advance to next chunk of data
            length -= count;
            offset += count;
            baseAddr += count;
        }

    }

    /**
     * Dump the entire contents of a byte array with DEBUG priority.
     */
    static void hexDump(byte[] data) {
        hexDump("ddms", LogLevel.DEBUG, data, 0, data.length);
    }

    /* currently prints to stdout; could write to a log window */
    private static void println(LogLevel logLevel, String tag, String message) {
        if (logLevel.getPriority() >= sLevel.getPriority()) {
            if (sLogOutput != null) {
                sLogOutput.printLog(logLevel, tag, message);
            } else {
                printLog(logLevel, tag, message);
            }
        }
    }
    
    /**
     * Prints a log message.
     * @param logLevel
     * @param tag
     * @param message
     */
    public static void printLog(LogLevel logLevel, String tag, String message) {
        System.out.print(getLogFormatString(logLevel, tag, message));
    }

    /**
     * Formats a log message.
     * @param logLevel
     * @param tag
     * @param message
     */
    public static String getLogFormatString(LogLevel logLevel, String tag, String message) {
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss", Locale.getDefault());
        return String.format("%s %c/%s: %s\n", formatter.format(new Date()),
                logLevel.getPriorityLetter(), tag, message);
    }
}


