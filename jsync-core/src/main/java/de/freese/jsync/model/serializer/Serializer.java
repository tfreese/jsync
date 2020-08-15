/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import de.freese.jsync.Options;

/**
 * Interface zur Serialisierung.
 *
 * @author Thomas Freese
 * @param <T> Type
 */
public interface Serializer<T>
{
    /**
     * @return {@link Charset}
     */
    public default Charset getCharset()
    {
        return Options.CHARSET;
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
