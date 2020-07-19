/**
 * Created: 30.10.2016
 */

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
     * @return {@link Group}
     */
    public Group getGroup();

    /**
     * @return long
     */
    public long getLastModifiedTime();

    /**
     * Können unter Windows oder Netzlaufwerken null sein.
     *
     * @return Set<PosixFilePermission>
     */
    public Set<PosixFilePermission> getPermissions();

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
     * @return long
     */
    public long getSize();

    /**
     * @return {@link User}
     */
    public User getUser();

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
     * @param checksum String
     */
    public void setChecksum(final String checksum);

    /**
     * @param isFile boolean
     */
    public void setFile(final boolean isFile);

    /**
     * @param group {@link Group}
     */
    public void setGroup(final Group group);

    /**
     * @param lastModifiedTime long
     */
    public void setLastModifiedTime(final long lastModifiedTime);

    /**
     * @param permissions Set<PosixFilePermission>
     */
    public void setPermissions(final Set<PosixFilePermission> permissions);

    /**
     * @param size long
     */
    public void setSize(final long size);

    /**
     * @param user {@link User}
     */
    public void setUser(final User user);
}
