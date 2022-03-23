// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * Enthält die Informationen des Eigentümers der Datei.
 *
 * @author Thomas Freese
 */
public class User
{
    /**
     *
     */
    public static final int ID_MAX = 65535;
    /**
     *
     */
    public static final User NOBODY = new User("nobody", ID_MAX - 1);
    /**
     *
     */
    public static final User ROOT = new User("root", 0);
    /**
     *
     */
    private final String name;
    /**
     * unix:uid
     */
    private final int uid;

    /**
     * Erstellt ein neues {@link User} Object.
     *
     * @param name String
     * @param uid int
     */
    public User(final String name, final int uid)
    {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.uid = uid;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof User other))
        {
            return false;
        }

        return Objects.equals(this.name, other.name) && (this.uid == other.uid);
    }

    /**
     * @return String
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return int
     */
    public int getUid()
    {
        return this.uid;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return Objects.hash(this.name, this.uid);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("User [uid=");
        builder.append(this.uid);
        builder.append(", name=");
        builder.append(this.name);
        builder.append("]");

        return builder.toString();
    }
}
