// Created: 23.07.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class LongSerializer implements Serializer<Long>
{
    /**
     *
     */
    private static final Serializer<Long> INSTANCE = new LongSerializer();

    /**
     * @return Serializer<Long>
     */
    static Serializer<Long> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link LongSerializer} Object.
     */
    LongSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public Long readFrom(final ByteBuffer buffer)
    {
        return buffer.getLong();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final Long obj)
    {
        buffer.putLong(obj);
    }
}
