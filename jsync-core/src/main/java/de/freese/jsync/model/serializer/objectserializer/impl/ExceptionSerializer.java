// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.lang.reflect.Constructor;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class ExceptionSerializer extends AbstractObjectSerializer<Exception>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Exception readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        String clazzName = readString(adapter, source, getCharset());
        String message = readString(adapter, source, getCharset());
        int stackTraceLength = adapter.readInt(source);

        ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++)
        {
            stackTrace[i] = stackTraceElementSerializer.readFrom(registry, adapter, source);
        }

        Exception exception = null;

        try
        {
            @SuppressWarnings("unchecked")
            Class<? extends Exception> clazz = (Class<? extends Exception>) Class.forName(clazzName);
            Constructor<? extends Exception> constructor = clazz.getDeclaredConstructor(String.class);

            exception = constructor.newInstance(message);
        }
        catch (Exception ex)
        {
            exception = new Exception(message);
        }

        exception.setStackTrace(stackTrace);

        return exception;
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Exception value)
    {
        writeString(adapter, sink, value.getClass().getName(), getCharset());
        writeString(adapter, sink, value.getMessage(), getCharset());

        ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        StackTraceElement[] stackTrace = value.getStackTrace();
        adapter.writeInt(sink, stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace)
        {
            stackTraceElementSerializer.writeTo(registry, adapter, sink, stackTraceElement);
        }
    }
}
