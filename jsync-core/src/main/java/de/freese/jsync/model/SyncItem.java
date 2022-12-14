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
    String getChecksum();

    Group getGroup();

    long getLastModifiedTime();

    /**
     * Unter Windows oder Netzlaufwerken können diese Null sein.
     */
    Set<PosixFilePermission> getPermissions();

    /**
     * Unter Windows oder Netzlaufwerken können diese Null sein.
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
     */
    String getRelativePath();

    /**
     * Verzeichnis: Anzahl der 1st-Level Children<br>
     * Datei: Größe
     */
    long getSize();

    User getUser();

    default boolean isDirectory()
    {
        return !isFile();
    }

    boolean isFile();

    void setChecksum(final String checksum);

    void setFile(final boolean isFile);

    void setGroup(final Group group);

    void setLastModifiedTime(final long lastModifiedTime);

    void setPermissions(final Set<PosixFilePermission> permissions);

    /**
     * Verzeichnis: Anzahl der 1st-Level Children<br>
     * Datei: Größe
     */
    void setSize(final long size);

    void setUser(final User user);
}
