/**
 * Created: 30.10.2016
 */

package de.freese.jsync.model;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Objects;
import java.util.Set;

/**
 * Basis-Implementierung für ein Verzeichnis / Datei, welche es zu Synchronisieren gilt.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public abstract class AbstractSyncItem implements SyncItem
{
    /**
    *
    */
    private Group group = null;

    /**
    *
    */
    private long lastModifiedTime = 0;

    /**
    *
    */
    private Set<PosixFilePermission> permissions = null;

    /**
    *
    */
    private final String relativePath;

    /**
    *
    */
    private User user = null;

    /**
     * Erstellt ein neues {@link AbstractSyncItem} Object.
     *
     * @param relativePath String
     */
    AbstractSyncItem(final String relativePath)
    {
        super();

        this.relativePath = Objects.requireNonNull(relativePath, "relativePath required");
    }

    /**
     * @return {@link Group}
     */
    @Override
    public Group getGroup()
    {
        return this.group;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getLastModifiedTime()
     */
    @Override
    public long getLastModifiedTime()
    {
        return this.lastModifiedTime;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getPermissions()
     */
    @Override
    public Set<PosixFilePermission> getPermissions()
    {
        return this.permissions;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getPermissionsToString()
     */
    @Override
    public String getPermissionsToString()
    {
        if ((getPermissions() == null) || getPermissions().isEmpty())
        {
            return null;
        }

        return PosixFilePermissions.toString(getPermissions());
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getRelativePath()
     */
    @Override
    public String getRelativePath()
    {
        return this.relativePath;
    }

    /**
     * @return {@link User}
     */
    @Override
    public User getUser()
    {
        return this.user;
    }

    /**
     * @param group {@link Group}
     */
    public void setGroup(final Group group)
    {
        this.group = Objects.requireNonNull(group, "group required");
    }

    /**
     * @param lastModifiedTime long
     */
    public void setLastModifiedTime(final long lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * @param permissions {@link Set}
     */
    public void setPermissions(final Set<PosixFilePermission> permissions)
    {
        this.permissions = Objects.requireNonNull(permissions, "permissions required");
    }

    /**
     * @param user {@link User}
     */
    public void setUser(final User user)
    {
        this.user = Objects.requireNonNull(user, "user required");
    }
}
