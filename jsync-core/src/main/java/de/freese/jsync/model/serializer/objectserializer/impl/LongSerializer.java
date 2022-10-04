// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class LongSerializer implements ObjectSerializer<Long>
{
    @Override
    public <W, R> Long readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source)
    {
        return adapter.readLongWrapper(source);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Long value)
    {
        adapter.writeLongWrapper(sink, value);
    }
}
