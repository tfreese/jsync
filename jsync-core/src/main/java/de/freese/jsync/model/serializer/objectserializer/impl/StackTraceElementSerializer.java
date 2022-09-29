// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class StackTraceElementSerializer implements ObjectSerializer<StackTraceElement>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> StackTraceElement readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        String clazzName = adapter.readString(source, getCharset());
        String methodName = adapter.readString(source, getCharset());
        String fileName = adapter.readString(source, getCharset());
        int lineNumber = adapter.readInteger(source);

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final StackTraceElement value)
    {
        adapter.writeString(sink, value.getClassName(), getCharset());
        adapter.writeString(sink, value.getMethodName(), getCharset());
        adapter.writeString(sink, value.getFileName(), getCharset());
        adapter.writeInteger(sink, value.getLineNumber());
    }
}
