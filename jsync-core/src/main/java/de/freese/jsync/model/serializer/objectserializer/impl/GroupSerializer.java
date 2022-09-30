// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.Group;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class GroupSerializer implements ObjectSerializer<Group>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <W, R> Group readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source)
    {
        if (adapter.readByte(source) == 0)
        {
            return null;
        }

        // gid
        int gid = adapter.readInteger(source);

        // name
        String name = adapter.readString(source, getCharset());

        return new Group(name, gid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Group value)
    {
        if (value == null)
        {
            adapter.writeByte(sink, (byte) 0);
            return;
        }

        adapter.writeByte(sink, (byte) 1);

        // gid
        adapter.writeInteger(sink, value.getGid());

        // name
        adapter.writeString(sink, value.getName(), getCharset());
    }
}
