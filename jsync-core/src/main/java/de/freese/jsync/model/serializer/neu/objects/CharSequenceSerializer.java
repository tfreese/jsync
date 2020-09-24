// Created: 24.09.2020
package de.freese.jsync.model.serializer.neu.objects;

import de.freese.jsync.model.serializer.neu.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class CharSequenceSerializer implements ObjectSerializer<CharSequence>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class CharSequenceSerializerHolder
    {
        /**
         *
         */
        private static final CharSequenceSerializer INSTANCE = new CharSequenceSerializer();

        /**
         * Erstellt ein neues {@link CharSequenceSerializerHolder} Object.
         */
        private CharSequenceSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link CharSequenceSerializer}
     */
    public static CharSequenceSerializer getInstance()
    {
        return CharSequenceSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link CharSequenceSerializer} Object.
     */
    private CharSequenceSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> CharSequence readFrom(final DataAdapter<D> adapter, final D source)
    {
        int length = adapter.readInt(source);

        if (length == -1)
        {
            return null;
        }
        else if (length == 0)
        {
            return "";
        }

        // String value = adapter.readString(source, length, getCharset());
        byte[] bytes = adapter.readBytes(source, length);
        String value = new String(bytes, getCharset());

        return value;
    }

    /**
     * @see de.freese.jsync.model.serializer.neu.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.neu.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final CharSequence value)
    {
        if (value == null)
        {
            adapter.writeInt(sink, -1);
            return;
        }

        String stringValue = value.toString();

        if (stringValue.isEmpty())
        {
            adapter.writeInt(sink, 0);
            return;
        }

        // adapter.writeString(sink, stringValue, getCharset());
        byte[] bytes = stringValue.getBytes(getCharset());

        adapter.writeInt(sink, bytes.length);
        adapter.writeBytes(sink, bytes);
    }
}
