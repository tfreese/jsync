// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class IntegerSerializer implements ObjectSerializer<Integer> {
    @Override
    public <W, R> Integer readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        return adapter.readIntegerWrapper(source);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Integer value) {
        adapter.writeIntegerWrapper(sink, value);
    }

}
