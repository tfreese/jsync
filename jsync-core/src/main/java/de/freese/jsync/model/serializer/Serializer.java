// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objects.ObjectSerializer;

/**
 * Interface für die API.
 *
 * @author Thomas Freese
 * @param <D> Type of Source/Sink
 */
public interface Serializer<D>
{
    /**
     * @param source Object
     * @param type Class
     * @return Object
     */
    public <T> T readFrom(final D source, final Class<T> type);

    /**
     * @param type Class
     * @param serializer {@link ObjectSerializer}
     */
    public <T> void register(final Class<T> type, final ObjectSerializer<? super T> serializer);

    /**
     * @param <T> Type
     * @param sink Object
     * @param value Object
     */
    @SuppressWarnings("unchecked")
    public default <T> void writeTo(final D sink, final T value)
    {
        writeTo(sink, value, (Class<T>) value.getClass());
    }

    /**
     * @param <T> Type
     * @param sink Object
     * @param value Object
     * @param type Class
     */
    public <T> void writeTo(final D sink, final T value, final Class<T> type);
}
