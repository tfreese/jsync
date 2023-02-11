// Created: 22.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class FloatSerializer implements ObjectSerializer<Float> {
    @Override
    public <W, R> Float readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        return adapter.readFloatWrapper(source);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Float value) {
        adapter.writeFloatWrapper(sink, value);
    }

}
