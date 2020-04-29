/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Interface für ein Verzeichnis oder Datei, welche es zu Synchronisieren gilt.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public interface SyncItem
{
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
     * @return {@link Set}
     * @see PosixFilePermissions
     */
    public Set<PosixFilePermission> getPermissions();

    /**
     * Können unter Windows oder Netzlaufwerken null sein.
     *
     * @return {@link Set}
     */
    public String getPermissionsToString();

    /**
     * Verzeichnis/Datei relativ zum Basis-Verzeichnis.
     *
     * @return String
     */
    public String getRelativePath();

    /**
     * @return {@link User}
     */
    public User getUser();
}
