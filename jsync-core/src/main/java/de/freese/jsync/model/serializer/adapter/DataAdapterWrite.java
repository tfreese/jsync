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

    void writeByte(W sink, byte value);

    void writeBytes(W sink, byte[] bytes);

    default void writeDouble(W sink, double value)
    {
        long longValue = Double.doubleToRawLongBits(value);

        writeLong(sink, longValue);
    }

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

    default void writeFloat(W sink, float value)
    {
        int intValue = Float.floatToRawIntBits(value);

        writeInteger(sink, intValue);
    }

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

    default void writeInteger(W sink, int value)
    {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (0xFF & (value >> 24));
        bytes[1] = (byte) (0xFF & (value >> 16));
        bytes[2] = (byte) (0xFF & (value >> 8));
        bytes[3] = (byte) (0xFF & value);

        writeBytes(sink, bytes);
    }

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

    default void writeLong(W sink, long value)
    {
        byte[] bytes = new byte[8];

        bytes[0] = (byte) (0xFF & (value >> 56));
        bytes[1] = (byte) (0xFF & (value >> 48));
        bytes[2] = (byte) (0xFF & (value >> 40));
        bytes[3] = (byte) (0xFF & (value >> 32));
        bytes[4] = (byte) (0xFF & (value >> 24));
        bytes[5] = (byte) (0xFF & (value >> 16));
        bytes[6] = (byte) (0xFF & (value >> 8));
        bytes[7] = (byte) (0xFF & value);

        writeBytes(sink, bytes);
    }

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
