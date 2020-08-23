// Created: 23.07.2020
package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class BooleanSerializer implements Serializer<Boolean>
{
    /**
     *
     */
    private static final Serializer<Boolean> INSTANCE = new BooleanSerializer();

    /**
     * @return Serializer<Boolean>
     */
    static Serializer<Boolean> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link BooleanSerializer} Object.
     */
    BooleanSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public Boolean readFrom(final ByteBuffer buffer)
    {
        return buffer.get() == 1;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final Boolean obj)
    {
        buffer.put((byte) (Boolean.TRUE.equals(obj) ? 1 : 0));
    }
}
