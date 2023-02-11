// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class DoubleSerializer implements ObjectSerializer<Double> {
    @Override
    public <W, R> Double readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        return adapter.readDoubleWrapper(source);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Double value) {
        adapter.writeDoubleWrapper(sink, value);
    }

}
