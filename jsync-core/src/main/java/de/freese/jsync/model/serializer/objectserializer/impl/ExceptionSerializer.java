// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class ExceptionSerializer implements ObjectSerializer<Exception> {
    @SuppressWarnings("unchecked")
    @Override
    public <W, R> Exception readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        final String clazzName = adapter.readString(source, getCharset());
        final String message = adapter.readString(source, getCharset());
        final int stackTraceLength = adapter.readInteger(source);

        final ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        final StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++) {
            stackTrace[i] = stackTraceElementSerializer.readFrom(registry, adapter, source);
        }

        Exception exception = null;

        try {
            //            // final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //            // final Class<? extends Exception> clazz = (Class<? extends Exception>) classLoader.loadClass(clazzName);
            //            // final Class<? extends Exception> clazz = (Class<? extends Exception>) Class.forName(clazzName, true, classLoader);
            final Class<? extends Exception> clazz = (Class<? extends Exception>) Class.forName(clazzName);
            //            final Constructor<? extends Exception> constructor = clazz.getDeclaredConstructor(String.class);
            //
            //            exception = constructor.newInstance(message);

            // A look-up that can find public methods.
            final MethodHandles.Lookup publicMethodHandlesLookup = MethodHandles.publicLookup();

            // Search for method that: have return type of void (Constructor) and accept a String parameter.
            final MethodType methodType = MethodType.methodType(void.class, String.class);

            // Find the constructor based on the MethodType defined above.
            final MethodHandle invokableClassConstructor = publicMethodHandlesLookup.findConstructor(clazz, methodType);

            // Create an instance of the Invokable class by calling the exact handle, pass in the param value.
            exception = (Exception) invokableClassConstructor.invokeWithArguments(message);
        }
        catch (Throwable ex) {
            exception = new Exception(message);
        }

        exception.setStackTrace(stackTrace);

        return exception;
    }

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Exception value) {
        adapter.writeString(sink, value.getClass().getName(), getCharset());
        adapter.writeString(sink, value.getMessage(), getCharset());

        final ObjectSerializer<StackTraceElement> stackTraceElementSerializer = registry.getSerializer(StackTraceElement.class);
        final StackTraceElement[] stackTrace = value.getStackTrace();
        adapter.writeInteger(sink, stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace) {
            stackTraceElementSerializer.writeTo(registry, adapter, sink, stackTraceElement);
        }
    }
}
