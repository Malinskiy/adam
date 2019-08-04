/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.android.ddmlib.logcat;

import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.logcat.LogCatFilter;
import com.android.ddmlib.logcat.LogCatMessage;

import java.util.List;

import junit.framework.TestCase;

public class LogCatFilterTest extends TestCase {
    public void testFilterByLogLevel() {
        LogCatFilter filter = new LogCatFilter("",
                "", "", "", "", LogLevel.DEBUG);

        /* filter message below filter's log level */
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "", "", "");
        assertEquals(false, filter.matches(msg));

        /* do not filter message above filter's log level */
        msg = new LogCatMessage(LogLevel.ERROR,
                "", "", "", "", "", "");
        assertEquals(true, filter.matches(msg));
    }

    public void testFilterByPid() {
        LogCatFilter filter = new LogCatFilter("",
                "", "", "123", "", LogLevel.VERBOSE);

        /* show message with pid matching filter */
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "123", "", "", "", "", "");
        assertEquals(true, filter.matches(msg));

        /* don't show message with pid not matching filter */
        msg = new LogCatMessage(LogLevel.VERBOSE,
                "12", "", "", "", "", "");
        assertEquals(false, filter.matches(msg));
    }

    public void testFilterByAppNameRegex() {
        LogCatFilter filter = new LogCatFilter("",
                "", "", "", "dalvik.*", LogLevel.VERBOSE);

        /* show message with pid matching filter */
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "dalvikvm1", "", "", "");
        assertEquals(true, filter.matches(msg));

        /* don't show message with pid not matching filter */
        msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "system", "", "", "");
        assertEquals(false, filter.matches(msg));
    }

    public void testFilterByTagRegex() {
        LogCatFilter filter = new LogCatFilter("",
                "tag.*", "", "", "", LogLevel.VERBOSE);

        /* show message with tag matching filter */
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "tag123", "", "");
        assertEquals(true, filter.matches(msg));

        msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "ta123", "", "");
        assertEquals(false, filter.matches(msg));
    }

    public void testFilterByTextRegex() {
        LogCatFilter filter = new LogCatFilter("",
                "", "text.*", "", "", LogLevel.VERBOSE);

        /* show message with text matching filter */
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "", "", "text123");
        assertEquals(true, filter.matches(msg));

        msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "", "", "te123");
        assertEquals(false, filter.matches(msg));
    }

    public void testMatchingText() {
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "", "",                        //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "message with word1 and word2");       //$NON-NLS-1$
        assertEquals(true, search("word1 with", msg)); //$NON-NLS-1$
        assertEquals(true, search("text:w.* ", msg));  //$NON-NLS-1$
        assertEquals(false, search("absent", msg));    //$NON-NLS-1$
    }

    public void testTagKeyword() {
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "tag", "",                     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "sample message");                     //$NON-NLS-1$
        assertEquals(false, search("t.*", msg));       //$NON-NLS-1$
        assertEquals(true, search("tag:t.*", msg));    //$NON-NLS-1$
    }

    public void testPidKeyword() {
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "123", "", "", "", "",                     //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "sample message");                     //$NON-NLS-1$
        assertEquals(false, search("123", msg));       //$NON-NLS-1$
        assertEquals(true, search("pid:123", msg));    //$NON-NLS-1$
    }

    public void testAppNameKeyword() {
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "dalvik", "", "",                  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                "sample message");                     //$NON-NLS-1$
        assertEquals(false, search("dalv.*", msg));    //$NON-NLS-1$
        assertEquals(true, search("app:dal.*k", msg)); //$NON-NLS-1$
    }

    public void testCaseSensitivity() {
        LogCatMessage msg = new LogCatMessage(LogLevel.VERBOSE,
                "", "", "", "", "",
                "Sample message");

        // if regex has an upper case character, it should be
        // treated as a case sensitive search
        assertEquals(false, search("Message", msg));

        // if regex is all lower case, then it should be a
        // case insensitive search
        assertEquals(true, search("sample", msg));
    }

    /**
     * Helper method: search if the query string matches the message.
     * @param query words to search for
     * @param message text to search in
     * @return true if the encoded query is present in message
     */
    private boolean search(String query, LogCatMessage message) {
        List<LogCatFilter> filters = LogCatFilter.fromString(query,
                LogLevel.VERBOSE);

        /* all filters have to match for the query to match */
        for (LogCatFilter f : filters) {
            if (!f.matches(message)) {
                return false;
            }
        }
        return true;
    }
}
