// Created: 30.10.2016
package de.freese.jsync.model;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Objects;
import java.util.Set;

/**
 * Basis-Implementierung für ein Verzeichnis / Datei, welche es zu Synchronisieren gilt.<br>
 * Der Pfad ist relativ zum Basis-Verzeichnis.
 *
 * @author Thomas Freese
 */
public class DefaultSyncItem implements SyncItem
{
    /**
     *
     */
    private String checksum;
    /**
     *
     */
    private Group group;
    /**
     *
     */
    private boolean isFile;
    /**
     *
     */
    private long lastModifiedTime;
    /**
     *
     */
    private Set<PosixFilePermission> permissions;
    /**
     *
     */
    private final String relativePath;
    /**
     *
     */
    private long size;
    /**
     *
     */
    private User user;

    /**
     * Erstellt ein neues {@link DefaultSyncItem} Object.
     *
     * @param relativePath String
     */
    public DefaultSyncItem(final String relativePath)
    {
        super();

        this.relativePath = Objects.requireNonNull(relativePath, "relativePath required");
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getChecksum()
     */
    @Override
    public String getChecksum()
    {
        return this.checksum;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getGroup()
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
     * @see de.freese.jsync.model.SyncItem#getRelativePath()
     */
    @Override
    public String getRelativePath()
    {
        return this.relativePath;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getSize()
     */
    @Override
    public long getSize()
    {
        return this.size;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#getUser()
     */
    @Override
    public User getUser()
    {
        return this.user;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#isFile()
     */
    @Override
    public boolean isFile()
    {
        return this.isFile;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setChecksum(java.lang.String)
     */
    @Override
    public void setChecksum(final String checksum)
    {
        this.checksum = checksum;

    }

    /**
     * @see de.freese.jsync.model.SyncItem#setFile(boolean)
     */
    @Override
    public void setFile(final boolean isFile)
    {
        this.isFile = isFile;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setGroup(de.freese.jsync.model.Group)
     */
    @Override
    public void setGroup(final Group group)
    {
        this.group = group;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setLastModifiedTime(long)
     */
    @Override
    public void setLastModifiedTime(final long lastModifiedTime)
    {
        this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setPermissions(java.util.Set)
     */
    @Override
    public void setPermissions(final Set<PosixFilePermission> permissions)
    {
        this.permissions = permissions;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setSize(long)
     */
    @Override
    public void setSize(final long size)
    {
        this.size = size;
    }

    /**
     * @see de.freese.jsync.model.SyncItem#setUser(de.freese.jsync.model.User)
     */
    @Override
    public void setUser(final User user)
    {
        this.user = user;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SyncItem [");
        sb.append("relativePath=").append(getRelativePath());

        if (isFile())
        {
            sb.append(", size=").append(getSize());
        }

        sb.append("]");

        return sb.toString();
    }
}
