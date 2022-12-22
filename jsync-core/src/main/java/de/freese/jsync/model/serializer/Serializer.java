// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @param <W> Type of Sink
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public interface Serializer<W, R>
{
    <T> T readFrom(final R source, final Class<T> type);

    <T> void register(final Class<T> type, final ObjectSerializer<? super T> serializer);

    @SuppressWarnings("unchecked")
    default <T> void writeTo(final W sink, final T value)
    {
        writeTo(sink, value, (Class<T>) value.getClass());
    }

    <T> void writeTo(final W sink, final T value, final Class<T> type);
}
