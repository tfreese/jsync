/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Interface zur Serialisierung.
 *
 * @author Thomas Freese
 * @param <T> Entity-Type
 */
public interface Serializer<T>
{
    /**
     * @return {@link Charset}
     */
    public default Charset getCharset()
    {
        return StandardCharsets.UTF_8;
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @return T
     */
    public T readFrom(final ByteBuffer buffer);

    /**
     * @param buffer {@link ByteBuffer}
     * @param obj T
     */
    public void writeTo(final ByteBuffer buffer, final T obj);
}
