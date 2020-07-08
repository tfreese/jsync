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
import org.slf4j.Logger;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.sink.LocalhostSink;
import de.freese.jsync.filesystem.sink.Sink;
import de.freese.jsync.filesystem.source.LocalhostSource;
import de.freese.jsync.filesystem.source.Source;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
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
        ByteBuffer buffer = session != null ? session.getBuffer() : ByteBuffer.allocateDirect(1);

        buffer.clear();
        channel.read(buffer);
        buffer.flip();

        if (!buffer.hasRemaining())
        {
            // Disconnect.
            logger.debug("read disconnect");

            if (session != null)
            {
                session.setLastCommand(null);
            }

            selectionKey.interestOps(SelectionKey.OP_CONNECT);
            return;
        }

        JSyncCommand command = JSyncCommandSerializer.getInstance().readFrom(buffer);

        if (command == null)
        {
            logger.error("unknown JSyncCommand");
            return;
        }

        switch (command)
        {
            case CONNECT:
                requestConnect(selectionKey, channel, logger);
                break;

            case TARGET_CREATE_SYNC_ITEMS:
                targetRequestCreateSyncItems(selectionKey, channel);
                break;

            case TARGET_DELETE_FILE:
                targetRequestDeleteFile(selectionKey, channel);
                break;

            case TARGET_DELETE_DIRECTORY:
                targetRequestDeleteDirectory(selectionKey, channel);
                break;

            case TARGET_CREATE_DIRECTORY:
                targetRequestCreateDirectory(selectionKey, channel);
                break;

            case TARGET_WRITEABLE_FILE_CHANNEL:
                targetRequestFileChannel(selectionKey, channel);
                break;

            case TARGET_VALIDATE_FILE:
                targetRequestValidateFile(selectionKey, channel);
                break;

            case TARGET_UPDATE_FILE:
                targetRequestUpdateFile(selectionKey, channel);
                break;

            case TARGET_UPDATE_DIRECTORY:
                targetRequestUpdateDirectory(selectionKey, channel);
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                sourceRequestCreateSyncItems(selectionKey, channel);
                break;

            case SOURCE_READABLE_FILE_CHANNEL:
                sourceRequestFileChannel(selectionKey, channel);
                break;

            default:
                break;
        }
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

        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        channel.read(buffer);
        buffer.flip();

        Options options = Serializers.readFrom(buffer, Options.class);
        JSyncSession session = new JSyncSession(options, logger);
        session.setLastCommand(JSyncCommand.CONNECT);

        selectionKey.attach(session);

        buffer.clear();
        session.setLastCommand(JSyncCommand.CONNECT);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void sourceRequestCreateSyncItems(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender request: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String basePath = new String(bytes, getCharset());

        Path base = Paths.get(basePath);

        Source source = new LocalhostSource(session.getOptions(), base.toUri());
        session.setSource(source);

        buffer.clear();
        session.setLastCommand(JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void sourceRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender request: FileChannel");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String fileName = new String(bytes, getCharset());

        FileSyncItem syncItem = new FileSyncItem(fileName);
        syncItem.setSize(fileSize);

        // if (buffer.get() == 1)
        // {
        // bytes = new byte[buffer.getInt()];
        // buffer.get(bytes);
        //
        // String checksum = new String(bytes, getCharset());
        // syncItem.setChecksum(checksum);
        // }

        session.setFileSyncItem(syncItem);

        session.setLastCommand(JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void sourceResponseCreateSyncItems(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender response: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        Source source = session.getSource();
        source.createSyncItems(new GeneratorListener()
        {
            /**
             * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
             */
            @Override
            public void pathCount(final Path path, final int pathCount)
            {
                buffer.clear();
                buffer.putInt(pathCount);
                buffer.flip();

                try
                {
                    channel.write(buffer);
                }
                catch (IOException ioex)
                {
                    session.getLogger().error(null, ioex);
                }
            }

            /**
             * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
             */
            @Override
            public void processingSyncItem(final SyncItem syncItem)
            {
                buffer.clear();

                if (syncItem instanceof FileSyncItem)
                {
                    buffer.put((byte) 0);
                    Serializers.writeTo(buffer, (FileSyncItem) syncItem);
                }
                else
                {
                    buffer.put((byte) 1);
                    Serializers.writeTo(buffer, (DirectorySyncItem) syncItem);
                }

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
            }
        });

        session.getLogger().debug("sender response: Create SyncItems written");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link WritableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void sourceResponseFileChannel(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("sender response: FileChannel");

        ByteBuffer buffer = session.getBuffer();
        Source source = session.getSource();
        FileSyncItem syncItem = session.getFileSyncItem();

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

        try (ReadableByteChannel inChannel = source.getChannel(syncItem))
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
            session.setFileSyncItem(null);
        }
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestCreateDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Create Directory");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String directory = new String(bytes, getCharset());

        session.getSink().createDirectory(directory);
        session.setLastCommand(JSyncCommand.TARGET_CREATE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestCreateSyncItems(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String basePath = new String(bytes, getCharset());

        Path base = Paths.get(basePath);

        Sink sink = new LocalhostSink(session.getOptions(), base.toUri());
        session.setSink(sink);

        buffer.clear();
        session.setLastCommand(JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestDeleteDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Delete Directory");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String directory = new String(bytes, getCharset());

        session.getSink().deleteDirectory(directory);
        session.setLastCommand(JSyncCommand.TARGET_DELETE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestDeleteFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Delete File");

        ByteBuffer buffer = session.getBuffer();

        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);
        String file = new String(bytes, getCharset());

        session.getSink().deleteFile(file);
        session.setLastCommand(JSyncCommand.TARGET_DELETE_FILE);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: FileChannel");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String fileName = new String(bytes, getCharset());

        FileSyncItem syncItem = new FileSyncItem(fileName);
        syncItem.setSize(fileSize);

        // if (buffer.get() == 1)
        // {
        // bytes = new byte[buffer.getInt()];
        // buffer.get(bytes);
        //
        // String checksum = new String(bytes, getCharset());
        // syncItem.setChecksum(checksum);
        // }

        Sink sink = session.getSink();

        long fileBytesTransferred = 0;

        // BiConsumer<Long, Long> monitor = (written, gesamt) -> {
        // String msg = String.format("Writen data for %s: %s = %6.2f %%", fileName, JSyncUtils.toHumanReadableSize(written),
        // JSyncUtils.getPercent(written, gesamt));
        // getLogger().debug(msg);
        // };

        // MessageDigest messageDigest = DigestUtils.createSha256Digest();
        // DigestUtils.digest(messageDigest, buffer);

        try (WritableByteChannel outChannel = sink.getChannel(syncItem))
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
    protected void targetRequestUpdateDirectory(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Update Directory");

        ByteBuffer buffer = session.getBuffer();

        DirectorySyncItem syncItem = Serializers.readFrom(buffer, DirectorySyncItem.class);

        Sink sink = session.getSink();
        sink.updateDirectory(syncItem);

        session.setLastCommand(JSyncCommand.TARGET_UPDATE_DIRECTORY);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestUpdateFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Update File");

        ByteBuffer buffer = session.getBuffer();

        FileSyncItem syncItem = Serializers.readFrom(buffer, FileSyncItem.class);

        Sink sink = session.getSink();
        sink.updateFile(syncItem);

        session.setLastCommand(JSyncCommand.TARGET_UPDATE_FILE);
        selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param channel {@link ReadableByteChannel}
     * @throws Exception Falls was schief geht.
     */
    protected void targetRequestValidateFile(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver request: Validate File");

        ByteBuffer buffer = session.getBuffer();

        long fileSize = buffer.getLong();
        byte[] bytes = new byte[buffer.getInt()];
        buffer.get(bytes);

        String fileName = new String(bytes, getCharset());

        FileSyncItem syncItem = new FileSyncItem(fileName);
        syncItem.setSize(fileSize);

        if (buffer.get() == 1)
        {
            bytes = new byte[buffer.getInt()];
            buffer.get(bytes);

            String checksum = new String(bytes, getCharset());
            syncItem.setChecksum(checksum);
        }

        Sink sink = session.getSink();

        try
        {
            sink.validateFile(syncItem);
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
    protected void targetResponseCreateSyncItems(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    {
        JSyncSession session = (JSyncSession) selectionKey.attachment();

        session.getLogger().debug("receiver response: Create SyncItems");

        ByteBuffer buffer = session.getBuffer();

        Sink sink = session.getSink();
        sink.createSyncItems(new GeneratorListener()
        {
            /**
             * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
             */
            @Override
            public void pathCount(final Path path, final int pathCount)
            {
                buffer.clear();
                buffer.putInt(pathCount);
                buffer.flip();

                try
                {
                    channel.write(buffer);
                }
                catch (IOException ioex)
                {
                    session.getLogger().error(null, ioex);
                }
            }

            /**
             * @see de.freese.jsync.generator.listener.GeneratorListener#processingSyncItem(de.freese.jsync.model.SyncItem)
             */
            @Override
            public void processingSyncItem(final SyncItem syncItem)
            {
                buffer.clear();

                if (syncItem instanceof FileSyncItem)
                {
                    buffer.put((byte) 0);
                    Serializers.writeTo(buffer, (FileSyncItem) syncItem);
                }
                else
                {
                    buffer.put((byte) 1);
                    Serializers.writeTo(buffer, (DirectorySyncItem) syncItem);
                }

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
            }
        });

        session.getLogger().debug("receiver response: Create SyncItems written");
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
            case CONNECT:
                writeFinishFlag(channel, buffer);
                break;

            case TARGET_CREATE_SYNC_ITEMS:
                targetResponseCreateSyncItems(selectionKey, channel);
                writeFinishFlag(channel, buffer);
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
                sourceResponseCreateSyncItems(selectionKey, channel);
                writeFinishFlag(channel, buffer);
                break;

            case SOURCE_READABLE_FILE_CHANNEL:
                sourceResponseFileChannel(selectionKey, channel);
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
        channel.write(buffer);
    }
}
