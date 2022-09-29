// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class JSyncCommandSerializer implements ObjectSerializer<JSyncCommand>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> JSyncCommand readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        String name = adapter.readString(source, getCharset());

        if ((name == null) || name.isBlank())
        {
            return null;
        }

        return JSyncCommand.valueOf(name);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final JSyncCommand value)
    {
        adapter.writeString(sink, value.name(), getCharset());
    }
}
