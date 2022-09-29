// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class DoubleSerializer implements ObjectSerializer<Double>
{
    /**
     * @see ObjectSerializer#readFrom(SerializerRegistry,
     * DataAdapter, Object)
     */
    @Override
    public <D> Double readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        return adapter.readDoubleWrapper(source);
    }

    /**
     * @see ObjectSerializer#writeTo(SerializerRegistry,
     * DataAdapter, Object, Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Double value)
    {
        adapter.writeDoubleWrapper(sink, value);
    }

}
