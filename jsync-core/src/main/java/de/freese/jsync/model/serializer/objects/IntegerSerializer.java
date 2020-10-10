// Created: 22.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class IntegerSerializer implements ObjectSerializer<Integer>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class IntegerSerializerHolder
    {
        /**
         *
         */
        private static final IntegerSerializer INSTANCE = new IntegerSerializer();

        /**
         * Erstellt ein neues {@link IntegerSerializerHolder} Object.
         */
        private IntegerSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link IntegerSerializer}
     */
    public static IntegerSerializer getInstance()
    {
        return IntegerSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link IntegerSerializer} Object.
     */
    private IntegerSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Integer readFrom(final DataAdapter<D> adapter, final D source)
    {
        return adapter.readInt(source);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Integer value)
    {
        adapter.writeInt(sink, value);
    }

}
