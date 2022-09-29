// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

import java.nio.charset.Charset;

/**
 * Interface für eine Datensenke.<br>
 *
 * @param <W> Type of Sink
 *
 * @author Thomas Freese
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

    default void writeBooleanWrapper(final W sink, final Boolean value)
    {
        if (value == null)
        {
            writeByte(sink, (byte) 0);
        }
        else
        {
            writeByte(sink, (byte) 1);
            writeBoolean(sink, value);
        }
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

    default void writeDoubleWrapper(final W sink, final Double value)
    {
        if (value == null)
        {
            writeByte(sink, (byte) 0);
        }
        else
        {
            writeByte(sink, (byte) 1);
            writeDouble(sink, value);
        }
    }

    /**
     * @param sink Object
     * @param value float
     */
    void writeFloat(W sink, float value);

    default void writeFloatWrapper(final W sink, final Float value)
    {
        if (value == null)
        {
            writeByte(sink, (byte) 0);
        }
        else
        {
            writeByte(sink, (byte) 1);
            writeFloat(sink, value);
        }
    }

    /**
     * @param sink Object
     * @param value int
     */
    void writeInteger(W sink, int value);

    default void writeIntegerWrapper(final W sink, final Integer value)
    {
        if (value == null)
        {
            writeByte(sink, (byte) 0);
        }
        else
        {
            writeByte(sink, (byte) 1);
            writeInteger(sink, value);
        }
    }

    /**
     * @param sink Object
     * @param value long
     */
    void writeLong(W sink, long value);

    default void writeLongWrapper(final W sink, final Long value)
    {
        if (value == null)
        {
            writeByte(sink, (byte) 0);
        }
        else
        {
            writeByte(sink, (byte) 1);
            writeLong(sink, value);
        }
    }

    default void writeString(final W sink, final String value, final Charset charset)
    {
        if (value == null)
        {
            writeInteger(sink, -1);
            return;
        }
        else if (value.isBlank())
        {
            writeInteger(sink, 0);
            return;
        }

        byte[] bytes = value.getBytes(charset);

        writeInteger(sink, bytes.length);
        writeBytes(sink, bytes);
    }
}
