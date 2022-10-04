// Created: 14.07.2021
package de.freese.jsync.model.serializer;

import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface SerializerRegistry
{
    <T> ObjectSerializer<T> getSerializer(final Class<T> clazz);
}
