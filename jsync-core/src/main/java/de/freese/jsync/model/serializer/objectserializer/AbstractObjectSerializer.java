// Created: 14.07.2021
package de.freese.jsync.model.serializer.objectserializer;

import java.nio.charset.Charset;

import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.adapter.DataAdapterRead;
import de.freese.jsync.model.serializer.adapter.DataAdapterWrite;

/**
 * @author Thomas Freese
 *
 * @param <T> Type of Object
 */
public abstract class AbstractObjectSerializer<T> implements ObjectSerializer<T>
{
    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     *
     * @return Boolean
     */
    protected <R> Boolean readBooleanWrapper(final DataAdapterRead<R> adapter, final R source)
    {
        byte value = adapter.readByte(source);

        if (value == -1)
        {
            return null;
        }

        return value == 1;
    }

    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     *
     * @return Double
     */
    protected <R> Double readDoubleWrapper(final DataAdapterRead<R> adapter, final R source)
    {
        double value = adapter.readDouble(source);

        if (Double.isNaN(value))
        {
            // Alternative ?
            return null;
        }

        return value;
    }

    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     *
     * @return Float
     */
    protected <R> Float readFloatWrapper(final DataAdapterRead<R> adapter, final R source)
    {
        float value = adapter.readFloat(source);

        if (Float.isNaN(value))
        {
            // Alternative ?
            return null;
        }

        return value;
    }

    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     *
     * @return Integer
     */
    protected <R> Integer readIntegerWrapper(final DataAdapterRead<R> adapter, final R source)
    {
        int value = adapter.readInt(source);

        if (value == Integer.MIN_VALUE)
        {
            // Alternative ?
            return null;
        }

        return value;
    }

    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     *
     * @return Long
     */
    protected <R> Long readLongWrapper(final DataAdapterRead<R> adapter, final R source)
    {
        long value = adapter.readLong(source);

        if (value == Long.MIN_VALUE)
        {
            // Alternative ?
            return null;
        }

        return value;
    }

    /**
     * @param <R> Type of Source
     * @param adapter {@link DataAdapterRead}
     * @param source Object
     * @param charset {@link Charset}
     *
     * @return String
     */
    protected <R> String readString(final DataAdapterRead<R> adapter, final R source, final Charset charset)
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

        byte[] bytes = adapter.readBytes(source, length);

        return new String(bytes, charset);
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapterWrite}
     * @param sink Object
     * @param value Boolean
     */
    protected <W> void writeBooleanWrapper(final DataAdapterWrite<W> adapter, final W sink, final Boolean value)
    {
        if (value == null)
        {
            adapter.writeByte(sink, (byte) -1);
        }
        else
        {
            adapter.writeByte(sink, (byte) (value ? 1 : 0));
        }
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapterWrite}
     * @param sink Object
     * @param value Double
     */
    protected <W> void writeDoubleWrapper(final DataAdapterWrite<W> adapter, final W sink, final Double value)
    {
        if ((value == null) || value.isNaN() || value.isInfinite())
        {
            // Alternative ?
            adapter.writeDouble(sink, Double.NaN);
        }
        else
        {
            adapter.writeDouble(sink, value);
        }
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapterWrite}
     * @param sink Object
     * @param value Float
     */
    protected <W> void writeFloatWrapper(final DataAdapterWrite<W> adapter, final W sink, final Float value)
    {
        if ((value == null) || value.isNaN() || value.isInfinite())
        {
            // Alternative ?
            adapter.writeFloat(sink, Float.NaN);
        }
        else
        {
            adapter.writeFloat(sink, value);
        }
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapterWrite}
     * @param sink Object
     * @param value Integer
     */
    protected <W> void writeIntegerWrapper(final DataAdapterWrite<W> adapter, final W sink, final Integer value)
    {
        if (value == null)
        {
            // Alternative ?
            adapter.writeInt(sink, Integer.MIN_VALUE);
        }
        else
        {
            adapter.writeInt(sink, value);
        }
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapter}
     * @param sink Object
     * @param value Long
     */
    protected <W> void writeLongWrapper(final DataAdapterWrite<W> adapter, final W sink, final Long value)
    {
        if (value == null)
        {
            // Alternative ?
            adapter.writeLong(sink, Long.MIN_VALUE);
        }
        else
        {
            adapter.writeLong(sink, value);
        }
    }

    /**
     * @param <W> Type of Sink
     * @param adapter {@link DataAdapterWrite}
     * @param sink Object
     * @param value String
     * @param charset {@link Charset}
     */
    protected <W> void writeString(final DataAdapterWrite<W> adapter, final W sink, final String value, final Charset charset)
    {
        if (value == null)
        {
            adapter.writeInt(sink, -1);
            return;
        }
        else if (value.isBlank())
        {
            adapter.writeInt(sink, 0);
            return;
        }

        byte[] bytes = value.getBytes(charset);

        adapter.writeInt(sink, bytes.length);
        adapter.writeBytes(sink, bytes);
    }
}
