// Created: 22.09.2020
package de.freese.jsync.model.serializer.neu.adapter;

import java.nio.charset.Charset;

/**
 * Interface für eine Datenquelle/-senke.
 *
 * @author Thomas Freese
 * @param <D> Type of Source/Sink
 */
public interface DataAdapter<D>
{
    /**
     * @param source Object
     * @return byte
     */
    public byte readByte(D source);

    /**
     * @param source Object
     * @param length int
     * @return byte[]
     */
    public byte[] readBytes(D source, int length);

    /**
     * @param source Object
     * @return int
     */
    public int readInt(D source);

    /**
     * @param source Object
     * @return long
     */
    public long readLong(D source);

    /**
     * @param source Object
     * @param length int
     * @param charset {@link Charset}
     * @return String
     */
    public default String readString(final D source, final int length, final Charset charset)
    {
        byte[] bytes = readBytes(source, length);

        return new String(bytes, charset);
    }

    /**
     * @param sink Object
     * @param value byte
     */
    public void writeByte(D sink, byte value);

    /**
     * @param sink Object
     * @param bytes byte[]
     */
    public void writeBytes(D sink, byte[] bytes);

    /**
     * @param sink Object
     * @param value int
     */
    public void writeInt(D sink, int value);

    /**
     * @param sink Object
     * @param value long
     */
    public void writeLong(D sink, long value);

    /**
     * @param sink Object
     * @param value {@link CharSequence}
     * @param charset {@link Charset}
     */
    public default void writeString(final D sink, final String value, final Charset charset)
    {
        byte[] bytes = value.getBytes(charset);

        writeInt(sink, bytes.length);
        writeBytes(sink, bytes);
    }
}
