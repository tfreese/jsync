// Created: 23.07.2020
package de.freese.jsync.model.serializer;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class ExceptionSerializer implements Serializer<Exception>
{
    /**
     *
     */
    private static final Serializer<Exception> INSTANCE = new ExceptionSerializer();

    /**
     * @return Serializer<Exception>
     */
    static Serializer<Exception> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link ExceptionSerializer} Object.
     */
    ExceptionSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Exception readFrom(final ByteBuffer buffer)
    {
        String clazzName = Serializers.readFrom(buffer, String.class);
        String message = Serializers.readFrom(buffer, String.class);
        int stackTraceLength = buffer.getInt();

        StackTraceElement[] stackTrace = new StackTraceElement[stackTraceLength];

        for (int i = 0; i < stackTrace.length; i++)
        {
            stackTrace[i] = Serializers.readFrom(buffer, StackTraceElement.class);
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
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final Exception obj)
    {
        Serializers.writeTo(buffer, obj.getClass().getName());
        Serializers.writeTo(buffer, obj.getMessage());

        StackTraceElement[] stackTrace = obj.getStackTrace();
        buffer.putInt(stackTrace.length);

        for (StackTraceElement stackTraceElement : stackTrace)
        {
            Serializers.writeTo(buffer, stackTraceElement);
        }
    }
}
