// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class FloatSerializer implements ObjectSerializer<Float>
{
    /**
     * @see ObjectSerializer#readFrom(SerializerRegistry,
     * DataAdapter, Object)
     */
    @Override
    public <D> Float readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        return adapter.readFloatWrapper(source);
    }

    /**
     * @see ObjectSerializer#writeTo(SerializerRegistry,
     * DataAdapter, Object, Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Float value)
    {
        adapter.writeFloatWrapper(sink, value);
    }

}
