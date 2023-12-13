// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * @author Thomas Freese
 */
public class User {
    public static final int ID_MAX = 65535;
    public static final User NOBODY = new User("nobody", ID_MAX - 1);
    public static final User ROOT = new User("root", 0);

    private final String name;
    /**
     * unix:uid
     */
    private final int uid;

    public User(final String name, final int uid) {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.uid = uid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof User other)) {
            return false;
        }

        return Objects.equals(this.name, other.name) && (this.uid == other.uid);
    }

    public String getName() {
        return this.name;
    }

    public int getUid() {
        return this.uid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.uid);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("User [uid=");
        builder.append(this.uid);
        builder.append(", name=");
        builder.append(this.name);
        builder.append("]");

        return builder.toString();
    }
}
