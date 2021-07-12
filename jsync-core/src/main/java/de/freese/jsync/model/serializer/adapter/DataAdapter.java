// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

import java.nio.charset.Charset;

/**
 * Interface für eine Datenquelle/-senke.<br>
 *
 * @author Thomas Freese
 *
 * @param <D> Type of Source/Sink
 *
 * @see "org.springframework.core.io.buffer.DataBuffer"
 */
public interface DataAdapter<D>
{
    /**
     * @param source Object
     *
     * @return boolean
     */
    default boolean readBoolean(final D source)
    {
        return readInt(source) == 1;
    }

    /**
     * @param source Object
     *
     * @return byte
     */
    byte readByte(D source);

    /**
     * @param source Object
     * @param length int
     *
     * @return byte[]
     */
    byte[] readBytes(D source, int length);

    /**
     * @param source Object
     *
     * @return int
     */
    int readInt(D source);

    /**
     * @param source Object
     *
     * @return long
     */
    long readLong(D source);

    /**
     * @param source Object
     * @param charset {@link Charset}
     *
     * @return String
     */
    default String readString(final D source, final Charset charset)
    {
        int length = readInt(source);

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

    /**
     * @param sink Object
     * @param value boolean
     */
    default void writeBoolean(final D sink, final boolean value)
    {
        writeInt(sink, value ? 1 : 0);
    }

    /**
     * @param sink Object
     * @param value byte
     */
    void writeByte(D sink, byte value);

    /**
     * @param sink Object
     * @param bytes byte[]
     */
    void writeBytes(D sink, byte[] bytes);

    /**
     * @param sink Object
     * @param value int
     */
    void writeInt(D sink, int value);

    /**
     * @param sink Object
     * @param value long
     */
    void writeLong(D sink, long value);

    /**
     * @param sink Object
     * @param value {@link CharSequence}
     * @param charset {@link Charset}
     */
    default void writeString(final D sink, final String value, final Charset charset)
    {
        if (value == null)
        {
            writeInt(sink, -1);
            return;
        }
        else if (value.isBlank())
        {
            writeInt(sink, 0);
            return;
        }

        byte[] bytes = value.getBytes(charset);

        writeInt(sink, bytes.length);
        writeBytes(sink, bytes);
    }
}
