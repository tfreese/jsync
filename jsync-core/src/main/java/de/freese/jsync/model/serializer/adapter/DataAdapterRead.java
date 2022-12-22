// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

import java.nio.charset.Charset;

/**
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public interface DataAdapterRead<R>
{
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

    byte readByte(R source);

    byte[] readBytes(R source, int length);

    default double readDouble(R source)
    {
        long longValue = readLong(source);

        return Double.longBitsToDouble(longValue);
    }

    default Double readDoubleWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readDouble(source);
    }

    default float readFloat(R source)
    {
        int intValue = readInteger(source);

        return Float.intBitsToFloat(intValue);
    }

    default Float readFloatWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readFloat(source);
    }

    default int readInteger(R source)
    {
        byte[] bytes = readBytes(source, 4);

        // @formatter:off
       return ((bytes[0] & 0xFF) << 24)
               + ((bytes[1] & 0xFF) << 16)
               + ((bytes[2] & 0xFF) << 8)
               + (bytes[3] & 0xFF)
               ;
       // @formatter:on
    }

    default Integer readIntegerWrapper(final R source)
    {
        if (readByte(source) == 0)
        {
            return null;
        }

        return readInteger(source);
    }

    default long readLong(R source)
    {
        byte[] bytes = readBytes(source, 8);

        // @formatter:off
        return ((long) (bytes[0] & 0xFF) << 56)
                + ((long) (bytes[1] & 0xFF) << 48)
                + ((long) (bytes[2] & 0xFF) << 40)
                + ((long) (bytes[3] & 0xFF) << 32)
                + ((long) (bytes[4] & 0xFF) << 24)
                + ((long) (bytes[5] & 0xFF) << 16)
                + ((long) (bytes[6] & 0xFF) << 8)
                + ((long) bytes[7] & 0xFF)
                ;
        // @formatter:on
    }

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
