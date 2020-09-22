// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu;

/**
 * @author Thomas Freese
 */
class StringSerializer implements ObjectSerializer<String>
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
    static StringSerializer getInstance()
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
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.DataAdapter)
     */
    @Override
    public String readFrom(final DataAdapter data)
    {
        return data.readString(getCharset()).toString();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.DataAdapter, java.lang.Object)
     */
    @Override
    public void writeTo(final DataAdapter data, final String obj)
    {
        data.writeString(obj, getCharset());
    }
}
