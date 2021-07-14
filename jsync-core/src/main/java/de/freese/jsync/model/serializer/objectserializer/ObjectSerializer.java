// Created: 28.04.2020
package de.freese.jsync.model.serializer.objectserializer;

import java.nio.charset.Charset;

import de.freese.jsync.Options;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * Interface zur Serialisierung eines Objektes.
 *
 * @param <T> Type of Object
 *
 * @author Thomas Freese
 */
public interface ObjectSerializer<T>
{
    /**
     * @return {@link Charset}
     */
    default Charset getCharset()
    {
        return Options.CHARSET;
    }

    /**
     * @param registry {@link SerializerRegistry}
     * @param adapter {@link DataAdapter}
     * @param source Object
     *
     * @return Object
     */
    <D> T readFrom(SerializerRegistry registry, final DataAdapter<D> adapter, D source);

    /**
     * @param registry {@link SerializerRegistry}
     * @param adapter {@link DataAdapter}
     * @param sink Object
     * @param value Object
     */
    <D> void writeTo(SerializerRegistry registry, final DataAdapter<D> adapter, D sink, final T value);
}
