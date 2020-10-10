// Created: 22.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class LongSerializer implements ObjectSerializer<Long>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class LongSerializerHolder
    {
        /**
         *
         */
        private static final LongSerializer INSTANCE = new LongSerializer();

        /**
         * Erstellt ein neues {@link LongSerializerHolder} Object.
         */
        private LongSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link LongSerializer}
     */
    public static LongSerializer getInstance()
    {
        return LongSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link LongSerializer} Object.
     */
    private LongSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Long readFrom(final DataAdapter<D> adapter, final D source)
    {
        return adapter.readLong(source);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Long value)
    {
        adapter.writeLong(sink, value);
    }

}
