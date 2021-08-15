// Created: 15.08.2021
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.util.HashSet;
import java.util.Set;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;

/**
 * @author Thomas Freese
 */
public class PathFilterSerializer extends AbstractObjectSerializer<PathFilter>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> PathFilter readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        int count = adapter.readInt(source);

        Set<String> directoryFilters = new HashSet<>();

        for (int i = 0; i < count; i++)
        {
            directoryFilters.add(readString(adapter, source, getCharset()));
        }

        count = adapter.readInt(source);

        Set<String> fileFilters = new HashSet<>();

        for (int i = 0; i < count; i++)
        {
            fileFilters.add(readString(adapter, source, getCharset()));
        }

        return new PathFilterEndsWith(directoryFilters, fileFilters);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final PathFilter value)
    {
        Set<String> filters = value.getDirectoryFilter();
        adapter.writeInt(sink, filters.size());
        filters.forEach(filter -> writeString(adapter, sink, filter, getCharset()));

        filters = value.getFileFilter();
        adapter.writeInt(sink, filters.size());
        filters.forEach(filter -> writeString(adapter, sink, filter, getCharset()));
    }
}
