/**
 * Created: 04.11.2018
 */

package de.freese.jsync.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.slf4j.Logger;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.DefaultSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.server.JSyncCommand;
import de.freese.jsync.server.JSyncSession;

/**
 * Verarbeitet den Request und Response.<br>
 * Sync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see IoHandler
 */
public class JSyncIoHandler extends AbstractIoHandler
{
    /**
     * Erstellt ein neues {@link JSyncIoHandler} Object.
     */
    public JSyncIoHandler()
    {
        super();
    }

    /**
     * @see de.freese.jsync.server.handler.IoHandler#read(java.nio.channels.SelectionKey, org.slf4j.Logger)
     */
    @Override
    @SuppressWarnings("resource")
    public void read(final SelectionKey selectionKey, final Logger logger) throws Exception
    {
        ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

        JSyncSession session = (JSyncSession) selectionKey.attachment();

        // JSyncCommand lesen.
        ByteBuffer buffer = session != null ? session.getBuffer() : ByteBuffer.allocateDirect(256);

        buffer.clear();
        channel.read(buffer);
        buffer.flip();

        JSyncCommand command = JSyncCommandSerializer.getInstance().readFrom(buffer);

        if (command == null)
        {
            logger.error("unknown JSyncCommand");
            selectionKey.interestOps(SelectionKey.OP_READ);
            return;
        }

        switch (command)
        {
            case DISCONNECT:
                logger.debug("read disconnect");

                if (session != null)
                {
                    session.setLastCommand(null);
                }

                selectionKey.attach(null);
                selectionKey.interestOps(SelectionKey.OP_CONNECT);
                break;
            case CONNECT:
                requestConnect(selectionKey, channel, logger);
                break;

            case TARGET_CREATE_SYNC_ITEMS:
                receiverRequestCreateSyncItems(selectionKey, channel);
                break;

            case TARGET_DELETE_FILE:
                receiverRequestDeleteFile(selectionKey, channel);
                break;

            case TARGET_DELETE_DIRECTORY:
                receiverRequestDeleteDirectory(selectionKey, channel);
                break;

            case TARGET_CREATE_DIRECTORY:
                receiverRequestCreateDirectory(selectionKey, channel);
                break;

            case TARGET_WRITEABLE_FILE_CHANNEL:
                receiverRequestFileChannel(selectionKey, channel);
                break;

            case TARGET_VALIDATE_FILE:
                receiverRequestValidateFile(selectionKey, channel);
                break;

            case TARGET_UPDATE_FILE:
                receiverRequestUpdateFile(selectionKey, channel);
                break;

            case TARGET_UPDATE_DIRECTORY:
                receiverRequestUpdateDirectory(selectionKey, channel);
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                senderRequestCreateSyncItems(selectionKey, channel);
                break;

            case SOURCE_READABLE_FILE_CHANNEL:
                senderRequestFileChannel(selectionKey, channel);
                break;

            case SOURCE_CHECKSUM:
                senderRequestChecksum(selectionKey, channel);
                break;

            case TARGET_CHECKSUM:
                receiverRequestChecksum(selectionKey, channel);
                break;

            default:
                break;
        }
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestChecksum(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Checksum");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String relativePath = new String(bytes, getCharset());

        Receiver receiver = session.getReceiver();
        String checksum = receiver.getChecksum(relativePath, i -> {
        });

        session.setChecksum(checksum);

        buffer.clear();
        session.setLastCommand(JSyncCommand.TARGET_CHECKSUM);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestCreateDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Create Directory");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String directory = new String(bytes, getCharset());

        session.getReceiver().createDirectory(directory);
        session.setLastCommand(JSyncCommand.TARGET_CREATE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestCreateSyncItems(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String basePath = new String(bytes, getCharset());

        boolean followSymLinks = buffer.get() == 1;
        session.setFollowSymLinks(followSymLinks);

        Path base = Paths.get(basePath);

        Receiver receiver = new LocalhostReceiver(base.toUri());
        session.setReceiver(receiver);

        buffer.clear();
        session.setLastCommand(JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestDeleteDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Delete Directory");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String directory = new String(bytes, getCharset());

        session.getReceiver().deleteDirectory(directory);
        session.setLastCommand(JSyncCommand.TARGET_DELETE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestDeleteFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Delete File");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String file = new String(bytes, getCharset());

        session.getReceiver().deleteFile(file);
        session.setLastCommand(JSyncCommand.TARGET_DELETE_FILE);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: FileChannel");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String fileName = new String(bytes, getCharset());

        SyncItem syncItem = new DefaultSyncItem(fileName);
        syncItem.setFile(true);
        syncItem.setSize(fileSize);

        // if (buffer.get() == 1)
        // {
        // bytes = new byte[buffer.getInt()];
        // buffer.get(bytes);
        //
        // String checksum = new String(bytes, getCharset());
        // syncItem.setChecksum(checksum);
        // }

        Receiver receiver = session.getReceiver();

        long fileBytesTransferred = 0;

        // BiConsumer<Long, Long> monitor = (written, gesamt) -> {
        // String msg = String.format("Writen data for %s: %s = %6.2f %%", fileName, JSyncUtils.toHumanReadableSize(written),
        // JSyncUtils.getPercent(written, gesamt));
        // getLogger().debug(msg);
        // };

        // MessageDigest messageDigest = DigestUtils.createSha256Digest();
        // DigestUtils.digest(messageDigest, buffer);

        try (WritableByteChannel outChannel = receiver.getChannel(syncItem))
        // try (WritableByteChannel outChannel = new MonitoringWritableByteChannel(receiver.getChannel(syncItem), monitor, fileSize))
        {
            // Restlichen Buffer in die Datei schreiben.
            fileBytesTransferred += outChannel.write(buffer);
            buffer.clear();

            // while ((channel.read(buffer) > 0) || (fileBytesTransferred < fileSize))
            while (fileBytesTransferred < fileSize)
            {
                channel.read(buffer);
                buffer.flip();

                // DigestUtils.digest(messageDigest, buffer);

                while (buffer.hasRemaining())
                {
                    fileBytesTransferred += outChannel.write(buffer);
                }

                buffer.clear();
            }

            if (outChannel instanceof FileChannel)
            {
                ((FileChannel) outChannel).force(true);
            }
        }

        // Funktioniert, ist aber falsche Stelle.
        // String checksum = DigestUtils.digestAsHex(messageDigest);
        // session.setLastCecksum(checksum);

        session.setLastCommand(JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestUpdateDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Update Directory");

        ByteBuffer buffer = session.getBuffer();

        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);

        Receiver receiver = session.getReceiver();
        receiver.updateDirectory(syncItem);

        session.setLastCommand(JSyncCommand.TARGET_UPDATE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestUpdateFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Update File");

        ByteBuffer buffer = session.getBuffer();

        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);

        Receiver receiver = session.getReceiver();
        receiver.updateFile(syncItem);

        session.setLastCommand(JSyncCommand.TARGET_UPDATE_FILE);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverRequestValidateFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Validate File");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        boolean withChecksum = buffer.get() == 1;

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String fileName = new String(bytes, getCharset());

        SyncItem syncItem = new DefaultSyncItem(fileName);
        syncItem.setFile(true);
        syncItem.setSize(fileSize);

        if (buffer.get() == 1)
        {
            bytes = new byte[buffer.getInt()];
            buffer.get(bytes);

            String checksum = new String(bytes, getCharset());
            syncItem.setChecksum(checksum);
        }

        Receiver receiver = session.getReceiver();

        try
        {
            receiver.validateFile(syncItem, withChecksum);
        }
        catch (Exception ex)
        {
            session.getLogger().error(null, ex);
        }

        session.setLastCommand(JSyncCommand.TARGET_VALIDATE_FILE);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverResponseChecksum(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver response: Checksum");

        ByteBuffer buffer = session.getBuffer();

        String checksum = session.getChecksum();
        byte[] bytes = checksum.getBytes(getCharset());

        buffer.clear();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        channel.write(buffer);

        session.getLogger().debug("receiver response: Checksum written");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void receiverResponseCreateSyncItems(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver response: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        Receiver receiver = session.getReceiver();
        List<SyncItem> syncItems = receiver.getSyncItems(session.isFollowSymLinks());

        // Anzahl
        buffer.clear();
        buffer.putInt(syncItems.size());
        buffer.flip();
        channel.write(buffer);

        syncItems.forEach(syncItem -> {
            buffer.clear();

            Serializers.writeTo(buffer, syncItem);

            buffer.flip();

            try
            {
                while (buffer.hasRemaining())
                {
                    channel.write(buffer);
                }
            }
            catch (IOException ioex)
            {
                session.getLogger().error(null, ioex);
            }
        });

        session.getLogger().debug("receiver response: Create SyncItems written");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @param logger {@link Logger}
     * @throws Exception Falls was schief geht.
     */
    protected void requestConnect(final SelectionKey selectionKey, final ReadableByteChannel channel, final Logger logger) throws Exception
    {
        logger.debug("request: Connect");

        JSyncSession session = new JSyncSession(logger);

        selectionKey.attach(session);

        session.setLastCommand(JSyncCommand.CONNECT);
        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderRequestChecksum(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender request: Checksum");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String relativePath = new String(bytes, getCharset());

        Sender sender = session.getSender();
        String checksum = sender.getChecksum(relativePath, i -> {
        });

        session.setChecksum(checksum);

        buffer.clear();
        session.setLastCommand(JSyncCommand.SOURCE_CHECKSUM);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderRequestCreateSyncItems(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender request: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String basePath = new String(bytes, getCharset());

        boolean followSymLinks = buffer.get() == 1;
        session.setFollowSymLinks(followSymLinks);

        Path base = Paths.get(basePath);

        Sender source = new LocalhostSender(base.toUri());
        session.setSender(source);

        buffer.clear();
        session.setLastCommand(JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender request: FileChannel");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String fileName = new String(bytes, getCharset());

        SyncItem syncItem = new DefaultSyncItem(fileName);
        syncItem.setFile(true);
        syncItem.setSize(fileSize);

        // if (buffer.get() == 1)
        // {
        // bytes = new byte[buffer.getInt()];
        // buffer.get(bytes);
        //
        // String checksum = new String(bytes, getCharset());
        // syncItem.setChecksum(checksum);
        // }

        session.setSyncItem(syncItem);

        session.setLastCommand(JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderResponseChecksum(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender response: Checksum");

        ByteBuffer buffer = session.getBuffer();

        String checksum = session.getChecksum();
        byte[] bytes = checksum.getBytes(getCharset());

        buffer.clear();
        buffer.putInt(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        channel.write(buffer);

        session.getLogger().debug("sender response: Checksum written");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderResponseCreateSyncItems(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender response: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        Sender sender = session.getSender();
        List<SyncItem> syncItems = sender.getSyncItems(session.isFollowSymLinks());

        // Anzahl
        buffer.clear();
        buffer.putInt(syncItems.size());
        buffer.flip();
        channel.write(buffer);

        syncItems.forEach(syncItem -> {
            buffer.clear();

            Serializers.writeTo(buffer, syncItem);

            buffer.flip();

            try
            {
                while (buffer.hasRemaining())
                {
                    channel.write(buffer);
                }
            }
            catch (IOException ioex)
            {
                session.getLogger().error(null, ioex);
            }
        });

        session.getLogger().debug("sender response: Create SyncItems written");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void senderResponseFileChannel(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender response: FileChannel");

        ByteBuffer buffer = session.getBuffer();
        Sender sender = session.getSender();
        SyncItem syncItem = session.getSyncItem();

        long fileSize = syncItem.getSize();
        long fileBytesTransferred = 0;

        // BiConsumer<Long, Long> monitor = (read, gesamt) -> {
        // String msg = String.format("read data for %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(read),
        // JSyncUtils.getPercent(read, gesamt));
        // getLogger().debug(msg);
        // };

        // MessageDigest messageDigest = DigestUtils.createSha256Digest();
        // DigestUtils.digest(messageDigest, buffer);

        buffer.clear();

        try (ReadableByteChannel inChannel = sender.getChannel(syncItem))
        // try (ReadableByteChannel inChannel = new MonitoringReadableByteChannel(sender.getChannel(syncItem), monitor, fileSize))
        {
            // while ((inChannel.read(buffer) > 0) || (fileBytesTransferred < fileSize))
            while (fileBytesTransferred < fileSize)
            {
                inChannel.read(buffer);
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    fileBytesTransferred += channel.write(buffer);
                }

                buffer.clear();
            }
        }
        finally
        {
            session.setSyncItem(null);
        }
    }

    /**
     * @see de.freese.jsync.server.handler.IoHandler#write(java.nio.channels.SelectionKey, org.slf4j.Logger)
     */
    @Override
    @SuppressWarnings("resource")
    public void write(final SelectionKey selectionKey, final Logger logger) throws Exception
    {
        WritableByteChannel channel = (WritableByteChannel) selectionKey.channel();
        JSyncSession session = (JSyncSession) selectionKey.attachment();
        ByteBuffer buffer = session.getBuffer();

        switch (session.getLastCommand())
        {
            // case CONNECT:
            // writeFinishFlag(channel, buffer);
            // break;

            case TARGET_CREATE_SYNC_ITEMS:
                receiverResponseCreateSyncItems(selectionKey, channel);
                // writeFinishFlag(channel, buffer);
                break;

            case TARGET_DELETE_FILE:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_DELETE_DIRECTORY:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_CREATE_DIRECTORY:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_WRITEABLE_FILE_CHANNEL:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_VALIDATE_FILE:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_UPDATE_FILE:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_UPDATE_DIRECTORY:
                writeFinishFlag(channel, buffer);
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                senderResponseCreateSyncItems(selectionKey, channel);
                // writeFinishFlag(channel, buffer);
                break;

            case SOURCE_READABLE_FILE_CHANNEL:
                senderResponseFileChannel(selectionKey, channel);
                break;

            case SOURCE_CHECKSUM:
                senderResponseChecksum(selectionKey, channel);
                break;

            case TARGET_CHECKSUM:
                receiverResponseChecksum(selectionKey, channel);
                break;

            default:
                break;
        }

        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    /**
     * @param channel {@link WritableByteChannel}
     * @param buffer {@link ByteBuffer}
     * @throws IOException Falls was schief geht.
     */
    private void writeFinishFlag(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException
    {
        buffer.clear();
        buffer.put(Byte.MIN_VALUE);
        buffer.flip();
        // channel.write(buffer);
    }
}
