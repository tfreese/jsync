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
public final class OptionsSerializer implements ObjectSerializer<Options> {
    @Override
    public <W, R> Options readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        // bufferSize
        // int bufferSize = adapter.readInteger(source);

        // checksum
        final boolean checksum = adapter.readBoolean(source);

        // delete
        final boolean delete = adapter.readBoolean(source);

        // dryRun
        final boolean dryRun = adapter.readBoolean(source);

        // followSymLinks
        final boolean followSymLinks = adapter.readBoolean(source);

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

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final Options value) {
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
