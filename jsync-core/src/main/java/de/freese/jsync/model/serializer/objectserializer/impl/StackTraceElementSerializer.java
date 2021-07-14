// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class StackTraceElementSerializer extends AbstractObjectSerializer<StackTraceElement>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> StackTraceElement readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        String clazzName = readString(adapter, source, getCharset());
        String methodName = readString(adapter, source, getCharset());
        String fileName = readString(adapter, source, getCharset());
        int lineNumber = adapter.readInt(source);

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final StackTraceElement value)
    {
        writeString(adapter, sink, value.getClassName(), getCharset());
        writeString(adapter, sink, value.getMethodName(), getCharset());
        writeString(adapter, sink, value.getFileName(), getCharset());
        adapter.writeInt(sink, value.getLineNumber());
    }
}
