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

package com.android.ddmlib.receiver;

import com.android.ddmlib.files.FileEntry;
import com.android.ddmlib.files.FileListingService;
import com.android.ddmlib.model.IDevice;
import com.android.ddmlib.exception.AdbCommandRejectedException;
import com.android.ddmlib.exception.ShellCommandUnresponsiveException;
import com.android.ddmlib.exception.TimeoutException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;

public class LsReceiver extends MultiLineReceiver {

    private ArrayList<FileEntry> mEntryList;
    private ArrayList<String> mLinkList;
    private FileEntry[] mCurrentChildren;
    private FileEntry mParentEntry;

    /**
     * Create an ls receiver/parser.
     * @param currentChildren The list of current children. To prevent
     *      collapse during update, reusing the same FileEntry objects for
     *      files that were already there is paramount.
     * @param entryList the list of new children to be filled by the
     *      receiver.
     * @param linkList the list of link path to compute post ls, to figure
     *      out if the link pointed to a file or to a directory.
     */
    public LsReceiver(
            FileEntry parentEntry, ArrayList<FileEntry> entryList,
            ArrayList<String> linkList) {
        mParentEntry = parentEntry;
        mCurrentChildren = parentEntry.getCachedChildren();
        mEntryList = entryList;
        mLinkList = linkList;
    }

    @Override
    public void processNewLines(String[] lines) {
        for (String line : lines) {
            // no need to handle empty lines.
            if (line.isEmpty()) {
                continue;
            }

            // run the line through the regexp
            Matcher m = FileListingService.LS_L_PATTERN.matcher(line);
            if (!m.matches()) {
                continue;
            }

            // get the name
            String name = m.group(7);

            // get the rest of the groups
            String permissions = m.group(1);
            String owner = m.group(2);
            String group = m.group(3);
            String size = m.group(4);
            String date = m.group(5);
            String time = m.group(6);
            String info = null;

            // and the type
            int objectType = FileListingService.TYPE_OTHER;
            switch (permissions.charAt(0)) {
                case '-' :
                    objectType = FileListingService.TYPE_FILE;
                    break;
                case 'b' :
                    objectType = FileListingService.TYPE_BLOCK;
                    break;
                case 'c' :
                    objectType = FileListingService.TYPE_CHARACTER;
                    break;
                case 'd' :
                    objectType = FileListingService.TYPE_DIRECTORY;
                    break;
                case 'l' :
                    objectType = FileListingService.TYPE_LINK;
                    break;
                case 's' :
                    objectType = FileListingService.TYPE_SOCKET;
                    break;
                case 'p' :
                    objectType = FileListingService.TYPE_FIFO;
                    break;
            }


            // now check what we may be linking to
            if (objectType == FileListingService.TYPE_LINK) {
                String[] segments = name.split("\\s->\\s"); //$NON-NLS-1$

                // we should have 2 segments
                if (segments.length == 2) {
                    // update the entry name to not contain the link
                    name = segments[0];

                    // and the link name
                    info = segments[1];

                    // now get the path to the link
                    String[] pathSegments = info.split(FileListingService.FILE_SEPARATOR);
                    if (pathSegments.length == 1) {
                        // the link is to something in the same directory,
                        // unless the link is ..
                        if ("..".equals(pathSegments[0])) { //$NON-NLS-1$
                            // set the type and we're done.
                            objectType = FileListingService.TYPE_DIRECTORY_LINK;
                        } else {
                            // either we found the object already
                            // or we'll find it later.
                        }
                    }
                }

                // add an arrow in front to specify it's a link.
                info = "-> " + info; //$NON-NLS-1$;
            }

            // get the entry, either from an existing one, or a new one
            FileEntry entry = getExistingEntry(name);
            if (entry == null) {
                entry = new FileEntry(mParentEntry, name, objectType, false /* isRoot */);
            }

            // add some misc info
            entry.permissions = permissions;
            entry.size = size;
            entry.date = date;
            entry.time = time;
            entry.owner = owner;
            entry.group = group;
            if (objectType == FileListingService.TYPE_LINK) {
                entry.info = info;
            }

            mEntryList.add(entry);
        }
    }

    /**
     * Queries for an already existing Entry per name
     * @param name the name of the entry
     * @return the existing FileEntry or null if no entry with a matching
     * name exists.
     */
    private FileEntry getExistingEntry(String name) {
        for (int i = 0 ; i < mCurrentChildren.length; i++) {
            FileEntry e = mCurrentChildren[i];

            // since we're going to "erase" the one we use, we need to
            // check that the item is not null.
            if (e != null) {
                // compare per name, case-sensitive.
                if (name.equals(e.name)) {
                    // erase from the list
                    mCurrentChildren[i] = null;

                    // and return the object
                    return e;
                }
            }
        }

        // couldn't find any matching object, return null
        return null;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    /**
     * Determine if any symlinks in the <code entries> list are links-to-directories, and if so
     * mark them as such.  This allows us to traverse them properly later on.
     */
    public void finishLinks(IDevice device, ArrayList<FileEntry> entries)
            throws TimeoutException, AdbCommandRejectedException,
                   ShellCommandUnresponsiveException, IOException {
        final int[] nLines = {0};
        MultiLineReceiver receiver = new MultiLineReceiver() {
            @Override
            public void processNewLines(String[] lines) {
                for (String line : lines) {
                    Matcher m = FileListingService.LS_LD_PATTERN.matcher(line);
                    if (m.matches()) {
                        nLines[0]++;
                    }
                }
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };

        for (FileEntry entry : entries) {
            if (entry.getType() != FileListingService.TYPE_LINK) continue;

            // We simply need to determine whether the referent is a directory or not.
            // We do this by running `ls -ld ${link}/`.  If the referent exists and is a
            // directory, we'll see the normal directory listing.  Otherwise, we'll see an
            // error of some sort.
            nLines[0] = 0;

            final String command = String.format("ls -l -d %s%s", entry.getFullEscapedPath(),
                                                 FileListingService.FILE_SEPARATOR);

            device.executeShellCommand(command, receiver);

            if (nLines[0] > 0) {
                // We saw lines matching the directory pattern, so it's a directory!
                entry.setType(FileListingService.TYPE_DIRECTORY_LINK);
            }
        }
    }
}
