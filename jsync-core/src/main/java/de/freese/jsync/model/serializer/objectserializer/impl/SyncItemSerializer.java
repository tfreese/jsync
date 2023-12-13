// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.ObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class SyncItemSerializer implements ObjectSerializer<SyncItem> {
    @Override
    public <W, R> SyncItem readFrom(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final R source) {
        // relativePath
        final String relativePath = adapter.readString(source, getCharset());

        final SyncItem syncItem = new DefaultSyncItem(relativePath);

        // is File / Directory
        syncItem.setFile(adapter.readBoolean(source));

        // size
        syncItem.setSize(adapter.readLong(source));

        // lastModifiedTime
        syncItem.setLastModifiedTime(adapter.readLong(source));

        // checksum
        syncItem.setChecksum(adapter.readString(source, getCharset()));

        //        // permissions
        //        String permissions = adapter.readString(source, getCharset());
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

    @Override
    public <W, R> void writeTo(final SerializerRegistry registry, final DataAdapter<W, R> adapter, final W sink, final SyncItem value) {
        // relativePath
        adapter.writeString(sink, value.getRelativePath(), getCharset());

        // is File / Directory
        adapter.writeBoolean(sink, value.isFile());

        // size
        adapter.writeLong(sink, value.getSize());

        // lastModifiedTime
        adapter.writeLong(sink, value.getLastModifiedTime());

        // checksum
        adapter.writeString(sink, value.getChecksum(), getCharset());

        //        // permissions
        //        adapter.writeString(sink, value.getPermissionsToString(), getCharset());
        //
        //        // group
        //        registry.getSerializer(Group.class).writeTo(registry, adapter, sink, value.getGroup());
        //
        //        // user
        //        registry.getSerializer(User.class).writeTo(registry, adapter, sink, value.getUser());
    }
}
