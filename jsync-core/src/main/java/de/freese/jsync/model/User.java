package de.freese.jsync.model;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

/**
 * @param uid unix:uid
 *
 * @author Thomas Freese
 * @since 29.10.2016
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
    public boolean equals(final Object o) {
        if (!(o instanceof User(final String name1, final int uid1))) {
            return false;
        }

        return uid == uid1 && Objects.equals(name, name1);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uid);
    }

    @Override
    public @NonNull String toString() {
        return "User ["
                + "uid=" + uid
                + ", name=" + name
                + "]";
    }
}
