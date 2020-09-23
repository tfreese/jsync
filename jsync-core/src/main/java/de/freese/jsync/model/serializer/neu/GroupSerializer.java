// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

import de.freese.jsync.model.Group;

/**
 * @author Thomas Freese
 */
class GroupSerializer implements ObjectSerializer<Group>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class GroupSerializerHolder
    {
        /**
         *
         */
        private static final GroupSerializer INSTANCE = new GroupSerializer();

        /**
         * Erstellt ein neues {@link GroupSerializerHolder} Object.
         */
        private GroupSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link GroupSerializer}
     */
    static GroupSerializer getInstance()
    {
        return GroupSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link GroupSerializer} Object.
     */
    private GroupSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.DataAdapter)
     */
    @Override
    public Group readFrom(final DataAdapter data)
    {
        // gid
        int gid = data.readInt();

        // name
        String name = data.readString(getCharset());

        return new Group(name, gid);
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.DataAdapter, java.lang.Object)
     */
    @Override
    public void writeTo(final DataAdapter data, final Group obj)
    {
        // gid
        data.writeInt(obj.getGid());

        // name
        data.writeString(obj.getName(), getCharset());
    }
}
