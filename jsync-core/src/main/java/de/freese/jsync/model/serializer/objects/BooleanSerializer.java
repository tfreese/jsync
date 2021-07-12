// Created: 22.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

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
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Boolean readFrom(final DataAdapter<D> adapter, final D source)
    {
        return adapter.readBoolean(source);
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final Boolean value)
    {
        adapter.writeBoolean(sink, value);
    }

}
