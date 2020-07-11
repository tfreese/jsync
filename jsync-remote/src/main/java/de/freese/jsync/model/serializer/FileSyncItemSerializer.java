/**
 * Created: 28.04.2020
 */

package de.freese.jsync.model.serializer;

import java.nio.ByteBuffer;
import de.freese.jsync.model.FileSyncItem;

/**
 * @author Thomas Freese
 */
class FileSyncItemSerializer extends AbstractSyncItemSerializer<FileSyncItem>
{
    /**
    *
    */
    private static final Serializer<FileSyncItem> INSTANCE = new FileSyncItemSerializer();

    /**
     * @return Serializer<FileSyncItem>
     */
    static Serializer<FileSyncItem> getInstance()
    {
        return INSTANCE;
    }

    /**
     * Erstellt ein neues {@link FileSyncItemSerializer} Object.
     */
    FileSyncItemSerializer()
    {
        super();
    }

    /**
     * @see de.freese.jsync.model.serializer.AbstractSyncItemSerializer#createSyncItem(java.lang.String)
     */
    @Override
    protected FileSyncItem createSyncItem(final String relativePath)
    {
        FileSyncItem syncItem = new FileSyncItem(relativePath);

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.AbstractSyncItemSerializer#readFrom(java.nio.ByteBuffer)
     */
    @Override
    public FileSyncItem readFrom(final ByteBuffer buffer)
    {
        FileSyncItem syncItem = super.readFrom(buffer);

        // checksum
        if (buffer.get() == 1)
        {
            byte[] bytes = new byte[buffer.getInt()];
            buffer.get(bytes);
            String checksum = new String(bytes, getCharset());
            syncItem.setChecksum(checksum);
        }

        // size
        long size = buffer.getLong();
        syncItem.setSize(size);

        return syncItem;
    }

    /**
     * @see de.freese.jsync.model.serializer.AbstractSyncItemSerializer#writeTo(java.nio.ByteBuffer, de.freese.jsync.model.AbstractSyncItem)
     */
    @Override
    public void writeTo(final ByteBuffer buffer, final FileSyncItem obj)
    {
        super.writeTo(buffer, obj);

        // checksum
        if ((obj.getChecksum() == null) || obj.getChecksum().isBlank())
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);
            byte[] bytes = obj.getChecksum().getBytes(getCharset());
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }

        // size
        buffer.putLong(obj.getSize());
    }
}
