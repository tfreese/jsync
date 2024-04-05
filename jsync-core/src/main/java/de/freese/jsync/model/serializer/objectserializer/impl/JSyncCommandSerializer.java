// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class JSyncCommandSerializer implements ObjectSerializer<JSyncCommand> {
    @Override
    public <W, R> JSyncCommand readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        final String name = adapter.readString(source, getCharset());

        if (name == null || name.isBlank()) {
            return null;
        }

        return JSyncCommand.valueOf(name);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final JSyncCommand value) {
        adapter.writeString(sink, value.name(), getCharset());
    }
}
