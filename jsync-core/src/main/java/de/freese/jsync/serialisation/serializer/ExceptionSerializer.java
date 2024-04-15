// Created: 24.09.2020
package de.freese.jsync.serialisation.serializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class ExceptionSerializer {
    @SuppressWarnings("unchecked")
    public static <R> Exception read(final DataReader<R> reader, final R input) {
        final String clazzName = reader.readString(input);
        final String message = reader.readString(input);
        final int stackTraceLength = reader.readInteger(input);

        final StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++) {
            stackTrace[i] = StackTraceElementSerializer.read(reader, input);
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

    public static <W> void write(final DataWriter<W> writer, final W output, final Exception value) {
        writer.writeString(output, value.getClass().getName());
        writer.writeString(output, value.getMessage());

        final StackTraceElement[] stackTrace = value.getStackTrace();
        writer.writeInteger(output, stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace) {
            StackTraceElementSerializer.write(writer, output, stackTraceElement);
        }
    }

    private ExceptionSerializer() {
        super();
    }
}
