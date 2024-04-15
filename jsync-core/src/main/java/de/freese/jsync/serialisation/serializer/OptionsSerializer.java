// Created: 24.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.Options;
import de.freese.jsync.Options.Builder;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class OptionsSerializer {
    public static <R> Options read(final DataReader<R> reader, final R input) {
        // bufferSize
        // int bufferSize = reader.readInteger(input);

        // checksum
        final boolean checksum = reader.readBoolean(input);

        // delete
        final boolean delete = reader.readBoolean(input);

        // dryRun
        final boolean dryRun = reader.readBoolean(input);

        // followSymLinks
        final boolean followSymLinks = reader.readBoolean(input);

        return new Builder()
                //.bufferSize(bufferSize)
                .checksum(checksum)
                .delete(delete)
                .dryRun(dryRun)
                .followSymLinks(followSymLinks)
                .build();
    }

    public static <W> void write(final DataWriter<W> writer, final W output, final Options value) {
        // bufferSize
        // writer.writeInteger(output, value.getBufferSize());

        // checksum
        writer.writeBoolean(output, value.isChecksum());

        // delete
        writer.writeBoolean(output, value.isDelete());

        // dryRun
        writer.writeBoolean(output, value.isDryRun());

        // followSymLinks
        writer.writeBoolean(output, value.isFollowSymLinks());
    }

    private OptionsSerializer() {
        super();
    }
}
