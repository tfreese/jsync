// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import java.lang.reflect.Constructor;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class ExceptionSerializer implements ObjectSerializer<Exception>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class ExceptionSerializerHolder
    {
        /**
         *
         */
        private static final ExceptionSerializer INSTANCE = new ExceptionSerializer();

        /**
         * Erstellt ein neues {@link ExceptionSerializerHolder} Object.
         */
        private ExceptionSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link ExceptionSerializer}
     */
    public static ExceptionSerializer getInstance()
    {
        return ExceptionSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link ExceptionSerializer} Object.
     */
    private ExceptionSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <D> Exception readFrom(final DataAdapter<D> adapter, final D source)
    {
        String clazzName = StringSerializer.getInstance().readFrom(adapter, source);
        String message = StringSerializer.getInstance().readFrom(adapter, source);
        int stackTraceLength = adapter.readInt(source);

        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++)
        {
            stackTrace[i] = StackTraceElementSerializer.getInstance().readFrom(adapter, source);
        }

        Exception exception = null;

        try
        {
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
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Exception value)
    {
        StringSerializer.getInstance().writeTo(adapter, sink, value.getClass().getName());
        StringSerializer.getInstance().writeTo(adapter, sink, value.getMessage());

        StackTraceElement[] stackTrace = value.getStackTrace();
        adapter.writeInt(sink, stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace)
        {
            StackTraceElementSerializer.getInstance().writeTo(adapter, sink, stackTraceElement);
        }
    }
}
