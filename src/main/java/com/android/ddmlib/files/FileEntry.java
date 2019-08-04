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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an entry in a directory. This can be a file or a directory.
 */
public final class FileEntry {
    /** Pattern to escape filenames for shell command consumption.
     *  This pattern identifies any special characters that need to be escaped with a
     *  backslash. */
    private static final Pattern sEscapePattern = Pattern.compile(
            "([\\\\()*+?\"'&#/\\s])"); //$NON-NLS-1$

    /**
     * Comparator object for FileEntry
     */
    public static Comparator<FileEntry> sEntryComparator = new Comparator<FileEntry>() {
        @Override
        public int compare(FileEntry o1, FileEntry o2) {
            if (o1 instanceof FileEntry && o2 instanceof FileEntry) {
                FileEntry fe1 = o1;
                FileEntry fe2 = o2;
                return fe1.name.compareTo(fe2.name);
            }
            return 0;
        }
    };

    FileEntry parent;
    public String name;
    public String info;
    public String permissions;
    public String size;
    public String date;
    public String time;
    public String owner;
    public String group;
    int type;
    boolean isAppPackage;

    boolean isRoot;

    /**
     * Indicates whether the entry content has been fetched yet, or not.
     */
    long fetchTime = 0;

    final ArrayList<FileEntry> mChildren = new ArrayList<FileEntry>();

    /**
     * Creates a new file entry.
     * @param parent parent entry or null if entry is root
     * @param name name of the entry.
     * @param type entry type. Can be one of the following: {@link FileListingService#TYPE_FILE},
     * {@link FileListingService#TYPE_DIRECTORY}, {@link FileListingService#TYPE_OTHER}.
     */
    public FileEntry(FileEntry parent, String name, int type, boolean isRoot) {
        this.parent = parent;
        this.name = name;
        this.type = type;
        this.isRoot = isRoot;

        checkAppPackageStatus();
    }

    /**
     * Returns the name of the entry
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the size string of the entry, as returned by <code>ls</code>.
     */
    public String getSize() {
        return size;
    }

    /**
     * Returns the size of the entry.
     */
    public int getSizeValue() {
        return Integer.parseInt(size);
    }

    /**
     * Returns the date string of the entry, as returned by <code>ls</code>.
     */
    public String getDate() {
        return date;
    }

    /**
     * Returns the time string of the entry, as returned by <code>ls</code>.
     */
    public String getTime() {
        return time;
    }

    /**
     * Returns the permission string of the entry, as returned by <code>ls</code>.
     */
    public String getPermissions() {
        return permissions;
    }

    /**
     * Returns the owner string of the entry, as returned by <code>ls</code>.
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Returns the group owner of the entry, as returned by <code>ls</code>.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Returns the extra info for the entry.
     * <p/>For a link, it will be a description of the link.
     * <p/>For an application apk file it will be the application package as returned
     * by the Package Manager.
     */
    public String getInfo() {
        return info;
    }

    /**
     * Return the full path of the entry.
     * @return a path string using {@link FileListingService#FILE_SEPARATOR} as separator.
     */
    public String getFullPath() {
        if (isRoot) {
            return FileListingService.FILE_ROOT;
        }
        StringBuilder pathBuilder = new StringBuilder();
        fillPathBuilder(pathBuilder, false);

        return pathBuilder.toString();
    }

    /**
     * Return the fully escaped path of the entry. This path is safe to use in a
     * shell command line.
     * @return a path string using {@link FileListingService#FILE_SEPARATOR} as separator
     */
    public String getFullEscapedPath() {
        StringBuilder pathBuilder = new StringBuilder();
        fillPathBuilder(pathBuilder, true);

        return pathBuilder.toString();
    }

    /**
     * Returns the path as a list of segments.
     */
    public String[] getPathSegments() {
        ArrayList<String> list = new ArrayList<String>();
        fillPathSegments(list);

        return list.toArray(new String[list.size()]);
    }

    /**
     * Returns the Entry type as an int, which will match one of the TYPE_(...) constants
     */
    public int getType() {
        return type;
    }

    /**
     * Sets a new type.
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Returns if the entry is a folder or a link to a folder.
     */
    public boolean isDirectory() {
        return type == FileListingService.TYPE_DIRECTORY || type == FileListingService.TYPE_DIRECTORY_LINK;
    }

    /**
     * Returns the parent entry.
     */
    public FileEntry getParent() {
        return parent;
    }

    /**
     * Returns the cached children of the entry. This returns the cache created from calling
     * <code>FileListingService.getChildren()</code>.
     */
    public FileEntry[] getCachedChildren() {
        return mChildren.toArray(new FileEntry[mChildren.size()]);
    }

    /**
     * Returns the child {@link FileEntry} matching the name.
     * This uses the cached children list.
     * @param name the name of the child to return.
     * @return the FileEntry matching the name or null.
     */
    public FileEntry findChild(String name) {
        for (FileEntry entry : mChildren) {
            if (entry.name.equals(name)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Returns whether the entry is the root.
     */
    public boolean isRoot() {
        return isRoot;
    }

    void addChild(FileEntry child) {
        mChildren.add(child);
    }

    void setChildren(ArrayList<FileEntry> newChildren) {
        mChildren.clear();
        mChildren.addAll(newChildren);
    }

    boolean needFetch() {
        if (fetchTime == 0) {
            return true;
        }
        long current = System.currentTimeMillis();
        return current - fetchTime > FileListingService.REFRESH_TEST;

    }

    /**
     * Returns if the entry is a valid application package.
     */
    public boolean isApplicationPackage() {
        return isAppPackage;
    }

    /**
     * Returns if the file name is an application package name.
     */
    public boolean isAppFileName() {
        Matcher m = FileListingService.sApkPattern.matcher(name);
        return m.matches();
    }

    /**
     * Recursively fills the pathBuilder with the full path
     * @param pathBuilder a StringBuilder used to create the path.
     * @param escapePath Whether the path need to be escaped for consumption by
     * a shell command line.
     */
    protected void fillPathBuilder(StringBuilder pathBuilder, boolean escapePath) {
        if (isRoot) {
            return;
        }

        if (parent != null) {
            parent.fillPathBuilder(pathBuilder, escapePath);
        }
        pathBuilder.append(FileListingService.FILE_SEPARATOR);
        pathBuilder.append(escapePath ? escape(name) : name);
    }

    /**
     * Recursively fills the segment list with the full path.
     * @param list The list of segments to fill.
     */
    protected void fillPathSegments(ArrayList<String> list) {
        if (isRoot) {
            return;
        }

        if (parent != null) {
            parent.fillPathSegments(list);
        }

        list.add(name);
    }

    /**
     * Sets the internal app package status flag. This checks whether the entry is in an app
     * directory like /data/app or /system/app
     */
    private void checkAppPackageStatus() {
        isAppPackage = false;

        String[] segments = getPathSegments();
        if (type == FileListingService.TYPE_FILE && segments.length == 3 && isAppFileName()) {
            isAppPackage = FileListingService.DIRECTORY_APP.equals(segments[1]) &&
                           (FileListingService.DIRECTORY_SYSTEM.equals(segments[0]) || FileListingService.DIRECTORY_DATA.equals(segments[0]));
        }
    }

    /**
     * Returns an escaped version of the entry name.
     * @param entryName
     */
    public static String escape(String entryName) {
        return sEscapePattern.matcher(entryName).replaceAll("\\\\$1"); //$NON-NLS-1$
    }
}
