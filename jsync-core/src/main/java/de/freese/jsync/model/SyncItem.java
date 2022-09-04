// Created: 30.10.2016
package de.freese.jsync.model;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Interface für ein Verzeichnis oder Datei.<br>
 *
 * @author Thomas Freese
 */
public interface SyncItem
{
    /**
     * @return String
     */
    String getChecksum();

    /**
     * @return {@link Group}
     */
    Group getGroup();

    /**
     * @return long
     */
    long getLastModifiedTime();

    /**
     * Unter Windows oder Netzlaufwerken können diese Null sein.
     *
     * @return Set<PosixFilePermission>
     */
    Set<PosixFilePermission> getPermissions();

    /**
     * Unter Windows oder Netzlaufwerken können diese Null sein.
     *
     * @return {@link Set}
     */
    default String getPermissionsToString()
    {
        if ((getPermissions() == null) || getPermissions().isEmpty())
        {
            return null;
        }

        return PosixFilePermissions.toString(getPermissions());
    }

    /**
     * Verzeichnis/Datei relativ zum Basis-Verzeichnis.
     *
     * @return String
     */
    String getRelativePath();

    /**
     * Verzeichnis: Anzahl der 1st-Level Children<br>
     * Datei: Größe
     *
     * @return long
     */
    long getSize();

    /**
     * @return {@link User}
     */
    User getUser();

    /**
     * @return boolean
     */
    default boolean isDirectory()
    {
        return !isFile();
    }

    /**
     * @return boolean
     */
    boolean isFile();

    /**
     * @param checksum String
     */
    void setChecksum(final String checksum);

    /**
     * @param isFile boolean
     */
    void setFile(final boolean isFile);

    /**
     * @param group {@link Group}
     */
    void setGroup(final Group group);

    /**
     * @param lastModifiedTime long
     */
    void setLastModifiedTime(final long lastModifiedTime);

    /**
     * @param permissions Set<PosixFilePermission>
     */
    void setPermissions(final Set<PosixFilePermission> permissions);

    /**
     * Verzeichnis: Anzahl der 1st-Level Children<br>
     * Datei: Größe
     *
     * @param size long
     */
    void setSize(final long size);

    /**
     * @param user {@link User}
     */
    void setUser(final User user);
}
