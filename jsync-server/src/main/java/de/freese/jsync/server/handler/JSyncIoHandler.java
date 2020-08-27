// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;

/**
 * Verarbeitet den Request und Response.<br>
 * Sync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see IoHandler
 */
@SuppressWarnings("resource")
public class JSyncIoHandler implements IoHandler
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncIoHandler.class);
    /**
     *
     */
    private static final ThreadLocal<ByteBuffer> THREAD_LOCAL_BUFFER = ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(Options.BUFFER_SIZE));
    /**
     *
     */
    private static final ThreadLocal<Receiver> THREAD_LOCAL_RECEIVER = ThreadLocal.withInitial(LocalhostReceiver::new);
    /**
     *
     */
    private static final ThreadLocal<Sender> THREAD_LOCAL_SENDER = ThreadLocal.withInitial(LocalhostSender::new);

    /**
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    private static void writeBuffer(final SelectionKey selectionKey, final ByteBuffer buffer) throws IOException
    {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
    }

    /**
     * Erstellt ein neues {@link JSyncIoHandler} Object.
     */
    public JSyncIoHandler()
    {
        super();
    }

    /**
     * @see IoHandler#read(SelectionKey)
     */
    @Override
    public void read(final SelectionKey selectionKey) throws Exception
    {
        ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

        // JSyncCommand lesen.
        ByteBuffer buffer = THREAD_LOCAL_BUFFER.get();

        buffer.clear();

        int bytesRead = channel.read(buffer);

        if (bytesRead == -1)
        {
            // Nach Disconnect
            return;
        }

        buffer.flip();

        JSyncCommand command = Serializers.readFrom(buffer, JSyncCommand.class);
        getLogger().debug("read command: {}", command);

        if (command == null)
        {
            getLogger().error("unknown JSyncCommand");
            selectionKey.interestOps(SelectionKey.OP_READ);
            return;
        }

        switch (command)
        {
            case DISCONNECT:
                selectionKey.attach(null);
                // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                selectionKey.cancel();
                break;

            case CONNECT:
                // Empty
                break;

            case TARGET_CHECKSUM:
                createChecksum(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_CREATE_SYNC_ITEMS:
                createSyncItems(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_DELETE:
                delete(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            // case TARGET_WRITEABLE_FILE_CHANNEL:
            // receiverRequestFileChannel(selectionKey, channel);
            // break;

            case TARGET_VALIDATE_FILE:
                validate(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case TARGET_UPDATE:
                update(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                break;

            case SOURCE_CHECKSUM:
                createChecksum(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                break;

            case SOURCE_CREATE_SYNC_ITEMS:
                createSyncItems(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                break;

            // case SOURCE_READABLE_FILE_CHANNEL:
            // senderRequestFileChannel(selectionKey, channel);
            // break;

            default:
                break;
        }

        // selectionKey.interestOps(SelectionKey.OP_READ);
        // selectionKey.interestOps(SelectionKey.OP_WRITE);
    }

    /**
     * @see IoHandler#write(SelectionKey)
     */
    @Override
    public void write(final SelectionKey selectionKey) throws Exception
    {
        // WritableByteChannel channel = (WritableByteChannel) selectionKey.channel();

        if (selectionKey.attachment() instanceof Runnable)
        {
            Runnable task = (Runnable) selectionKey.attachment();
            task.run();
        }

        selectionKey.interestOps(SelectionKey.OP_READ);
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Create the checksum.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param fileSystem {@link FileSystem}
     * @throws Exception Falls was schief geht.
     */
    protected void createChecksum(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        getLogger().debug("Create Checksum: {}/{}", baseDir, relativeFile);

        String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
        });

        getLogger().debug("Checksum created: {}/{}", baseDir, relativeFile);

        buffer.clear();
        Serializers.writeTo(buffer, checksum);
        buffer.flip();

        writeBuffer(selectionKey, buffer);
    }

    /**
     * Create the Sync-Items.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param fileSystem {@link FileSystem}
     * @throws Exception Falls was schief geht.
     */
    protected void createSyncItems(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        boolean followSymLinks = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("Create SyncItems: {}", baseDir);

        fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
            getLogger().debug("Send SyncItem: {}", syncItem);

            buffer.clear();
            Serializers.writeTo(buffer, syncItem);
            buffer.flip();

            try
            {
                writeBuffer(selectionKey, buffer);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        });

        // EOL
        buffer.clear();
        Serializers.writeEOL(buffer);
        buffer.flip();

        writeBuffer(selectionKey, buffer);

        getLogger().debug("SyncItems written: {}", baseDir);
    }

    /**
     * Delete Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void delete(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativePath = Serializers.readFrom(buffer, String.class);
        boolean followSymLinks = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("Delete: {}/{}", baseDir, relativePath);

        receiver.delete(baseDir, relativePath, followSymLinks);

        getLogger().debug("Deleted: {}/{}", baseDir, relativePath);
    }

    /**
     * Update Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void update(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);

        getLogger().debug("Update: {}/{}", baseDir, syncItem.getRelativePath());

        receiver.update(baseDir, syncItem);

        getLogger().debug("Updated: {}/{}", baseDir, syncItem.getRelativePath());
    }

    /**
     * Validate Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void validate(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
        boolean withChecksum = Serializers.readFrom(buffer, Boolean.class);

        getLogger().debug("Validate: {}/{}", baseDir, syncItem.getRelativePath());

        try
        {
            receiver.validateFile(baseDir, syncItem, withChecksum);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            buffer.clear();
            Serializers.writeTo(buffer, ex.getMessage());
            buffer.flip();

            writeBuffer(selectionKey, buffer);
        }

        getLogger().debug("Validated: {}/{}", baseDir, syncItem.getRelativePath());
    }

    // /**
    // * @param selectionKey {@link SelectionKey}
    // * @param channel {@link ReadableByteChannel}
    // *
    // * @throws Exception Falls was schief geht.
    // */
    // protected void receiverRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    // {
    // JSyncSession session = (JSyncSession) selectionKey.attachment();
    //
    // session.getLogger().debug("receiver request: FileChannel");
    //
    // ByteBuffer buffer = session.getBuffer();
    //
    // String baseDir = Serializers.readFrom(buffer, String.class);
    // String relativeFile = Serializers.readFrom(buffer, String.class);
    //
    //// long fileSize = buffer.getLong();
    //// byte[] bytes = new byte[buffer.getInt()];
    //// buffer.get(bytes);
    ////
    //// String fileName = new String(bytes, getCharset());
    ////
    //// SyncItem syncItem = new DefaultSyncItem(fileName);
    //// syncItem.setFile(true);
    //// syncItem.setSize(fileSize);
    //
    // Receiver receiver = THREAD_LOCAL_RECEIVER.get();
    //
    // long fileBytesTransferred = 0;
    //
    // // BiConsumer<Long, Long> monitor = (written, gesamt) -> {
    // // String msg = String.format("Writen data for %s: %s = %6.2f %%", fileName, JSyncUtils.toHumanReadableSize(written),
    // // JSyncUtils.getPercent(written, gesamt));
    // // getLogger().debug(msg);
    // // };
    //
    // // MessageDigest messageDigest = DigestUtils.createSha256Digest();
    // // DigestUtils.digest(messageDigest, buffer);
    //
    // try (WritableByteChannel outChannel = receiver.getChannel(baseDir, relativeFile))
    // // try (WritableByteChannel outChannel = new MonitoringWritableByteChannel(receiver.getChannel(syncItem), monitor, fileSize))
    // {
    // // Restlichen Buffer in die Datei schreiben.
    // fileBytesTransferred += outChannel.write(buffer);
    // buffer.clear();
    //
    //// while (fileBytesTransferred < fileSize)
    // {
    // channel.read(buffer);
    // buffer.flip();
    //
    // // DigestUtils.digest(messageDigest, buffer);
    //
    // while (buffer.hasRemaining())
    // {
    // fileBytesTransferred += outChannel.write(buffer);
    // }
    //
    // buffer.clear();
    // }
    //
    // if (outChannel instanceof FileChannel)
    // {
    // ((FileChannel) outChannel).force(true);
    // }
    // }
    //
    // // Funktioniert, ist aber falsche Stelle.
    // // String checksum = DigestUtils.digestAsHex(messageDigest);
    // // session.setLastCecksum(checksum);
    //
    // session.setLastCommand(JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
    // selectionKey.interestOps(SelectionKey.OP_WRITE);
    // }

    // /**
    // * @param selectionKey {@link SelectionKey}
    // * @param channel {@link ReadableByteChannel}
    // *
    // * @throws Exception Falls was schief geht.
    // */
    // protected void senderRequestFileChannel(final SelectionKey selectionKey, final ReadableByteChannel channel) throws Exception
    // {
    // JSyncSession session = (JSyncSession) selectionKey.attachment();
    //
    // session.getLogger().debug("sender request: FileChannel");
    //
    // ByteBuffer buffer = session.getBuffer();
    //
    // String baseDir = Serializers.readFrom(buffer, String.class);
    // String relativeFile = Serializers.readFrom(buffer, String.class);
    //
    // SyncItem syncItem = new DefaultSyncItem(fileName);
    // syncItem.setFile(true);
    // syncItem.setSize(fileSize);
    //
    // session.setSyncItem(syncItem);
    //
    // session.setLastCommand(JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
    // selectionKey.interestOps(SelectionKey.OP_WRITE);
    // }

    // /**
    // * @param selectionKey {@link SelectionKey}
    // * @param channel {@link WritableByteChannel}
    // *
    // * @throws Exception Falls was schief geht.
    // */
    // protected void senderResponseFileChannel(final SelectionKey selectionKey, final WritableByteChannel channel) throws Exception
    // {
    // JSyncSession session = (JSyncSession) selectionKey.attachment();
    //
    // session.getLogger().debug("sender response: FileChannel");
    //
    // ByteBuffer buffer = session.getBuffer();
    //
    // Sender sender = THREAD_LOCAL_SENDER.get();
    // SyncItem syncItem = session.getSyncItem();
    //
    // long fileSize = syncItem.getSize();
    // long fileBytesTransferred = 0;
    //
    // // BiConsumer<Long, Long> monitor = (read, gesamt) -> {
    // // String msg = String.format("read data for %s: %s = %6.2f %%", syncItem.getRelativePath(), JSyncUtils.toHumanReadableSize(read),
    // // JSyncUtils.getPercent(read, gesamt));
    // // getLogger().debug(msg);
    // // };
    //
    // // MessageDigest messageDigest = DigestUtils.createSha256Digest();
    // // DigestUtils.digest(messageDigest, buffer);
    //
    // buffer.clear();
    //
    // try (ReadableByteChannel inChannel = sender.getChannel(syncItem))
    // // try (ReadableByteChannel inChannel = new MonitoringReadableByteChannel(sender.getChannel(syncItem), monitor, fileSize))
    // {
    // while (fileBytesTransferred < fileSize)
    // {
    // inChannel.read(buffer);
    // buffer.flip();
    //
    // while (buffer.hasRemaining())
    // {
    // fileBytesTransferred += channel.write(buffer);
    // }
    //
    // buffer.clear();
    // }
    // }
    // finally
    // {
    // session.setSyncItem(null);
    // }
    // }
}
