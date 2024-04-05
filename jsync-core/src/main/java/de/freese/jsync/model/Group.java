// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class Group {
    public static final int ID_MAX = 65535;
    public static final Group NOBODY = new Group("nobody", ID_MAX - 1);
    public static final Group ROOT = new Group("root", 0);
    /**
     * unix:gid
     */
    private final int gid;
    private final String name;

    public Group(final String name, final int gid) {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.gid = gid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Group other)) {
            return false;
        }

        return this.gid == other.gid && Objects.equals(this.name, other.name);
    }

    public int getGid() {
        return this.gid;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.gid, this.name);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("gid=").append(this.gid);
        sb.append(", name='").append(this.name).append('\'');
        sb.append(']');

        return sb.toString();
    }
}
