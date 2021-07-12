// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class StringSerializer implements ObjectSerializer<String>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class StringSerializerHolder
    {
        /**
         *
         */
        private static final StringSerializer INSTANCE = new StringSerializer();

        /**
         * Erstellt ein neues {@link StringSerializerHolder} Object.
         */
        private StringSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link StringSerializer}
     */
    public static StringSerializer getInstance()
    {
        return StringSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link StringSerializer} Object.
     */
    private StringSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> String readFrom(final DataAdapter<D> adapter, final D source)
    {
        return adapter.readString(source, getCharset());
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final String value)
    {
        adapter.writeString(sink, value, getCharset());
    }
}
