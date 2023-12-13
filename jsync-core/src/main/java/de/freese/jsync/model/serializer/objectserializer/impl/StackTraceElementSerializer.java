// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class StackTraceElementSerializer implements ObjectSerializer<StackTraceElement> {
    @Override
    public <W, R> StackTraceElement readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        final String clazzName = adapter.readString(source, getCharset());
        final String methodName = adapter.readString(source, getCharset());
        final String fileName = adapter.readString(source, getCharset());
        final int lineNumber = adapter.readInteger(source);

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final StackTraceElement value) {
        adapter.writeString(sink, value.getClassName(), getCharset());
        adapter.writeString(sink, value.getMethodName(), getCharset());
        adapter.writeString(sink, value.getFileName(), getCharset());
        adapter.writeInteger(sink, value.getLineNumber());
    }
}
