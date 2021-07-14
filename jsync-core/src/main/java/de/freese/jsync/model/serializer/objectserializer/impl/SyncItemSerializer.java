// Created: 24.09.2020
package de.freese.jsync.model.serializer.objectserializer.impl;

import java.nio.file.attribute.PosixFilePermissions;

import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;
import de.freese.jsync.model.serializer.SerializerRegistry;
import de.freese.jsync.model.serializer.adapter.DataAdapter;
import de.freese.jsync.model.serializer.objectserializer.AbstractObjectSerializer;

/**
 * @author Thomas Freese
 */
public final class SyncItemSerializer extends AbstractObjectSerializer<SyncItem>
{
    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> SyncItem readFrom(final SerializerRegistry registry, final DataAdapter<D> adapter, final D source)
    {
        // relativePath
        String relativePath = readString(adapter, source, getCharset());

        SyncItem syncItem = new DefaultSyncItem(relativePath);

        // is File / Directory
        syncItem.setFile(adapter.readBoolean(source));

        // size
        syncItem.setSize(adapter.readLong(source));

        // lastModifiedTime
        syncItem.setLastModifiedTime(adapter.readLong(source));

        // permissions
        String permissions = readString(adapter, source, getCharset());

        if (permissions != null)
        {
            syncItem.setPermissions(PosixFilePermissions.fromString(permissions));
        }

        // group
        syncItem.setGroup(registry.getSerializer(Group.class).readFrom(registry, adapter, source));

        // user
        syncItem.setUser(registry.getSerializer(User.class).readFrom(registry, adapter, source));

        // checksum
        syncItem.setChecksum(readString(adapter, source, getCharset()));

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.objectserializer.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.SerializerRegistry,
     *      de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object, java.lang.Object)
     */
    @Override
    public <D> void writeTo(final SerializerRegistry registry, final DataAdapter<D> adapter, final D sink, final SyncItem value)
    {
        // relativePath
        writeString(adapter, sink, value.getRelativePath(), getCharset());

        // is File / Directory
        adapter.writeBoolean(sink, value.isFile());

        // size
        adapter.writeLong(sink, value.getSize());

        // lastModifiedTime
        adapter.writeLong(sink, value.getLastModifiedTime());

        // permissions
        writeString(adapter, sink, value.getPermissionsToString(), getCharset());

        // group
        registry.getSerializer(Group.class).writeTo(registry, adapter, sink, value.getGroup());

        // user
        registry.getSerializer(User.class).writeTo(registry, adapter, sink, value.getUser());

        // checksum
        writeString(adapter, sink, value.getChecksum(), getCharset());
    }
}
