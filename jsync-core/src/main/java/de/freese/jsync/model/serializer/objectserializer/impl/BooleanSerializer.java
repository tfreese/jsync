// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class BooleanSerializer implements ObjectSerializer<Boolean>
{
    @Override
    public <W, R> Boolean readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source)
    {
        return adapter.readBooleanWrapper(source);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Boolean value)
    {
        adapter.writeBooleanWrapper(sink, value);
    }
}
