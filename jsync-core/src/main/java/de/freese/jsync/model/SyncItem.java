// Created: 30.10.2016
package de.freese.jsync.model;

/**
 * @author Thomas Freese
 */
public interface SyncItem
{
    String getChecksum();

    //    Group getGroup();

    long getLastModifiedTime();

    /**
     * For Windows or Net-Drives these can be NULL.
     */
    //    Set<PosixFilePermission> getPermissions();

    /**
     * For Windows or Net-Drives these can be NULL.
     */
    //    default String getPermissionsToString()
    //    {
    //        if ((getPermissions() == null) || getPermissions().isEmpty())
    //        {
    //            return null;
    //        }
    //
    //        return PosixFilePermissions.toString(getPermissions());
    //    }

    String getRelativePath();

    /**
     * Directory: Number of 1st-Level Children<br>
     * File: Size in Bytes
     */
    long getSize();

    //    User getUser();

    default boolean isDirectory()
    {
        return !isFile();
    }

    boolean isFile();

    void setChecksum(final String checksum);

    void setFile(final boolean isFile);

    //    void setGroup(final Group group);

    void setLastModifiedTime(final long lastModifiedTime);

    //    void setPermissions(final Set<PosixFilePermission> permissions);

    /**
     * Directory: Number of 1st-Level Children<br>
     * File: Size in Bytes
     */
    void setSize(final long size);

    //    void setUser(final User user);
}
