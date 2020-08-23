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
    public String getChecksum();

    /**
     * @param checksum String
     */
    public void setChecksum(final String checksum);

    /**
     * @return {@link Group}
     */
    public Group getGroup();

    /**
     * @param group {@link Group}
     */
    public void setGroup(final Group group);

    /**
     * @return long
     */
    public long getLastModifiedTime();

    /**
     * @param lastModifiedTime long
     */
    public void setLastModifiedTime(final long lastModifiedTime);

    /**
     * Können unter Windows oder Netzlaufwerken null sein.
     *
     * @return Set<PosixFilePermission>
     */
    public Set<PosixFilePermission> getPermissions();

    /**
     * @param permissions Set<PosixFilePermission>
     */
    public void setPermissions(final Set<PosixFilePermission> permissions);

    /**
     * Können unter Windows oder Netzlaufwerken null sein.
     *
     * @return {@link Set}
     */
    public default String getPermissionsToString()
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
    public String getRelativePath();

    /**
     * Verzeichnis: Anzahl der 1st-Level Childs<br>
     * Datei: Größe
     *
     * @return long
     */
    public long getSize();

    /**
     * Verzeichnis: Anzahl der 1st-Level Childs<br>
     * Datei: Größe
     *
     * @param size long
     */
    public void setSize(final long size);

    /**
     * @return {@link User}
     */
    public User getUser();

    /**
     * @param user {@link User}
     */
    public void setUser(final User user);

    /**
     * @return boolean
     */
    public default boolean isDirectory()
    {
        return !isFile();
    }

    /**
     * @return boolean
     */
    public boolean isFile();

    /**
     * @param isFile boolean
     */
    public void setFile(final boolean isFile);
}
