/**
 * Created: 16.07.2020
 */

package de.freese.jsync.model;

import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Meta-Daten eines {@link SyncItem}.
 *
 * @author Thomas Freese
 */
public class SyncItemMeta
{
    /**
    *
    */
    private String checksum = null;

    /**
    *
    */
    private Group group = null;

    /**
    *
    */
    private boolean isFile = false;

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
    private long size = 0;

    /**
    *
    */
    private User user = null;

    /**
     * Erstellt ein neues {@link SyncItemMeta} Object.
     */
    public SyncItemMeta()
    {
        super();
    }

    /**
     * @return String
     */
    public String getChecksum()
    {
        return this.checksum;
    }

    /**
     * @return {@link Group}
     */
    public Group getGroup()
    {
        return this.group;
    }

    /**
     * @return long
     */
    public long getLastModifiedTime()
    {
        return this.lastModifiedTime;
    }

    /**
     * @return Set<PosixFilePermission>
     */
    public Set<PosixFilePermission> getPermissions()
    {
        return this.permissions;
    }

    /**
     * Können unter Windows oder Netzlaufwerken null sein.
     *
     * @return {@link Set}
     */
    public String getPermissionsToString()
    {
        if ((getPermissions() == null) || getPermissions().isEmpty())
        {
            return null;
        }

        return PosixFilePermissions.toString(getPermissions());
    }

    /**
     * @return long
     */
    public long getSize()
    {
        return this.size;
    }

    /**
     * @return {@link User}
     */
    public User getUser()
    {
        return this.user;
    }

    /**
     * @return boolean
     */
    public boolean isFile()
    {
        return this.isFile;
    }

    /**
     * @param checksum String
     */
    public void setChecksum(final String checksum)
    {
        this.checksum = checksum;
    }

    /**
     * @param isFile boolean
     */
    public void setFile(final boolean isFile)
    {
        this.isFile = isFile;
    }

    /**
     * @param group {@link Group}
     */
    public void setGroup(final Group group)
    {
        this.group = group;
    }

    /**
     * @param lastModifiedTime long
     */
    public void setLastModifiedTime(final long lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * @param permissions Set<PosixFilePermission>
     */
    public void setPermissions(final Set<PosixFilePermission> permissions)
    {
        this.permissions = permissions;
    }

    /**
     * @param size long
     */
    public void setSize(final long size)
    {
        this.size = size;
    }

    /**
     * @param user {@link User}
     */
    public void setUser(final User user)
    {
        this.user = user;
    }
}
