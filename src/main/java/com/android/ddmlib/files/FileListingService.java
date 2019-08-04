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

package com.android.ddmlib.files;

import com.android.ddmlib.Device;
import com.android.ddmlib.exception.AdbCommandRejectedException;
import com.android.ddmlib.exception.ShellCommandUnresponsiveException;
import com.android.ddmlib.exception.TimeoutException;
import com.android.ddmlib.receiver.LsReceiver;
import com.android.ddmlib.receiver.MultiLineReceiver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides {@link Device} side file listing service.
 * <p/>To get an instance for a known {@link Device}, call {@link Device#getFileListingService()}.
 */
public final class FileListingService {

    /** Pattern to find filenames that match "*.apk" */
    public static final Pattern sApkPattern =
        Pattern.compile(".*\\.apk", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

    private static final String PM_FULL_LISTING = "pm list packages -f"; //$NON-NLS-1$

    /** Pattern to parse the output of the 'pm -lf' command.<br>
     * The output format looks like:<br>
     * /data/app/myapp.apk=com.mypackage.myapp */
    private static final Pattern sPmPattern = Pattern.compile("^package:(.+?)=(.+)$"); //$NON-NLS-1$

    /** Top level data folder. */
    public static final String DIRECTORY_DATA = "data"; //$NON-NLS-1$
    /** Top level sdcard folder. */
    public static final String DIRECTORY_SDCARD = "sdcard"; //$NON-NLS-1$
    /** Top level mount folder. */
    public static final String DIRECTORY_MNT = "mnt"; //$NON-NLS-1$
    /** Top level system folder. */
    public static final String DIRECTORY_SYSTEM = "system"; //$NON-NLS-1$
    /** Top level temp folder. */
    public static final String DIRECTORY_TEMP = "tmp"; //$NON-NLS-1$
    /** Application folder. */
    public static final String DIRECTORY_APP = "app"; //$NON-NLS-1$

    public static final long REFRESH_RATE = 5000L;
    /**
     * Refresh test has to be slightly lower for precision issue.
     */
    static final long REFRESH_TEST = (long)(REFRESH_RATE * .8);

    /** Entry type: File */
    public static final int TYPE_FILE = 0;
    /** Entry type: Directory */
    public static final int TYPE_DIRECTORY = 1;
    /** Entry type: Directory Link */
    public static final int TYPE_DIRECTORY_LINK = 2;
    /** Entry type: Block */
    public static final int TYPE_BLOCK = 3;
    /** Entry type: Character */
    public static final int TYPE_CHARACTER = 4;
    /** Entry type: Link */
    public static final int TYPE_LINK = 5;
    /** Entry type: Socket */
    public static final int TYPE_SOCKET = 6;
    /** Entry type: FIFO */
    public static final int TYPE_FIFO = 7;
    /** Entry type: Other */
    public static final int TYPE_OTHER = 8;

    /** Device side file separator. */
    public static final String FILE_SEPARATOR = "/"; //$NON-NLS-1$

    public static final String FILE_ROOT = "/"; //$NON-NLS-1$


    /**
     * Regexp pattern to parse the result from ls.
     */
    public static final Pattern LS_L_PATTERN = Pattern.compile(
            "^([bcdlsp-][-r][-w][-xsS][-r][-w][-xsS][-r][-w][-xstST])\\s+(\\S+)\\s+(\\S+)\\s+" +
            "([\\d\\s,]*)\\s+(\\d{4}-\\d\\d-\\d\\d)\\s+(\\d\\d:\\d\\d)\\s+(.*)$"); //$NON-NLS-1$

    public static final Pattern LS_LD_PATTERN = Pattern.compile(
                    "d[rwx-]{9}\\s+\\S+\\s+\\S+\\s+[0-9-]{10}\\s+\\d{2}:\\d{2}$"); //$NON-NLS-1$


    private Device mDevice;
    private FileEntry mRoot;

    private ArrayList<Thread> mThreadList = new ArrayList<Thread>();

    /**
     * Classes which implement this interface provide a method that deals with asynchronous
     * result from <code>ls</code> command on the device.
     *
     * @see FileListingService#getChildren(FileEntry, boolean, FileListingService.IListingReceiver)
     */
    public interface IListingReceiver {
        void setChildren(FileEntry entry, FileEntry[] children);

        void refreshEntry(FileEntry entry);
    }

    /**
     * Creates a File Listing Service for a specified {@link Device}.
     * @param device The Device the service is connected to.
     */
    public FileListingService(Device device) {
        mDevice = device;
    }

    /**
     * Returns the root element.
     * @return the {@link FileEntry} object representing the root element or
     * <code>null</code> if the device is invalid.
     */
    public FileEntry getRoot() {
        if (mDevice != null) {
            if (mRoot == null) {
                mRoot = new FileEntry(null /* parent */, "" /* name */, TYPE_DIRECTORY,
                        true /* isRoot */);
            }

            return mRoot;
        }

        return null;
    }

    /**
     * Returns the children of a {@link FileEntry}.
     * <p/>
     * This method supports a cache mechanism and synchronous and asynchronous modes.
     * <p/>
     * If <var>receiver</var> is <code>null</code>, the device side <code>ls</code>
     * command is done synchronously, and the method will return upon completion of the command.<br>
     * If <var>receiver</var> is non <code>null</code>, the command is launched is a separate
     * thread and upon completion, the receiver will be notified of the result.
     * <p/>
     * The result for each <code>ls</code> command is cached in the parent
     * <code>FileEntry</code>. <var>useCache</var> allows usage of this cache, but only if the
     * cache is valid. The cache is valid only for {@link FileListingService#REFRESH_RATE} ms.
     * After that a new <code>ls</code> command is always executed.
     * <p/>
     * If the cache is valid and <code>useCache == true</code>, the method will always simply
     * return the value of the cache, whether a {@link IListingReceiver} has been provided or not.
     *
     * @param entry The parent entry.
     * @param useCache A flag to use the cache or to force a new ls command.
     * @param receiver A receiver for asynchronous calls.
     * @return The list of children or <code>null</code> for asynchronous calls.
     *
     * @see FileEntry#getCachedChildren()
     */
    public FileEntry[] getChildren(final FileEntry entry, boolean useCache,
            final IListingReceiver receiver) {
        // first thing we do is check the cache, and if we already have a recent
        // enough children list, we just return that.
        if (useCache && !entry.needFetch()) {
            return entry.getCachedChildren();
        }

        // if there's no receiver, then this is a synchronous call, and we
        // return the result of ls
        if (receiver == null) {
            doLs(entry);
            return entry.getCachedChildren();
        }

        // this is a asynchronous call.
        // we launch a thread that will do ls and give the listing
        // to the receiver
        Thread t = new Thread("ls " + entry.getFullPath()) { //$NON-NLS-1$
            @Override
            public void run() {
                doLs(entry);

                receiver.setChildren(entry, entry.getCachedChildren());

                final FileEntry[] children = entry.getCachedChildren();
                if (children.length > 0 && children[0].isApplicationPackage()) {
                    final HashMap<String, FileEntry> map = new HashMap<String, FileEntry>();

                    for (FileEntry child : children) {
                        String path = child.getFullPath();
                        map.put(path, child);
                    }

                    // call pm.
                    String command = PM_FULL_LISTING;
                    try {
                        mDevice.executeShellCommand(command, new MultiLineReceiver() {
                            @Override
                            public void processNewLines(String[] lines) {
                                for (String line : lines) {
                                    if (!line.isEmpty()) {
                                        // get the filepath and package from the line
                                        Matcher m = sPmPattern.matcher(line);
                                        if (m.matches()) {
                                            // get the children with that path
                                            FileEntry entry = map.get(m.group(1));
                                            if (entry != null) {
                                                entry.info = m.group(2);
                                                receiver.refreshEntry(entry);
                                            }
                                        }
                                    }
                                }
                            }
                            @Override
                            public boolean isCancelled() {
                                return false;
                            }
                        });
                    } catch (Exception e) {
                        // adb failed somehow, we do nothing.
                    }
                }


                // if another thread is pending, launch it
                synchronized (mThreadList) {
                    // first remove ourselves from the list
                    mThreadList.remove(this);

                    // then launch the next one if applicable.
                    if (!mThreadList.isEmpty()) {
                        Thread t = mThreadList.get(0);
                        t.start();
                    }
                }
            }
        };

        // we don't want to run multiple ls on the device at the same time, so we
        // store the thread in a list and launch it only if there's no other thread running.
        // the thread will launch the next one once it's done.
        synchronized (mThreadList) {
            // add to the list
            mThreadList.add(t);

            // if it's the only one, launch it.
            if (mThreadList.size() == 1) {
                t.start();
            }
        }

        // and we return null.
        return null;
    }

    /**
     * Returns the children of a {@link FileEntry}.
     * <p/>
     * This method is the explicit synchronous version of
     * {@link #getChildren(FileEntry, boolean, IListingReceiver)}. It is roughly equivalent to
     * calling
     * getChildren(FileEntry, false, null)
     *
     * @param entry The parent entry.
     * @return The list of children
     * @throws TimeoutException in case of timeout on the connection when sending the command.
     * @throws AdbCommandRejectedException if adb rejects the command.
     * @throws ShellCommandUnresponsiveException in case the shell command doesn't send any output
     *            for a period longer than <var>maxTimeToOutputResponse</var>.
     * @throws IOException in case of I/O error on the connection.
     */
    public FileEntry[] getChildrenSync(final FileEntry entry) throws TimeoutException,
            AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        doLsAndThrow(entry);
        return entry.getCachedChildren();
    }

    private void doLs(FileEntry entry) {
        try {
            doLsAndThrow(entry);
        } catch (Exception e) {
            // do nothing
        }
    }

    private void doLsAndThrow(FileEntry entry) throws TimeoutException,
            AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        // create a list that will receive the list of the entries
        ArrayList<FileEntry> entryList = new ArrayList<FileEntry>();

        // create a list that will receive the link to compute post ls;
        ArrayList<String> linkList = new ArrayList<String>();

        try {
            // create the command
            String command = "ls -l " + entry.getFullEscapedPath(); //$NON-NLS-1$
            if (entry.isDirectory()) {
                // If we expect a file to behave like a directory, we should stick a "/" at the end.
                // This is a good habit, and is mandatory for symlinks-to-directories, which will
                // otherwise behave like symlinks.
                command += FILE_SEPARATOR;
            }

            // create the receiver object that will parse the result from ls
            LsReceiver receiver = new LsReceiver(entry, entryList, linkList);

            // call ls.
            mDevice.executeShellCommand(command, receiver);

            // finish the process of the receiver to handle links
            receiver.finishLinks(mDevice, entryList);
        } finally {
            // at this point we need to refresh the viewer
            entry.fetchTime = System.currentTimeMillis();

            // sort the children and set them as the new children
            Collections.sort(entryList, FileEntry.sEntryComparator);
            entry.setChildren(entryList);
        }
    }

}
