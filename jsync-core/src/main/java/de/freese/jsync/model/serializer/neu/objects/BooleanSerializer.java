// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu.objects;

import de.freese.jsync.model.serializer.neu.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class BooleanSerializer implements ObjectSerializer<Boolean>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class BooleanSerializerHolder
    {
        /**
         *
         */
        private static final BooleanSerializer INSTANCE = new BooleanSerializer();

        /**
         * Erstellt ein neues {@link BooleanSerializerHolder} Object.
         */
        private BooleanSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link BooleanSerializer}
     */
    public static BooleanSerializer getInstance()
    {
        return BooleanSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link BooleanSerializer} Object.
     */
    private BooleanSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Boolean readFrom(final DataAdapter<D> adapter, final D source)
    {
        return adapter.readByte(source) == 1;
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Boolean value)
    {
        adapter.writeByte(sink, (byte) (Boolean.TRUE.equals(value) ? 1 : 0));
    }

}
