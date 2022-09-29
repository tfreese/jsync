// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class UserSerializer implements ObjectSerializer<User>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> User readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        if (adapter.readByte(source) == 0)
        {
            return null;
        }

        // uid
        int uid = adapter.readInteger(source);

        // name
        String name = adapter.readString(source, getCharset());

        return new User(name, uid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final User value)
    {
        if (value == null)
        {
            adapter.writeByte(sink, (byte) 0);
            return;
        }

        adapter.writeByte(sink, (byte) 1);

        // uid
        adapter.writeInteger(sink, value.getUid());

        // name
        adapter.writeString(sink, value.getName(), getCharset());
    }
}
