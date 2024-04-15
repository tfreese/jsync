// Created: 24.09.2020
package de.freese.jsync.serialisation.serializer;

import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.serialisation.io.DataReader;
import de.freese.jsync.serialisation.io.DataWriter;

/**
 * @author Thomas Freese
 */
public final class SyncItemSerializer {
    public static <R> SyncItem read(final DataReader<R> reader, final R input) {
        // relativePath
        final String relativePath = reader.readString(input);

        final SyncItem syncItem = new DefaultSyncItem(relativePath);

        // is File / Directory
        syncItem.setFile(reader.readBoolean(input));

        // size
        syncItem.setSize(reader.readLong(input));

        // lastModifiedTime
        syncItem.setLastModifiedTime(reader.readLong(input));

        // checksum
        syncItem.setChecksum(reader.readString(input));

        //        // permissions
        //        String permissions = reader.readString(source, getCharset());
        //
        //        if (permissions != null)
        //        {
        //            syncItem.setPermissions(PosixFilePermissions.fromString(permissions));
        //        }
        //
        //        // group
        //        syncItem.setGroup(registry.getSerializer(Group.class).readFrom(registry, adapter, source));
        //
        //        // user
        //        syncItem.setUser(registry.getSerializer(User.class).readFrom(registry, adapter, source));

        return syncItem;
    }

    public static <W> void write(final DataWriter<W> writer, final W output, final SyncItem value) {
        // relativePath
        writer.writeString(output, value.getRelativePath());

        // is File / Directory
        writer.writeBoolean(output, value.isFile());

        // size
        writer.writeLong(output, value.getSize());

        // lastModifiedTime
        writer.writeLong(output, value.getLastModifiedTime());

        // checksum
        writer.writeString(output, value.getChecksum());

        //        // permissions
        //        writer.writeString(sink, value.getPermissionsToString(), getCharset());
        //
        //        // group
        //        registry.getSerializer(Group.class).writeTo(registry, writer, sink, value.getGroup());
        //
        //        // user
        //        registry.getSerializer(User.class).writeTo(registry, writer, sink, value.getUser());
    }

    private SyncItemSerializer() {
        super();
    }
}
