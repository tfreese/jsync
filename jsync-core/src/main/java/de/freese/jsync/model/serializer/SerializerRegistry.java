// Created: 14.07.2021
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface SerializerRegistry
{
    /**
     * @param <T> Entity-Type
     * @param clazz Class
     *
     * @return {@link ObjectSerializer}
     */
    <T> ObjectSerializer<T> getSerializer(final Class<T> clazz);
}
