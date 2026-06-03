// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * @param gid unix:gid
 *
 * @author Thomas Freese
 */
public record Group(String name, int gid) {
    public static final int ID_MAX = 65535;
    public static final Group NOBODY = new Group("nobody", ID_MAX - 1);
    public static final Group ROOT = new Group("root", 0);

    public Group(final String name, final int gid) {

        this.name = Objects.requireNonNull(name, "name required");
        this.gid = gid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Group(final String name1, final int gid1))) {
            return false;
        }

        return gid == gid1 && Objects.equals(name, name1);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ["
                + "gid=" + gid
                + ", name='" + name + '\''
                + ']';
    }
}
