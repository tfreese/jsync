// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class UserSerializer extends AbstractObjectSerializer<User>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> User readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        if (adapter.readInt(source) == 0)
        {
            return null;
        }

        // uid
        int uid = adapter.readInt(source);

        // name
        String name = readString(adapter, source, getCharset());

        return new User(name, uid);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final User value)
    {
        if (value == null)
        {
            adapter.writeInt(sink, 0);
            return;
        }

        adapter.writeInt(sink, 1);

        // uid
        adapter.writeInt(sink, value.getUid());

        // name
        writeString(adapter, sink, value.getName(), getCharset());
    }
}
