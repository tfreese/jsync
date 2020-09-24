// Created: 28.04.2020
package de.freese.jsync.model.serializer.neu.objects;

import java.nio.charset.Charset;
import de.freese.jsync.Options;
import de.freese.jsync.model.serializer.neu.adapter.DataAdapter;

/**
 * Interface zur Serialisierung eines Objektes.
 *
 * @param <T> Type of Object
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
     * @param adapter {@link DataAdapter}
     * @param source Object
     * @return Object
     */
    public <D> T readFrom(final DataAdapter<D> adapter, D source);

    /**
     * @param adapter {@link DataAdapter}
     * @param sink Object
     * @param value Object
     */
    public <D> void writeTo(final DataAdapter<D> adapter, D sink, final T value);
}
