// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.Group;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class GroupSerializer extends AbstractObjectSerializer<Group>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Group readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        if (adapter.readInt(source) == 0)
        {
            return null;
        }

        // gid
        int gid = adapter.readInt(source);

        // name
        String name = readString(adapter, source, getCharset());

        return new Group(name, gid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Group value)
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
        writeString(adapter, sink, value.getName(), getCharset());
    }
}
