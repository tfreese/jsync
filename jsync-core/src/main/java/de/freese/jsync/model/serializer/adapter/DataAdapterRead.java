// Created: 22.09.2020
package de.freese.jsync.model.serializer.adapter;

/**
 * Interface für eine Datenquelle.<br>
 *
 * @author Thomas Freese
 *
 * @param <R> Type of Source
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

    /**
     * @param source Object
     *
     * @return float
     */
    float readFloat(R source);

    /**
     * @param source Object
     *
     * @return int
     */
    int readInt(R source);

    /**
     * @param source Object
     *
     * @return long
     */
    long readLong(R source);
}
