// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.Receiver;
import de.freese.jsync.filesystem.ReceiverDelegateLogger;
import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filesystem.SenderDelegateLogger;
import de.freese.jsync.filesystem.local.LocalhostReceiver;
import de.freese.jsync.filesystem.local.LocalhostSender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.server.JsyncServerResponse;
import de.freese.jsync.nio.transport.NioTransport;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.utils.pool.Pool;

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
    private static final Pool<Receiver> POOL_RECEIVER = new Pool<>(true, true)
    {
        /**
         * @see de.freese.jsync.utils.pool.Pool#create()
         */
        @Override
        protected Receiver create()
        {
            return new ReceiverDelegateLogger(new LocalhostReceiver());
        }
    };

    /**
     *
     */
    private static final Pool<Sender> POOL_SENDER = new Pool<>(true, true)
    {
        /**
         * @see de.freese.jsync.utils.pool.Pool#create()
         */
        @Override
        protected Sender create()
        {
            return new SenderDelegateLogger(new LocalhostSender());
        }
    };

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
    private final NioTransport nioTransport = new NioTransport();

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
            String checksum = fileSystem.generateChecksum(baseDir, relativeFile, null);

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
     * @param channel {@link SocketChannel}
     * @param fileSystem {@link FileSystem}
     */
    protected void createSyncItems(final SocketChannel channel, final FileSystem fileSystem)
    {
        AtomicReference<String> refBaseDir = new AtomicReference<>();
        AtomicReference<Boolean> refFollowSymLinks = new AtomicReference<>();
        AtomicReference<PathFilter> refPathFilter = new AtomicReference<>();

        try
        {
            this.nioTransport.readAll(channel, buffer -> {
                refBaseDir.set(getSerializer().readFrom(buffer, String.class));
                refFollowSymLinks.set(getSerializer().readFrom(buffer, Boolean.class));
                refPathFilter.set(getSerializer().readFrom(buffer, PathFilter.class));
            });

            String baseDir = refBaseDir.get();
            boolean followSymLinks = refFollowSymLinks.get();
            PathFilter pathFilter = refPathFilter.get();

            fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).subscribe(syncItem -> {
                this.nioTransport.writeData(channel, buffer -> getSerializer().writeTo(buffer, syncItem));
            });

            this.nioTransport.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.nioTransport.writeError(channel, ex);
            }
            catch (Exception ioex)
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
        Sender sender = POOL_SENDER.obtain();
        Receiver receiver = POOL_RECEIVER.obtain();

        try
        {
            SocketChannel channel = (SocketChannel) selectionKey.channel();

            AtomicReference<JSyncCommand> refCommand = new AtomicReference<>();

            this.nioTransport.readFrame(channel, buf -> refCommand.set(getSerializer().readFrom(buf, JSyncCommand.class)));

            JSyncCommand command = refCommand.get();
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
                    // FINISH-Frame lesen
                    this.nioTransport.readFrame(channel, buf -> {
                    });

                    this.nioTransport.writeData(channel, buf -> getSerializer().writeTo(buf, "DISCONNECTED"));
                    this.nioTransport.writeFinish(channel);

                    selectionKey.attach(null);
                    // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                    selectionKey.channel().close();
                    selectionKey.cancel();
                    break;

                case CONNECT:
                    // FINISH-Frame lesen
                    this.nioTransport.readFrame(channel, buf -> {
                    });

                    this.nioTransport.writeData(channel, buf -> getSerializer().writeTo(buf, "CONNECTED"));
                    this.nioTransport.writeFinish(channel);
                    break;

                case SOURCE_CHECKSUM:
                    createChecksum(selectionKey, null, sender);
                    break;

                case SOURCE_CREATE_SYNC_ITEMS:
                    createSyncItems(channel, sender);
                    break;

                case SOURCE_READ_FILE:
                    readFile(selectionKey, null, sender);
                    break;

                case TARGET_CHECKSUM:
                    createChecksum(selectionKey, null, receiver);
                    break;

                case TARGET_CREATE_DIRECTORY:
                    createDirectory(selectionKey, null, receiver);
                    break;

                case TARGET_CREATE_SYNC_ITEMS:
                    createSyncItems(channel, receiver);
                    break;

                case TARGET_DELETE:
                    delete(selectionKey, null, receiver);
                    break;

                case TARGET_UPDATE:
                    update(selectionKey, null, receiver);
                    break;

                case TARGET_VALIDATE_FILE:
                    validate(selectionKey, null, receiver);
                    break;

                case TARGET_WRITE_FILE:
                    writeFile(selectionKey, null, receiver);
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
            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    /**
     * Die Daten werden zum Client gesendet.
     *
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @param sender {@link Sender}
     *
     * @throws Exception Falls was schief geht.
     */
    protected void readFile(final SelectionKey selectionKey, final ByteBuffer buffer, final Sender sender) throws Exception
    {
        // String baseDir = getSerializer().readFrom(buffer, String.class);
        // String relativeFile = getSerializer().readFrom(buffer, String.class);
        // long sizeOfFile = buffer.getLong();
        //
        // Exception exception = null;
        // WritableByteChannel outChannel = null;
        //
        // try
        // {
        // outChannel = (WritableByteChannel) selectionKey.channel();
        // fileHandle = sender.readFileHandle(baseDir, relativeFile, sizeOfFile);
        // }
        // catch (Exception ex)
        // {
        // exception = ex;
        // }
        //
        // if (exception != null)
        // {
        // getLogger().error(null, exception);
        //
        // try
        // {
        // // Exception senden.
        // Exception ex = exception;
        // JsyncServerResponse.error(buffer).write(selectionKey, buf -> getSerializer().writeTo(buf, ex, Exception.class));
        // }
        // catch (IOException ioex)
        // {
        // getLogger().error(null, ioex);
        // }
        // }
        // else
        // {
        // try (ReadableByteChannel inChannel = fileHandle.getHandle())
        // {
        // // Response senden
        // buffer.clear();
        // buffer.putInt(RemoteUtils.STATUS_OK); // Status
        // buffer.putLong(sizeOfFile); // Content-Length
        //
        // @SuppressWarnings("unused")
        // long totalWritten = 0;
        // long totalRead = 0;
        //
        // while (totalRead < sizeOfFile)
        // {
        // totalRead += inChannel.read(buffer);
        // buffer.flip();
        //
        // while (buffer.hasRemaining())
        // {
        // totalWritten += outChannel.write(buffer);
        //
        // getLogger().debug("ReadableByteChannel: sizeOfFile={}, totalRead={}", sizeOfFile, totalRead);
        // }
        //
        // buffer.clear();
        // }
        // }
        // catch (IOException ioex)
        // {
        // getLogger().error(null, ioex);
        // }
        // }
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
            receiver.validateFile(baseDir, syncItem, withChecksum, null);

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
    protected void writeFile(final SelectionKey selectionKey, final ByteBuffer buffer, final Receiver receiver) throws Exception
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
}
