// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.lang.reflect.Constructor;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class ExceptionSerializer implements ObjectSerializer<Exception> {
    @Override
    public <W, R> Exception readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        String clazzName = adapter.readString(source, getCharset());
        String message = adapter.readString(source, getCharset());
        int stackTraceLength = adapter.readInteger(source);

        ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++) {
            stackTrace[i] = stackTraceElementSerializer.readFrom(registry, adapter, source);
        }

        Exception exception = null;

        try {
            // ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // Class<? extends Exception> clazz = (Class<? extends Exception>) classLoader.loadClass(clazzName);
            // Class<? extends Exception> clazz = (Class<? extends Exception>) Class.forName(clazzName, true, classLoader);
            Class<? extends Exception> clazz = (Class<? extends Exception>) Class.forName(clazzName);
            Constructor<? extends Exception> constructor = clazz.getDeclaredConstructor(String.class);

            exception = constructor.newInstance(message);
        }
        catch (Exception ex) {
            exception = new Exception(message);
        }

        exception.setStackTrace(stackTrace);

        return exception;
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Exception value) {
        adapter.writeString(sink, value.getClass().getName(), getCharset());
        adapter.writeString(sink, value.getMessage(), getCharset());

        ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        StackTraceElement[] stackTrace = value.getStackTrace();
        adapter.writeInteger(sink, stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace) {
            stackTraceElementSerializer.writeTo(registry, adapter, sink, stackTraceElement);
        }
    }
}
