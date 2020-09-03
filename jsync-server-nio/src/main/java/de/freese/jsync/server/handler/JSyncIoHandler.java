// Created: 04.11.2018
package de.freese.jsync.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.receiver.LocalhostReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.filesystem.sender.LocalhostSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.pool.ByteBufferPool;

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

        writeBuffer(channel, buffer);
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    private static void writeBuffer(final SocketChannel channel, final ByteBuffer buffer) throws IOException
    {
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

        String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
        });

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
     */
    protected void createSyncItems(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem)
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        boolean followSymLinks = Serializers.readFrom(buffer, Boolean.class);

        List<SyncItem> syncItems = new ArrayList<>(128);
        Exception exception = null;

        try
        {
            fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
                getLogger().debug("{}: SyncItem generated: {}", getRemoteAddress(selectionKey), syncItem);

                syncItems.add(syncItem);
            });
        }
        catch (Exception ex)
        {
            exception = ex;
        }

        if (exception != null)
        {
            getLogger().error(null, exception);

            try
            {
                // Anzahl SyncItems senden.
                buffer.clear();
                buffer.putInt(0);
                buffer.flip();
                writeBuffer(selectionKey, buffer);

                // Exception senden.
                // TODO
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            try
            {
                // Anzahl SyncItems senden.
                buffer.clear();
                buffer.putInt(syncItems.size());
                buffer.flip();
                writeBuffer(selectionKey, buffer);

                // SyncItems senden.
                for (SyncItem syncItem : syncItems)
                {
                    buffer.clear();
                    Serializers.writeTo(buffer, syncItem);
                    buffer.flip();
                    writeBuffer(selectionKey, buffer);
                }

                // EOL senden.
                buffer.clear();
                Serializers.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
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

        receiver.delete(baseDir, relativePath, followSymLinks);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void fileChannel(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        ReadableByteChannel inChannel = (ReadableByteChannel) selectionKey.channel();

        long bytesRead = 0;
        long bytesWrote = 0;

        try (WritableByteChannel outChannel = receiver.getChannel(baseDir, relativeFile))
        {
            // Beim Empfang sind weitere Daten im Buffer enthalten.
            do
            {
                while (buffer.hasRemaining())
                {
                    bytesWrote += outChannel.write(buffer);
                }

                buffer.clear();
                bytesRead = inChannel.read(buffer);
                buffer.flip();
            }
            while (bytesRead > 0);
            // while (inChannel.read(buffer) > 0)
            // {
            // buffer.flip();
            //
            // while (buffer.hasRemaining())
            // {
            // bytesWrote += outChannel.write(buffer);
            // }
            //
            // buffer.clear();
            // }

            if (outChannel instanceof FileChannel)
            {
                ((FileChannel) outChannel).force(true);
            }
        }
    }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     * @throws Exception Falls was schief geht.
     */
    protected void fileChannel(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);

        WritableByteChannel outChannel = (WritableByteChannel) selectionKey.channel();

        long bytesWrote = 0;

        buffer.clear();

        try (ReadableByteChannel inChannel = sender.getChannel(baseDir, relativeFile))
        {
            while (inChannel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    bytesWrote += outChannel.write(buffer);
                }

                buffer.clear();
            }
        }
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param selectionKey SelectionKey
     * @return String
     */
    private String getRemoteAddress(final SelectionKey selectionKey)
    {
        try
        {
            return ((SocketChannel) selectionKey.channel()).getRemoteAddress().toString();
        }
        catch (IOException ex)
        {
            return "";
        }
    }

    /**
     * @see IoHandler#read(SelectionKey)
     */
    @Override
    public void read(final SelectionKey selectionKey)
    {
        ByteBuffer buffer = ByteBufferPool.getInstance().get();

        try
        {
            ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

            // JSyncCommand lesen.
            buffer.clear();

            int bytesRead = channel.read(buffer);

            if (bytesRead == -1)
            {
                // Nach Disconnect
                return;
            }

            buffer.flip();

            JSyncCommand command = Serializers.readFrom(buffer, JSyncCommand.class);
            getLogger().debug("{}: read command: {}", getRemoteAddress(selectionKey), command);

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
                    selectionKey.channel().close();
                    selectionKey.cancel();
                    break;

                case CONNECT:
                    // Empty
                    break;

                case READ_CHUNK:
                    readChunk(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
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

                case TARGET_WRITEABLE_FILE_CHANNEL:
                    fileChannel(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

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

                case SOURCE_READABLE_FILE_CHANNEL:
                    fileChannel(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                    break;

                case WRITE_CHUNK:
                    writeChunk(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                default:
                    break;
            }

            // selectionKey.interestOps(SelectionKey.OP_READ);
            // selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            ByteBufferPool.getInstance().release(buffer);
        }
    }

    /**
     * Read File-Chunk from Sender.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     * @throws Exception Falls was schief geht.
     */
    protected void readChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);
        long position = buffer.getLong();
        long size = buffer.getLong();

        try
        {
            ByteBuffer bufferChunk = ByteBufferPool.getInstance().get();

            try
            {
                sender.readChunk(baseDir, relativeFile, position, size, bufferChunk);

                bufferChunk.flip();
                writeBuffer(selectionKey, bufferChunk);
            }
            finally
            {
                ByteBufferPool.getInstance().release(bufferChunk);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            buffer.clear();
            Serializers.writeTo(buffer, ex.getMessage());
            buffer.flip();

            writeBuffer(selectionKey, buffer);
        }
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

        receiver.update(baseDir, syncItem);
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
    }

    /**
     * @see IoHandler#write(SelectionKey)
     */
    @Override
    public void write(final SelectionKey selectionKey)
    {
        try
        {
            // WritableByteChannel channel = (WritableByteChannel) selectionKey.channel();

            if (selectionKey.attachment() instanceof Runnable)
            {
                Runnable task = (Runnable) selectionKey.attachment();
                task.run();
            }

            selectionKey.interestOps(SelectionKey.OP_READ);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * Write File-Chunk to Receiver.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void writeChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = Serializers.readFrom(buffer, String.class);
        String relativeFile = Serializers.readFrom(buffer, String.class);
        long position = buffer.getLong();
        long size = buffer.getLong();

        ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

        try
        {
            ByteBuffer bufferData = ByteBufferPool.getInstance().get();
            bufferData.clear();

            while (bufferData.position() < size)
            {
                bufferData.put(buffer);

                buffer.clear();
                channel.read(buffer);
                buffer.flip();
            }

            try
            {
                receiver.writeChunk(baseDir, relativeFile, position, size, bufferData);
            }
            finally
            {
                ByteBufferPool.getInstance().release(bufferData);
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            buffer.clear();
            Serializers.writeTo(buffer, ex.getMessage());
            buffer.flip();

            writeBuffer(selectionKey, buffer);
        }
    }
}
