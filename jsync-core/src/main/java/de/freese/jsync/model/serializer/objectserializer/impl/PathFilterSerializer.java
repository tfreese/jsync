// Created: 15.08.2021
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.util.HashSet;
import java.util.Set;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterEndsWith;
import de.freese.jsync.filter.PathFilterNoOp;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public class PathFilterSerializer implements ObjectSerializer<PathFilter>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <W, R> PathFilter readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source)
    {
        int count = adapter.readInteger(source);

        Set<String> directoryFilters = new HashSet<>();

        for (int i = 0; i < count; i++)
        {
            directoryFilters.add(adapter.readString(source, getCharset()));
        }

        count = adapter.readInteger(source);

        Set<String> fileFilters = new HashSet<>();

        for (int i = 0; i < count; i++)
        {
            fileFilters.add(adapter.readString(source, getCharset()));
        }

        if (directoryFilters.isEmpty() && fileFilters.isEmpty())
        {
            return PathFilterNoOp.INSTANCE;
        }

        return new PathFilterEndsWith(directoryFilters, fileFilters);
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final PathFilter value)
    {
        Set<String> filters = value.getDirectoryFilter();
        adapter.writeInteger(sink, filters.size());
        filters.forEach(filter -> adapter.writeString(sink, filter, getCharset()));

        filters = value.getFileFilter();
        adapter.writeInteger(sink, filters.size());
        filters.forEach(filter -> adapter.writeString(sink, filter, getCharset()));
    }
}
