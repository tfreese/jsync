// Created: 30.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class DefaultSyncItem implements SyncItem {
    private final String relativePath;

    private String checksum;

    //    private Group group;

    private boolean isFile;

    private long lastModifiedTime;

    //    private Set<PosixFilePermission> permissions;

    private long size;

    //    private User user;

    public DefaultSyncItem(final String relativePath) {
        super();

        this.relativePath = Objects.requireNonNull(relativePath, "relativePath required");
    }

    @Override
    public String getChecksum() {
        return this.checksum;
    }

    //    @Override
    //    public Group getGroup()
    //    {
    //        return this.group;
    //    }

    @Override
    public long getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    //    @Override
    //    public Set<PosixFilePermission> getPermissions()
    //    {
    //        return this.permissions;
    //    }

    @Override
    public String getRelativePath() {
        return this.relativePath;
    }

    @Override
    public long getSize() {
        return this.size;
    }

    //    @Override
    //    public User getUser()
    //    {
    //        return this.user;
    //    }

    @Override
    public boolean isFile() {
        return this.isFile;
    }

    @Override
    public void setChecksum(final String checksum) {
        this.checksum = checksum;
    }

    @Override
    public void setFile(final boolean isFile) {
        this.isFile = isFile;
    }

    //    @Override
    //    public void setGroup(final Group group)
    //    {
    //        this.group = group;
    //    }

    @Override
    public void setLastModifiedTime(final long lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
    }

    //    @Override
    //    public void setPermissions(final Set<PosixFilePermission> permissions)
    //    {
    //        this.permissions = permissions;
    //    }

    @Override
    public void setSize(final long size) {
        this.size = size;
    }

    //    @Override
    //    public void setUser(final User user)
    //    {
    //        this.user = user;
    //    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SyncItem [");
        sb.append("relativePath=").append(getRelativePath());

        if (isFile()) {
            sb.append(", size=").append(getSize());
        }

        sb.append("]");

        return sb.toString();
    }
}
