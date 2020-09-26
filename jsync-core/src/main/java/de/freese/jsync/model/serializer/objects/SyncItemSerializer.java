// Created: 24.09.2020
package de.freese.jsync.model.serializer.objects;

import java.nio.file.attribute.PosixFilePermissions;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.adapter.DataAdapter;

/**
 * @author Thomas Freese
 */
public final class SyncItemSerializer implements ObjectSerializer<SyncItem>
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class SyncItemSerializerHolder
    {
        /**
         *
         */
        private static final SyncItemSerializer INSTANCE = new SyncItemSerializer();

        /**
         * Erstellt ein neues {@link SyncItemSerializerHolder} Object.
         */
        private SyncItemSerializerHolder()
        {
            super();
        }
    }

    /**
     * @return {@link SyncItemSerializer}
     */
    public static SyncItemSerializer getInstance()
    {
        return SyncItemSerializerHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link SyncItemSerializer} Object.
     */
    private SyncItemSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#readFrom(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object)
     */
    @Override
    public <D> SyncItem readFrom(final DataAdapter<D> adapter, final D source)
    {
        // relativePath
        String relativePath = StringSerializer.getInstance().readFrom(adapter, source);

        SyncItem syncItem = new DefaultSyncItem(relativePath);

        // is File / Directory
        syncItem.setFile(BooleanSerializer.getInstance().readFrom(adapter, source));

        // size
        syncItem.setSize(adapter.readLong(source));

        // lastModifiedTime
        syncItem.setLastModifiedTime(adapter.readLong(source));

        // permissions
        String permissions = StringSerializer.getInstance().readFrom(adapter, source);

        if (permissions != null)
        {
            syncItem.setPermissions(PosixFilePermissions.fromString(permissions));
        }

        // group
        syncItem.setGroup(GroupSerializer.getInstance().readFrom(adapter, source));

        // user
        syncItem.setUser(UserSerializer.getInstance().readFrom(adapter, source));

        // checksum
        syncItem.setChecksum(StringSerializer.getInstance().readFrom(adapter, source));

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.objects.ObjectSerializer#writeTo(de.freese.jsync.model.serializer.adapter.DataAdapter, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public <D> void writeTo(final DataAdapter<D> adapter, final D sink, final SyncItem value)
    {
        // relativePath
        StringSerializer.getInstance().writeTo(adapter, sink, value.getRelativePath());

        // is File / Directory
        BooleanSerializer.getInstance().writeTo(adapter, sink, value.isFile());

        // size
        adapter.writeLong(sink, value.getSize());

        // lastModifiedTime
        adapter.writeLong(sink, value.getLastModifiedTime());

        // permissions
        StringSerializer.getInstance().writeTo(adapter, sink, value.getPermissionsToString());

        // group
        GroupSerializer.getInstance().writeTo(adapter, sink, value.getGroup());

        // user
        UserSerializer.getInstance().writeTo(adapter, sink, value.getUser());

        // checksum
        StringSerializer.getInstance().writeTo(adapter, sink, value.getChecksum());
    }
}
