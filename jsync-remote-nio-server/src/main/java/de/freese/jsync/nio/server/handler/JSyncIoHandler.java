// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

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
import de.freese.jsync.nio.transport.NioFrameProtocol;
import de.freese.jsync.utils.pool.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

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
     *
     */
    private final NioFrameProtocol frameProtocol = new NioFrameProtocol();
    /**
     *
     */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

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

            ByteBuffer buffer = this.frameProtocol.readFrame(channel);

            JSyncCommand command = getSerializer().readFrom(buffer, JSyncCommand.class);

            this.frameProtocol.getBufferPool().free(buffer);

            getLogger().debug("{}: read command: {}", getRemoteAddress(selectionKey), command);

            if (command == null)
            {
                getLogger().error("unknown JSyncCommand");
                selectionKey.interestOps(SelectionKey.OP_READ);
                return;
            }

            getLogger().debug("{}", this.frameProtocol.getBufferPool());

            switch (command)
            {
                case DISCONNECT:
                    // FINISH-Frame lesen
                    this.frameProtocol.readFrame(channel);

                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, "DISCONNECTED"));
                    this.frameProtocol.writeFinish(channel);

                    selectionKey.attach(null);
                    // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                    selectionKey.channel().close();
                    selectionKey.cancel();

                    break;

                case CONNECT:
                    // FINISH-Frame lesen
                    this.frameProtocol.readFrame(channel);

                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, "CONNECTED"));
                    this.frameProtocol.writeFinish(channel);
                    break;

                case SOURCE_CHECKSUM:
                    createChecksum(channel, sender);
                    break;

                case SOURCE_CREATE_SYNC_ITEMS:
                    createSyncItems(channel, sender);
                    break;

                case SOURCE_READ_FILE:
                    readFile(channel, sender);
                    break;

                case TARGET_CHECKSUM:
                    createChecksum(channel, receiver);
                    break;

                case TARGET_CREATE_DIRECTORY:
                    createDirectory(channel, receiver);
                    break;

                case TARGET_CREATE_SYNC_ITEMS:
                    createSyncItems(channel, receiver);
                    break;

                case TARGET_DELETE:
                    delete(channel, receiver);
                    break;

                case TARGET_UPDATE:
                    update(channel, receiver);
                    break;

                case TARGET_VALIDATE_FILE:
                    validate(channel, receiver);
                    break;

                case TARGET_WRITE_FILE:
                    writeFile(channel, receiver);
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
     * @see de.freese.jsync.nio.server.handler.IoHandler#write(java.lang.Object)
     */
    @Override
    public void write(final SelectionKey selectionKey)
    {
        try
        {
            if (selectionKey.attachment() instanceof Runnable task)
            {
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
     * Create the checksum.
     *
     * @param channel {@link SocketChannel}
     * @param fileSystem {@link FileSystem}
     */
    protected void createChecksum(final SocketChannel channel, final FileSystem fileSystem)
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            String relativeFile = getSerializer().readFrom(buffer, String.class);

            LongConsumer consumer = checksumBytesRead ->
            {
                try
                {
                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, Long.toString(checksumBytesRead)));
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
            };

            String checksum = fileSystem.generateChecksum(baseDir, relativeFile, consumer);

            this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, checksum));
            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (Exception ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }

    /**
     * Create the Directory.
     *
     * @param channel {@link SocketChannel}
     * @param receiver {@link Receiver}
     */
    protected void createDirectory(final SocketChannel channel, final Receiver receiver)
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            String relativePath = getSerializer().readFrom(buffer, String.class);

            receiver.createDirectory(baseDir, relativePath);

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
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
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            boolean followSymLinks = getSerializer().readFrom(buffer, Boolean.class);
            PathFilter pathFilter = getSerializer().readFrom(buffer, PathFilter.class);

            fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).subscribe(syncItem ->
            {
                try
                {
                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, syncItem));
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
            });

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (Exception ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }

    /**
     * Delete Directory or File.
     *
     * @param channel {@link SocketChannel}
     * @param receiver {@link Receiver}
     */
    protected void delete(final SocketChannel channel, final Receiver receiver)
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            String relativePath = getSerializer().readFrom(buffer, String.class);
            boolean followSymLinks = getSerializer().readFrom(buffer, Boolean.class);

            receiver.delete(baseDir, relativePath, followSymLinks);

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
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
     * Die Daten werden zum Client gesendet.
     *
     * @param channel {@link SocketChannel}
     * @param sender {@link Sender}
     *
     * @throws Exception Falls was schiefgeht.
     */
    protected void readFile(final SocketChannel channel, final Sender sender) throws Exception
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            String relativeFile = getSerializer().readFrom(buffer, String.class);
            long sizeOfFile = getSerializer().readFrom(buffer, Long.class);

            sender.readFile(baseDir, relativeFile, sizeOfFile).subscribe(buf ->
            {
                try
                {
                    this.frameProtocol.writeData(channel, buf);
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
                finally
                {
                    this.frameProtocol.getBufferPool().free(buf);
                }
            });

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }

    /**
     * Update Directory or File.
     *
     * @param channel {@link SocketChannel}
     * @param receiver {@link Receiver}
     */
    protected void update(final SocketChannel channel, final Receiver receiver)
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);

            receiver.update(baseDir, syncItem);

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }

    /**
     * Validate Directory or File.
     *
     * @param channel {@link SocketChannel}
     * @param receiver {@link Receiver}
     */
    protected void validate(final SocketChannel channel, final Receiver receiver)
    {
        ByteBuffer buffer = this.frameProtocol.readAll(channel).blockFirst();

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);
            boolean withChecksum = getSerializer().readFrom(buffer, Boolean.class);

            LongConsumer consumer = checksumBytesRead ->
            {
                try
                {
                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, checksumBytesRead));
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
            };

            receiver.validateFile(baseDir, syncItem, withChecksum, consumer);

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }

    /**
     * Die Daten werden zum Server gesendet.
     *
     * @param channel {@link SocketChannel}
     * @param receiver {@link Receiver}
     *
     * @throws Exception Falls was schiefgeht.
     */
    protected void writeFile(final SocketChannel channel, final Receiver receiver) throws Exception
    {
        ByteBuffer buffer = this.frameProtocol.readFrame(channel);

        try
        {
            String baseDir = getSerializer().readFrom(buffer, String.class);
            String relativeFile = getSerializer().readFrom(buffer, String.class);
            long sizeOfFile = getSerializer().readFrom(buffer, Long.class);

            Flux<ByteBuffer> data = this.frameProtocol.readAll(channel);

            receiver.writeFile(baseDir, relativeFile, sizeOfFile, data).subscribe(bytesWritten ->
            {
                try
                {
                    this.frameProtocol.writeData(channel, buf -> getSerializer().writeTo(buf, bytesWritten));
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
            });

            this.frameProtocol.writeFinish(channel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);

            try
            {
                this.frameProtocol.writeError(channel, buf -> getSerializer().writeTo(buf, ex));
            }
            catch (IOException ex2)
            {
                getLogger().error(null, ex2);
            }
        }
        finally
        {
            this.frameProtocol.getBufferPool().free(buffer);
        }
    }
}
