// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * @param uid unix:uid
 *
 * @author Thomas Freese
 */
public record User(String name, int uid) {
    public static final int ID_MAX = 65535;
    public static final User NOBODY = new User("nobody", ID_MAX - 1);
    public static final User ROOT = new User("root", 0);

    public User(final String name, final int uid) {

        this.name = Objects.requireNonNull(name, "name required");
        this.uid = uid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User(final String name1, final int uid1))) {
            return false;
        }

        return Objects.equals(name, name1) && uid == uid1;
    }

    @Override
    public String toString() {
        return "User ["
                + "uid=" + uid +
                ", name=" + name
                + "]";
    }
}
