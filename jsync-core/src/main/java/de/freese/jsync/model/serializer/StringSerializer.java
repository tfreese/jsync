// Created: 23.07.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class StringSerializer implements Serializer<String>
{
    /**
     *
     */
    private static final Serializer<String> INSTANCE = new StringSerializer();

    /**
     * @return Serializer<String>
     */
    static Serializer<String> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link StringSerializer} Object.
     */
    StringSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public String readFrom(final ByteBuffer buffer)
    {
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String text = new String(bytes, getCharset());

        return text;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final String obj)
    {
        byte[] bytes = obj.getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);
    }
}
