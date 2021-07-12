// Created: 22.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.Group;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class GroupSerializer implements ObjectSerializer<Group>
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
    public static GroupSerializer getInstance()
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
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Group readFrom(final DataAdapter<D> adapter, final D source)
    {
        if (adapter.readInt(source) == 0)
        {
            return null;
        }

        // gid
        int gid = adapter.readInt(source);

        // name
        String name = adapter.readString(source, getCharset());

        return new Group(name, gid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Group value)
    {
        if (value == null)
        {
            adapter.writeInt(sink, 0);
            return;
        }

        adapter.writeInt(sink, 1);

        // gid
        adapter.writeInt(sink, value.getGid());

        // name
        adapter.writeString(sink, value.getName(), getCharset());
    }
}
