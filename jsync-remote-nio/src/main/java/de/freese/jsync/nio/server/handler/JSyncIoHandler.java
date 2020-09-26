// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

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
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * Verarbeitet den Request und Response.<br>
 * Sync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 * @see IoHandler
 */
public class JSyncIoHandler implements IoHandler<SelectionKey>
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
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);

        Exception exception = null;
        String checksum = null;

        try
        {
            checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                getSerializer().writeTo(buffer, checksum);
                RemoteUtils.writeEOL(buffer);
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
     * Create the Sync-Items.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     */
    protected void createDirectory(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativePath = getSerializer().readFrom(buffer, String.class);

        Exception exception = null;

        try
        {
            receiver.createDirectory(baseDir, relativePath);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
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
     * Create the Sync-Items.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param fileSystem {@link FileSystem}
     */
    protected void createSyncItems(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem)
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        boolean followSymLinks = getSerializer().readFrom(buffer, Boolean.class);

        Exception exception = null;
        List<SyncItem> syncItems = new ArrayList<>(128);

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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                buffer.putInt(syncItems.size());
                buffer.flip();
                writeBuffer(selectionKey, buffer);

                // SyncItems senden.
                for (SyncItem syncItem : syncItems)
                {
                    buffer.clear();
                    getSerializer().writeTo(buffer, syncItem);
                    buffer.flip();
                    writeBuffer(selectionKey, buffer);
                }

                // EOL senden.
                buffer.clear();
                RemoteUtils.writeEOL(buffer);
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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativePath = getSerializer().readFrom(buffer, String.class);
        boolean followSymLinks = getSerializer().readFrom(buffer, Boolean.class);

        Exception exception = null;

        try
        {
            receiver.delete(baseDir, relativePath, followSymLinks);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
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
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void fileChannel(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long size = buffer.getLong();

        Exception exception = null;
        WritableByteChannel outChannel = null;
        ReadableByteChannel inChannel = null;

        try
        {
            inChannel = (ReadableByteChannel) selectionKey.channel();
            outChannel = receiver.getChannel(baseDir, relativeFile, size);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                @SuppressWarnings("unused")
                long totalRead = 0;
                long totalWritten = 0;

                while (totalWritten < size)
                {
                    buffer.clear();

                    totalRead += inChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining())
                    {
                        totalWritten += outChannel.write(buffer);
                    }
                }

                if (outChannel instanceof FileChannel)
                {
                    ((FileChannel) outChannel).force(false);
                }

                // Response senden, kann hier erst erfolgen, wenn alle Daten empfangen wurden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
            finally
            {
                outChannel.close();
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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long size = buffer.getLong();

        Exception exception = null;
        WritableByteChannel outChannel = null;
        ReadableByteChannel inChannel = null;

        try
        {
            outChannel = (WritableByteChannel) selectionKey.channel();
            inChannel = sender.getChannel(baseDir, relativeFile, size);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);

                @SuppressWarnings("unused")
                long totalWritten = 0;
                long totalRead = 0;

                while (totalRead < size)
                {
                    buffer.clear();

                    totalRead += inChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining())
                    {
                        totalWritten += outChannel.write(buffer);
                    }
                }
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
            finally
            {
                inChannel.close();
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
     * @return {@link Serializer}<ByteBuffer>
     */
    private Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.nio.server.handler.IoHandler#read(java.lang.Object)
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

            JSyncCommand command = getSerializer().readFrom(buffer, JSyncCommand.class);
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
                    // Response senden.
                    buffer.clear();
                    RemoteUtils.writeResponseOK(buffer);
                    RemoteUtils.writeEOL(buffer);
                    buffer.flip();
                    writeBuffer(selectionKey, buffer);
                    break;

                case READ_CHUNK:
                    readChunk(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                    break;

                case TARGET_CHECKSUM:
                    createChecksum(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_CREATE_DIRECTORY:
                    createDirectory(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
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

            if (selectionKey.isValid())
            {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long position = buffer.getLong();
        long size = buffer.getLong();

        Exception exception = null;
        ByteBuffer bufferChunk = ByteBufferPool.getInstance().get();

        try
        {
            sender.readChunk(baseDir, relativeFile, position, size, bufferChunk);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);

                bufferChunk.flip();
                writeBuffer(selectionKey, bufferChunk);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }

        ByteBufferPool.getInstance().release(bufferChunk);
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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);

        Exception exception = null;

        try
        {
            receiver.update(baseDir, syncItem);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
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
     * Validate Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void validate(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);
        boolean withChecksum = getSerializer().readFrom(buffer, Boolean.class);

        Exception exception = null;

        try
        {
            receiver.validateFile(baseDir, syncItem, withChecksum);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
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
     * @see de.freese.jsync.nio.server.handler.IoHandler#write(java.lang.Object)
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
                selectionKey.attach(null);

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
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long position = buffer.getLong();
        long size = buffer.getLong();

        Exception exception = null;
        ByteBuffer bufferData = ByteBufferPool.getInstance().get();

        try
        {
            ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

            bufferData.clear();

            while (bufferData.position() < size)
            {
                bufferData.put(buffer);

                buffer.clear();
                channel.read(buffer);
                buffer.flip();
            }

            receiver.writeChunk(baseDir, relativeFile, position, size, bufferData);
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
                // Exception senden.
                buffer.clear();
                RemoteUtils.writeResponseERROR(buffer);
                getSerializer().writeTo(buffer, exception, Exception.class);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
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
                // Response senden.
                buffer.clear();
                RemoteUtils.writeResponseOK(buffer);
                RemoteUtils.writeEOL(buffer);
                buffer.flip();
                writeBuffer(selectionKey, buffer);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
    }
}
