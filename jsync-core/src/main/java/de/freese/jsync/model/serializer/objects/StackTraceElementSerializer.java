// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class StackTraceElementSerializer implements ObjectSerializer<StackTraceElement>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class StackTraceElementSerializerHolder
    {
        /**
         *
         */
        private static final StackTraceElementSerializer INSTANCE = new StackTraceElementSerializer();

        /**
         * Erstellt ein neues {@link StackTraceElementSerializerHolder} Object.
         */
        private StackTraceElementSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link StackTraceElementSerializer}
     */
    public static StackTraceElementSerializer getInstance()
    {
        return StackTraceElementSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link StackTraceElementSerializer} Object.
     */
    private StackTraceElementSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> StackTraceElement readFrom(final DataAdapter<D> adapter, final D source)
    {
        String clazzName = StringSerializer.getInstance().readFrom(adapter, source);
        String methodName = StringSerializer.getInstance().readFrom(adapter, source);
        String fileName = StringSerializer.getInstance().readFrom(adapter, source);
        int lineNumber = adapter.readInt(source);

        return new StackTraceElement(clazzName, methodName, fileName, lineNumber);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final StackTraceElement value)
    {
        StringSerializer.getInstance().writeTo(adapter, sink, value.getClassName());
        StringSerializer.getInstance().writeTo(adapter, sink, value.getMethodName());
        StringSerializer.getInstance().writeTo(adapter, sink, value.getFileName());
        adapter.writeInt(sink, value.getLineNumber());
    }
}
