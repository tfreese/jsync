// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * Interface für die API.
 *
 * @param <W> Type of Sink
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public interface Serializer<W, R>
{
    /**
     * @param source Object
     * @param type Class
     *
     * @return Object
     */
    <T> T readFrom(final R source, final Class<T> type);

    /**
     * @param type Class
     * @param serializer {@link ObjectSerializer}
     */
    <T> void register(final Class<T> type, final ObjectSerializer<? super T> serializer);

    /**
     * @param <T> Type
     * @param sink Object
     * @param value Object
     */
    @SuppressWarnings("unchecked")
    default <T> void writeTo(final W sink, final T value)
    {
        writeTo(sink, value, (Class<T>) value.getClass());
    }

    /**
     * @param <T> Type
     * @param sink Object
     * @param value Object
     * @param type Class
     */
    <T> void writeTo(final W sink, final T value, final Class<T> type);
}
