// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

import java.nio.charset.Charset;

/**
 * Interface für eine Datenquelle.<br>
 *
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public interface DataAdapterRead<R>
{
    /**
     * @param source Object
     *
     * @return boolean
     */
    default boolean readBoolean(final R source)
    {
        return readByte(source) == 1;
    }

    default Boolean readBooleanWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readBoolean(source);
    }

    /**
     * @param source Object
     *
     * @return byte
     */
    byte readByte(R source);

    /**
     * @param source Object
     * @param length int
     *
     * @return byte[]
     */
    byte[] readBytes(R source, int length);

    /**
     * @param source Object
     *
     * @return double
     */
    double readDouble(R source);

    default Double readDoubleWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readDouble(source);
    }

    /**
     * @param source Object
     *
     * @return float
     */
    float readFloat(R source);

    default Float readFloatWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readFloat(source);
    }

    /**
     * @param source Object
     *
     * @return int
     */
    int readInteger(R source);

    default Integer readIntegerWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readInteger(source);
    }

    /**
     * @param source Object
     *
     * @return long
     */
    long readLong(R source);

    default Long readLongWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readLong(source);
    }

    default String readString(final R source, final Charset charset)
    {
        int length = readInteger(source);

        if (length == -1)
        {
            return null;
        }
        else if (length == 0)
        {
            return "";
        }

        byte[] bytes = readBytes(source, length);

        return new String(bytes, charset);
    }
}
