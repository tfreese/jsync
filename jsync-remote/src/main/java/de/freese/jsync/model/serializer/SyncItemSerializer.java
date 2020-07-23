/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import java.nio.file.attribute.PosixFilePermissions;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 */
class SyncItemSerializer implements Serializer<SyncItem>
{
    /**
    *
    */
    private static final Serializer<SyncItem> INSTANCE = new SyncItemSerializer();

    /**
     * @return Serializer<SyncItem>
     */
    static Serializer<SyncItem> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link SyncItemSerializer} Object.
     */
    SyncItemSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public SyncItem readFrom(final ByteBuffer buffer)
    {
        // relativePath
        String relativePath = StringSerializer.getInstance().readFrom(buffer);

        SyncItem syncItem = new DefaultSyncItem(relativePath);

        // is File / Directory
        syncItem.setFile(buffer.get() == 1);

        syncItem.setSize(buffer.getLong());

        // lastModifiedTime
        long lastModifiedTime = buffer.getLong();
        syncItem.setLastModifiedTime(lastModifiedTime);

        // permissions
        if (buffer.get() == 1)
        {
            String permissions = StringSerializer.getInstance().readFrom(buffer);

            syncItem.setPermissions(PosixFilePermissions.fromString(permissions));
        }

        // group
        if (buffer.get() == 1)
        {
            Group group = GroupSerializer.getInstance().readFrom(buffer);
            syncItem.setGroup(group);
        }

        // user
        if (buffer.get() == 1)
        {
            User user = UserSerializer.getInstance().readFrom(buffer);
            syncItem.setUser(user);
        }

        // checksum
        if (buffer.get() == 1)
        {
            String checksum = StringSerializer.getInstance().readFrom(buffer);

            syncItem.setChecksum(checksum);
        }

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final SyncItem obj)
    {
        // relativePath
        byte[] bytes = obj.getRelativePath().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        // is File / Directory
        buffer.put(obj.isFile() ? (byte) 1 : (byte) 0);

        // size
        buffer.putLong(obj.getSize());

        // lastModifiedTime
        buffer.putLong(obj.getLastModifiedTime());

        // permissions
        String permissions = obj.getPermissionsToString();

        if ((permissions == null) || permissions.isBlank())
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);

            bytes = permissions.getBytes(getCharset());
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }

        // group
        if (obj.getGroup() == null)
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);
            GroupSerializer.getInstance().writeTo(buffer, obj.getGroup());
        }

        // user
        if (obj.getUser() == null)
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);
            UserSerializer.getInstance().writeTo(buffer, obj.getUser());
        }

        // checksum
        if ((obj.getChecksum() == null) || obj.getChecksum().isBlank())
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);

            bytes = obj.getChecksum().getBytes(getCharset());
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }
    }
}
