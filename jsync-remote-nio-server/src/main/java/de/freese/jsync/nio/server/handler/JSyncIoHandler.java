// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filesystem.local.LocalhostReceiver;
import de.freese.jsync.filesystem.local.LocalhostSender;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.server.JsyncServerResponse;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * Verarbeitet den Request und Response.<br>
 * Sync-Implementierung des {@link IoHandler}.
 *
 * @author Thomas Freese
 *
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
     *
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
     *
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

        try
        {
            String checksum = fileSystem.getChecksum(baseDir, relativeFile, i -> {
            });

            JsyncServerResponse.ok(buffer).write(selectionKey, buf -> getSerializer().writeTo(buffer, checksum));
        }
        catch (Exception ex)
        {
            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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

        try
        {
            receiver.createDirectory(baseDir, relativePath);

            // Response senden.
            JsyncServerResponse.ok(buffer).write(selectionKey);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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

        try
        {
            List<SyncItem> syncItems = new ArrayList<>(128);

            fileSystem.generateSyncItems(baseDir, followSymLinks, syncItem -> {
                getLogger().debug("{}: SyncItem generated: {}", getRemoteAddress(selectionKey), syncItem);

                syncItems.add(syncItem);
            });

            // Response senden.
            JsyncServerResponse.ok(buffer).write(selectionKey, buf -> {
                buf.putInt(syncItems.size());

                for (SyncItem syncItem : syncItems)
                {
                    getSerializer().writeTo(buf, syncItem);
                }
            });
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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

        try
        {
            receiver.delete(baseDir, relativePath, followSymLinks);

            // Response senden.
            JsyncServerResponse.ok(buffer).write(selectionKey);
        }
        catch (Exception ex)
        {
            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
     *
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
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().allocate();

        try
        {
            ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();

            // JSyncCommand lesen.
            int bytesRead = channel.read(byteBuffer);

            if (bytesRead == -1)
            {
                // Nach Disconnect
                return;
            }

            byteBuffer.flip();

            JSyncCommand command = getSerializer().readFrom(byteBuffer, JSyncCommand.class);
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
                    JsyncServerResponse.ok(byteBuffer).write(selectionKey, buf -> getSerializer().writeTo(buf, "DISCONNECTED"));

                    selectionKey.attach(null);
                    // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                    selectionKey.channel().close();
                    selectionKey.cancel();
                    break;

                case CONNECT:
                    JsyncServerResponse.ok(byteBuffer).write(selectionKey, buf -> getSerializer().writeTo(buf, "CONNECTED"));
                    break;

                case SOURCE_CHECKSUM:
                    createChecksum(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                    break;

                case SOURCE_CREATE_SYNC_ITEMS:
                    createSyncItems(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                    break;

                // case SOURCE_READ_CHUNK:
                // readChunk(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                // break;

                case SOURCE_READ_FILE_HANDLE:
                    readFileHandle(selectionKey, byteBuffer, THREAD_LOCAL_SENDER.get());
                    break;

                case TARGET_CHECKSUM:
                    createChecksum(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_CREATE_DIRECTORY:
                    createDirectory(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_CREATE_SYNC_ITEMS:
                    createSyncItems(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_DELETE:
                    delete(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_UPDATE:
                    update(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                case TARGET_VALIDATE_FILE:
                    validate(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                    break;

                // case TARGET_WRITE_CHUNK:
                // writeChunk(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
                // break;

                case TARGET_WRITE_FILE_HANDLE:
                    writeFileHandle(selectionKey, byteBuffer, THREAD_LOCAL_RECEIVER.get());
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
            ByteBufferPool.getInstance().release(byteBuffer);
        }
    }

    // /**
    // * Read File-Chunk from Sender.
    // *
    // * @param selectionKey {@link SelectionKey}
    // * @param buffer {@link ByteBuffer}
    // * @param sender {@link Sender}
    // */
    // protected void readChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender)
    // {
    // String baseDir = getSerializer().readFrom(buffer, String.class);
    // String relativeFile = getSerializer().readFrom(buffer, String.class);
    // long position = buffer.getLong();
    // long sizeOfChunk = buffer.getLong();
    //
    // DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) sizeOfChunk);
    // dataBuffer.readPosition(0);
    // dataBuffer.writePosition(0);
    //
    // ByteBuffer bufferChunk = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);
    //
    // try
    // {
    // sender.readChunk(baseDir, relativeFile, position, sizeOfChunk, bufferChunk);
    //
    // // Response senden
    // // JsyncServerResponse.ok(buffer).write(selectionKey);
    // buffer.clear();
    // buffer.putInt(RemoteUtils.STATUS_OK); // Status
    // buffer.putLong(sizeOfChunk); // Content-Length
    // buffer.flip();
    //
    // bufferChunk.flip();
    //
    // SocketChannel channel = (SocketChannel) selectionKey.channel();
    // channel.write(new ByteBuffer[]
    // {
    // buffer, bufferChunk
    // });
    // // writeBuffer(selectionKey, bufferChunk);
    // }
    // catch (Exception ex)
    // {
    // getLogger().error(null, ex);
    //
    // // Exception senden.
    // try
    // {
    // JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
    // }
    // catch (IOException ioex)
    // {
    // getLogger().error(null, ioex);
    // }
    // }
    // finally
    // {
    // DataBufferUtils.release(dataBuffer);
    // }
    // }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     *
     * @throws Exception Falls was schief geht.
     */
    protected void readFileHandle(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long sizeOfFile = buffer.getLong();

        Exception exception = null;
        WritableByteChannel outChannel = null;
        FileHandle fileHandle = null;

        try
        {
            outChannel = (WritableByteChannel) selectionKey.channel();
            fileHandle = sender.readFileHandle(baseDir, relativeFile, sizeOfFile);
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
            try (ReadableByteChannel inChannel = fileHandle.getHandle())
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

        try
        {
            receiver.update(baseDir, syncItem);

            // Response senden.
            JsyncServerResponse.ok(buffer).write(selectionKey);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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

        try
        {
            receiver.validateFile(baseDir, syncItem, withChecksum);

            // Response senden.
            JsyncServerResponse.ok(buffer).write(selectionKey);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                // Exception senden.
                JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
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
     * Die Daten werden zum Server gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param receiver {@link Receiver}
     *
     * @throws Exception Falls was schief geht.
     */
    protected void writeFileHandle(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
    {
        String baseDir = getSerializer().readFrom(buffer, String.class);
        String relativeFile = getSerializer().readFrom(buffer, String.class);
        long sizeOfFile = getSerializer().readFrom(buffer, Long.class);

        Exception exception = null;
        // WritableResource resourceReceiver = null;
        ReadableByteChannel inChannel = null;

        try
        {
            inChannel = (ReadableByteChannel) selectionKey.channel();
            // resourceReceiver = receiver.getResource(baseDir, relativeFile, sizeOfFile);
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
            Path path = Paths.get(baseDir, relativeFile);
            Path parentPath = path.getParent();

            if (Files.notExists(parentPath))
            {
                Files.createDirectories(parentPath);
            }

            if (Files.notExists(path))
            {
                Files.createFile(path);
            }

            try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))
            {
                long totalWritten = 0;

                while (totalWritten < sizeOfFile)
                {
                    buffer.clear();
                    inChannel.read(buffer);
                    buffer.flip();

                    while (buffer.hasRemaining())
                    {
                        totalWritten += fileChannel.write(buffer);

                        getLogger().debug("WritableByteChannel: sizeOfFile={}, totalWritten={}", sizeOfFile, totalWritten);
                    }
                }

                fileChannel.force(false);
            }
            catch (Exception ex)
            {
                getLogger().error(null, ex);
            }

            // JsyncServerResponse.ok(buffer).write(selectionKey);
            buffer.clear();
            buffer.putInt(RemoteUtils.STATUS_OK); // Status
            buffer.putLong(sizeOfFile); // Content-Length

            buffer.flip();
            writeBuffer(selectionKey, buffer);
        }
    }

    // /**
    // * Write File-Chunk to Receiver.
    // *
    // * @param selectionKey {@link SelectionKey}
    // * @param buffer {@link ByteBuffer}
    // * @param receiver {@link Receiver}
    // */
    // protected void writeChunk(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver)
    // {
    // String baseDir = getSerializer().readFrom(buffer, String.class);
    // String relativeFile = getSerializer().readFrom(buffer, String.class);
    // long position = buffer.getLong();
    // long sizeOfChunk = buffer.getLong();
    //
    // DataBuffer dataBuffer = this.dataBufferFactory.allocateBuffer((int) sizeOfChunk);
    // dataBuffer.readPosition(0);
    // dataBuffer.writePosition(0);
    //
    // ByteBuffer bufferChunk = dataBuffer.asByteBuffer(0, (int) sizeOfChunk);
    //
    // try
    // {
    // ReadableByteChannel channel = (ReadableByteChannel) selectionKey.channel();
    //
    // bufferChunk.clear();
    // bufferChunk.put(buffer);
    //
    // while (bufferChunk.position() < sizeOfChunk)
    // {
    // buffer.clear();
    // channel.read(buffer);
    // buffer.flip();
    //
    // bufferChunk.put(buffer);
    // }
    //
    // receiver.writeChunk(baseDir, relativeFile, position, sizeOfChunk, bufferChunk);
    //
    // // Response senden.
    // JsyncServerResponse.ok(buffer).write(selectionKey);
    // }
    // catch (Exception ex)
    // {
    // getLogger().error(null, ex);
    //
    // try
    // {
    // // Exception senden.
    // JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
    // }
    // catch (IOException ioex)
    // {
    // getLogger().error(null, ioex);
    // }
    // }
    // finally
    // {
    // DataBufferUtils.release(dataBuffer);
    // }
    // }
}
