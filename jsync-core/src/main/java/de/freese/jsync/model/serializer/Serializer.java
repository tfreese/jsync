// Created: 24.09.2020
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @param <W> Type of Sink
 * @param <R> Type of Source
 *
 * @author Thomas Freese
 */
public interface Serializer<W, R> {
    <T> T readFrom(R source, Class<T> type);

    <T> void register(Class<T> type, ObjectSerializer<? super T> serializer);

    @SuppressWarnings("unchecked")
    default <T> void writeTo(W sink, T value) {
        writeTo(sink, value, (Class<T>) value.getClass());
    }

    <T> void writeTo(W sink, T value, Class<T> type);
}
