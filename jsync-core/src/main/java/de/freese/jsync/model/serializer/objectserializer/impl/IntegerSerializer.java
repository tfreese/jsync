// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class IntegerSerializer implements ObjectSerializer<Integer>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Integer readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        return adapter.readIntegerWrapper(source);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Integer value)
    {
        adapter.writeIntegerWrapper(sink, value);
    }

}
