// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class StringSerializer implements ObjectSerializer<String>
{
    @Override
    public <W, R> String readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source)
    {
        return adapter.readString(source, getCharset());
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final String value)
    {
        adapter.writeString(sink, value, getCharset());
    }
}
