// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class OptionsSerializer implements ObjectSerializer<Options>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> Options readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        // bufferSize
        // int bufferSize = adapter.readInteger(source);

        // checksum
        boolean checksum = adapter.readBoolean(source);

        // delete
        boolean delete = adapter.readBoolean(source);

        // dryRun
        boolean dryRun = adapter.readBoolean(source);

        // followSymLinks
        boolean followSymLinks = adapter.readBoolean(source);

        // @formatter:off
        return new Builder()
                //.bufferSize(bufferSize)
                .checksum(checksum)
                .delete(delete)
                .dryRun(dryRun)
                .followSymLinks(followSymLinks)
                .build()
                ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     * de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final Options value)
    {
        // bufferSize
        // adapter.writeInteger(sink, value.getBufferSize());

        // checksum
        adapter.writeBoolean(sink, value.isChecksum());

        // delete
        adapter.writeBoolean(sink, value.isDelete());

        // dryRun
        adapter.writeBoolean(sink, value.isDryRun());

        // followSymLinks
        adapter.writeBoolean(sink, value.isFollowSymLinks());
    }
}
