// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

/**
 * Interface für eine Datensenke.<br>
 *
 * @author Thomas Freese
 *
 * @param <W> Type of Sink
 */
public interface DataAdapterWrite<W>
{
    /**
     * @param sink Object
     * @param value boolean
     */
    default void writeBoolean(final W sink, final boolean value)
    {
        writeByte(sink, (byte) (value ? 1 : 0));
    }

    /**
     * @param sink Object
     * @param value byte
     */
    void writeByte(W sink, byte value);

    /**
     * @param sink Object
     * @param bytes byte[]
     */
    void writeBytes(W sink, byte[] bytes);

    /**
     * @param sink Object
     * @param value double
     */
    void writeDouble(W sink, double value);

    /**
     * @param sink Object
     * @param value float
     */
    void writeFloat(W sink, float value);

    /**
     * @param sink Object
     * @param value int
     */
    void writeInt(W sink, int value);

    /**
     * @param sink Object
     * @param value long
     */
    void writeLong(W sink, long value);
}