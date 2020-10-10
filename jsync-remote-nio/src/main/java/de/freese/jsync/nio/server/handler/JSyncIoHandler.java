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
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
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
import de.freese.jsync.nio.server.JsyncServerResponse;
import de.freese.jsync.remote.RemoteUtils;
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
    protected static void writeBuffer(final SelectionKey selectionKey, final ByteBuffer buffer) throws IOException
    {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        writeBuffer(channel, buffer);
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    protected static void writeBuffer(final SocketChannel channel, final ByteBuffer buffer) throws IOException
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
     */
    protected void createChecksum(final SelectionKey selectionKey, final ByteBuffer buffer, final FileSystem fileSystem)
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                String chksm = checksum;
                JsyncServerResponse.ok(buffer).write(selectionKey, buf -> getSerializer().writeTo(buffer, chksm));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
    }

    /**
     * Create the Directory.
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey);
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey, buf -> {
                    buf.putInt(syncItems.size());

                    for (SyncItem syncItem : syncItems)
                    {
                        getSerializer().writeTo(buf, syncItem);
                    }
                });
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
     */
    protected void delete(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @param selectionKey SelectionKey
     * @return String
     */
    protected String getRemoteAddress(final SelectionKey selectionKey)
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
    protected Serializer<ByteBuffer> getSerializer()
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
                    JsyncServerResponse.ok(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, "DISCONNECTED"));

                    selectionKey.attach(null);
                    // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                    selectionKey.channel().close();
                    selectionKey.cancel();
                    break;

                case CONNECT:
                    JsyncServerResponse.ok(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, "CONNECTED"));
                    break;

                case SOURCE_CHECKSUM:
                    createChecksum(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                    break;

                case SOURCE_CREATE_SYNC_ITEMS:
                    createSyncItems(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                    break;

                case SOURCE_READ_CHUNK:
                    readChunk(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
                    break;

                case SOURCE_READABLE_RESOURCE:
                    resourceReadable(selectionKey, buffer, THREAD_LOCAL_SENDER.get());
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

                case TARGET_WRITE_CHUNK:
                    writeChunk(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_UPDATE:
                    update(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_VALIDATE_FILE:
                    validate(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_WRITEABLE_RESOURCE:
                    resourceWritable(selectionKey, buffer, THREAD_LOCAL_RECEIVER.get());
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
     */
    protected void readChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender)
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long position = buffer.getLong();
        long sizeOfChunk = buffer.getLong();

        Exception exception = null;
        ByteBuffer bufferChunk = ByteBufferPool.getInstance().get();

        try
        {
            sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, bufferChunk);
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                // JsyncServerResponse.ok(buffer).write(selectionKey);
                buffer.clear();
                buffer.putInt(RemoteUtils.STATUS_OK); // Status
                buffer.putLong(sizeOfChunk); // Content-Length
                buffer.flip();

                bufferChunk.flip();

                SocketChannel channel = (SocketChannel) selectionKey.channel();
                channel.write(new ByteBuffer[]
                {
                        buffer, bufferChunk
                });
                // writeBuffer(selectionKey, bufferChunk);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }

        ByteBufferPool.getInstance().release(bufferChunk);
    }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     * @throws Exception Falls was schief geht.
     */
    protected void resourceReadable(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long sizeOfFile = buffer.getLong();

        Exception exception = null;
        WritableByteChannel outChannel = null;
        Resource resourceSender = null;

        try
        {
            outChannel = (WritableByteChannel) selectionKey.channel();
            resourceSender = sender.getResource(baseDir, relativeFile, sizeOfFile);
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            try (ReadableByteChannel inChannel = resourceSender.readableChannel())
            {
                // Response senden
                buffer.clear();
                buffer.putInt(RemoteUtils.STATUS_OK); // Status
                buffer.putLong(sizeOfFile); // Content-Length

                @SuppressWarnings("unused")
                long totalWritten = 0;
                long totalRead = 0;

                while (totalRead < sizeOfFile)
                {
                    totalRead += inChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining())
                    {
                        totalWritten += outChannel.write(buffer);

                        getLogger().debug("ReadableByteChannel: sizeOfFile={}, totalRead={}", sizeOfFile, totalRead);
                    }

                    buffer.clear();
                }
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
    }

    /**
     * Die Daten werden zum Server gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     * @throws Exception Falls was schief geht.
     */
    protected void resourceWritable(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long sizeOfFile = buffer.getLong();

        Exception exception = null;
        WritableResource resourceReceiver = null;
        ReadableByteChannel inChannel = null;

        try
        {
            inChannel = (ReadableByteChannel) selectionKey.channel();
            resourceReceiver = receiver.getResource(baseDir, relativeFile, sizeOfFile);
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }
        else
        {
            try (WritableByteChannel outChannel = resourceReceiver.writableChannel())
            {
                @SuppressWarnings("unused")
                long totalRead = 0;
                long totalWritten = 0;

                while (totalWritten < sizeOfFile)
                {
                    buffer.clear();

                    totalRead += inChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining())
                    {
                        totalWritten += outChannel.write(buffer);

                        getLogger().debug("WritableByteChannel: sizeOfFile={}, totalWritten={}", sizeOfFile, totalWritten);
                    }
                }

                if (outChannel instanceof FileChannel)
                {
                    ((FileChannel) outChannel).force(false);
                }

                // JsyncServerResponse.ok(buffer).write(selectionKey);
                buffer.clear();
                buffer.putInt(RemoteUtils.STATUS_OK); // Status
                buffer.putLong(sizeOfFile); // Content-Length

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
     * Update Directory or File.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     */
    protected void update(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey);
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
     */
    protected void validate(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey);
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
     */
    protected void writeChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long position = buffer.getLong();
        long sizeOfChunk = buffer.getLong();

        Exception exception = null;
        ByteBuffer bufferChunk = ByteBufferPool.getInstance().get();

        try
        {
            ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

            bufferChunk.clear();
            bufferChunk.put(buffer);

            while (bufferChunk.position() < sizeOfChunk)
            {
                buffer.clear();
                channel.read(buffer);
                buffer.flip();

                bufferChunk.put(buffer);
            }

            receiver.writeChunk(baseDir, relativeFile, position, sizeOfChunk, bufferChunk);
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
                Exception ex = exception;
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
                JsyncServerResponse.ok(buffer).write(selectionKey);
            }
            catch (IOException ioex)
            {
                getLogger().error(null, ioex);
            }
        }

        ByteBufferPool.getInstance().release(bufferChunk);
    }
}
