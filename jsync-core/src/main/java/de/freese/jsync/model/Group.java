// Created: 29.10.2016
package de.freese.jsync.model;

import java.util.Objects;

/**
 * Enthält die Infos der Gruppe der Datei.
 *
 * @author Thomas Freese
 */
public class Group
{
    /**
     *
     */
    public static final int ID_MAX = 65535;

    /**
     *
     */
    public static final Group NOBODY = new Group("nobody", ID_MAX - 1);

    /**
     *
     */
    public static final Group ROOT = new Group("root", 0);

    /**
     * unix:gid
     */
    private final int gid;

    /**
     *
     */
    private final String name;

    /**
     * Erstellt ein neues {@link Group} Object.
     *
     * @param name String
     * @param gid  int
     */
    public Group(final String name, final int gid)
    {
        super();

        this.name = Objects.requireNonNull(name, "name required");
        this.gid = gid;
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

        if (!(obj instanceof Group))
        {
            return false;
        }

        Group other = (Group) obj;

        if (this.gid != other.gid)
        {
            return false;
        }

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

        return true;
    }

    /**
     * @return int
     */
    public int getGid()
    {
        return this.gid;
    }

    /**
     * @return String
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;

        result = (prime * result) + this.gid;
        result = (prime * result) + ((this.name == null) ? 0 : this.name.hashCode());

        return result;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(" [");
        sb.append("gid=").append(this.gid);
        sb.append(", name='").append(this.name).append('\'');
        sb.append(']');

        return sb.toString();
    }
}
