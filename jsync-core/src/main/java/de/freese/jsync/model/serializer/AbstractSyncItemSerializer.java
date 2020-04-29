/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import java.nio.file.attribute.PosixFilePermissions;
import de.freese.jsync.model.AbstractSyncItem;
import de.freese.jsync.model.Group;
import de.freese.jsync.model.User;

/**
 * @author Thomas Freese
 * @param <T> Entity-Type
 */
abstract class AbstractSyncItemSerializer<T extends AbstractSyncItem> implements Serializer<T>
{
    /**
     * Erstellt ein neues {@link AbstractSyncItemSerializer} Object.
     */
    protected AbstractSyncItemSerializer()
    {
        super();
    }

    /**
     * @param relativePath String
     * @return {@link AbstractSyncItem}
     */
    protected abstract T createSyncItem(String relativePath);

    /**
     * @see de.freese.jsync.model.serializer.Serializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public T readFrom(final ByteBuffer buffer)
    {
        // relativePath
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String relativePath = new String(bytes, getCharset());

        T syncItem = createSyncItem(relativePath);

        // lastModifiedTime
        long lastModifiedTime = buffer.getLong();
        syncItem.setLastModifiedTime(lastModifiedTime);

        // permissions
        if (buffer.get() == 1)
        {
            bytes = new byte[buffer.getInt()];
            buffer.get(bytes);

            String permissions = new String(bytes, getCharset());

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

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.Serializer#writeTo(java.nio.ByteBuffer, java.lang.Object)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final T obj)
    {
        // relativePath
        byte[] bytes = obj.getRelativePath().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

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
    }
}
