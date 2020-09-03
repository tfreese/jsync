// Created: 23.07.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class StackTracelElementSerializer implements Serializer<StackTraceElement>
{
    /**
     *
     */
    private static final Serializer<StackTraceElement> INSTANCE = new StackTracelElementSerializer();

    /**
     * @return Serializer<StackTraceElement>
     */
    static Serializer<StackTraceElement> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link StackTracelElementSerializer} Object.
     */
    StackTracelElementSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public StackTraceElement readFrom(final ByteBuffer buffer)
    {
        String clazzName = Serializers.readFrom(buffer, String.class);
        String methodName = Serializers.readFrom(buffer, String.class);
        String fileName = Serializers.readFrom(buffer, String.class);
        int lineNumber = buffer.getInt();

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final StackTraceElement obj)
    {
        Serializers.writeTo(buffer, obj.getClassName());
        Serializers.writeTo(buffer, obj.getMethodName());
        Serializers.writeTo(buffer, obj.getFileName());
        buffer.putInt(obj.getLineNumber());
    }
}
