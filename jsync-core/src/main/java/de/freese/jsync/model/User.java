// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * Enthält die Infos des Eigentümers der Datei.
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
     * @param uid  int
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

        if (obj == null)
        {
            return false;
        }

        if (!(obj instanceof User))
        {
            return false;
        }

        User other = (User) obj;

        if (this.name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!this.name.equals(other.name))
        {
            return false;
        }

        if (this.uid != other.uid)
        {
            return false;
        }

        return true;
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
        final int prime = 31;
        int result = 1;

        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());
        result = (prime * result) + this.uid;

        return result;
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
