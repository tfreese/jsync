// Created: 28.04.2020
package de.freese.jsync.model.serializer.neu;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import de.freese.jsync.Options;

/**
 * Interface zur Serialisierung.
 *
 * @param <T> Object-Type
 * @author Thomas Freese
 */
public interface ObjectSerializer<T>
{
    /**
     * @return {@link Charset}
     */
    public default Charset getCharset()
    {
        return Options.CHARSET;
    }

    /**
     * @param data {@link ByteBuffer}
     * @return T
     */
    public T readFrom(final DataAdapter data);

    /**
     * @param data {@link DataAdapter}
     * @param obj T
     */
    public void writeTo(final DataAdapter data, final T obj);
}
