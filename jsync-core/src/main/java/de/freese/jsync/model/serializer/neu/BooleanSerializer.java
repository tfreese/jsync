// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

/**
 * @author Thomas Freese
 */
class BooleanSerializer implements ObjectSerializer<Boolean>
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
    static BooleanSerializer getInstance()
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
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.DataAdapter)
     */
    @Override
    public Boolean readFrom(final DataAdapter data)
    {
        return data.readInt() == 1;
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.DataAdapter, java.lang.Object)
     */
    @Override
    public void writeTo(final DataAdapter data, final Boolean obj)
    {
        data.writeInt(Boolean.TRUE.equals(obj) ? 1 : 0);
    }
}
