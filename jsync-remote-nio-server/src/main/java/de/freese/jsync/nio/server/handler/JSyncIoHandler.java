// Created: 04.11.2018
package de.freese.jsync.nio.server.handler;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

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
import de.freese.jsync.nio.transport.NioFrameProtocol;
import de.freese.jsync.serialisation.DefaultSerializer;
import de.freese.jsync.serialisation.Serializer;
import de.freese.jsync.serialisation.io.ByteBufferReader;
import de.freese.jsync.serialisation.io.ByteBufferWriter;
import de.freese.jsync.utils.pool.Pool;

/**
 * @author Thomas Freese
 */
public class JSyncIoHandler implements IoHandler<SelectionKey> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncIoHandler.class);

    private static final Pool<Receiver> POOL_RECEIVER = new Pool<>(true, true) {
        @Override
        protected Receiver create() {
            return new ReceiverDelegateLogger(new LocalhostReceiver());
        }
    };

    private static final Pool<Sender> POOL_SENDER = new Pool<>(true, true) {
        @Override
        protected Sender create() {
            return new SenderDelegateLogger(new LocalhostSender());
        }
    };

    private final NioFrameProtocol frameProtocol = new NioFrameProtocol();

    private final Serializer<ByteBuffer, ByteBuffer> serializer = new DefaultSerializer<>(new ByteBufferReader(), new ByteBufferWriter());

    @Override
    public void read(final SelectionKey selectionKey) {
        final Sender sender = POOL_SENDER.obtain();
        final Receiver receiver = POOL_RECEIVER.obtain();

        try {
            final SocketChannel channel = (SocketChannel) selectionKey.channel();

            final ByteBuffer buffer = frameProtocol.readFrame(channel);

            final JSyncCommand command = getSerializer().readJSyncCommand(buffer);

            frameProtocol.getBufferPool().free(buffer);

            getLogger().debug("{}: read command: {}", getRemoteAddress(selectionKey), command);

            if (command == null) {
                getLogger().error("unknown JSyncCommand");
                selectionKey.interestOps(SelectionKey.OP_READ);
                return;
            }

            getLogger().debug("{}", frameProtocol.getBufferPool());

            switch (command) {
                case DISCONNECT -> {
                    // FINISH-Frame
                    frameProtocol.readFrame(channel);
                    frameProtocol.writeData(channel, buf -> getSerializer().writeString(buf, "DISCONNECTED"));
                    frameProtocol.writeFinish(channel);
                    selectionKey.attach(null);
                    // selectionKey.interestOps(SelectionKey.OP_CONNECT);
                    selectionKey.channel().close();
                    selectionKey.cancel();
                }
                case CONNECT -> {
                    // FINISH-Frame
                    frameProtocol.readFrame(channel);
                    frameProtocol.writeData(channel, buf -> getSerializer().writeString(buf, "CONNECTED"));
                    frameProtocol.writeFinish(channel);
                }
                case SOURCE_CHECKSUM -> createChecksum(channel, sender);
                case SOURCE_CREATE_SYNC_ITEMS -> createSyncItems(channel, sender);
                case SOURCE_READ_FILE -> readFile(channel, sender);
                case TARGET_CHECKSUM -> createChecksum(channel, receiver);
                case TARGET_CREATE_DIRECTORY -> createDirectory(channel, receiver);
                case TARGET_CREATE_SYNC_ITEMS -> createSyncItems(channel, receiver);
                case TARGET_DELETE -> delete(channel, receiver);
                case TARGET_UPDATE -> update(channel, receiver);
                case TARGET_VALIDATE_FILE -> validate(channel, receiver);
                case TARGET_WRITE_FILE -> writeFile(channel, receiver);
                default -> {
                    // Empty
                }
            }

            if (selectionKey.isValid()) {
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
        finally {
            POOL_SENDER.free(sender);
            POOL_RECEIVER.free(receiver);
        }
    }

    @Override
    public void write(final SelectionKey selectionKey) {
        try {
            if (selectionKey.attachment() instanceof Runnable task) {
                selectionKey.attach(null);

                task.run();
            }

            selectionKey.interestOps(SelectionKey.OP_READ);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    /**
     * Create the checksum.
     */
    protected void createChecksum(final SocketChannel channel, final FileSystem fileSystem) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final String relativeFile = getSerializer().readString(buffer);

            final LongConsumer consumer = checksumBytesRead -> {
                try {
                    frameProtocol.writeData(channel, buf -> getSerializer().writeString(buf, Long.toString(checksumBytesRead)));
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };

            final String checksum = fileSystem.generateChecksum(baseDir, relativeFile, consumer);

            frameProtocol.writeData(channel, buf -> getSerializer().writeString(buf, checksum));
            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (Exception ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void createDirectory(final SocketChannel channel, final Receiver receiver) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final String relativePath = getSerializer().readString(buffer);

            receiver.createDirectory(baseDir, relativePath);

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void createSyncItems(final SocketChannel channel, final FileSystem fileSystem) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final boolean followSymLinks = getSerializer().readBoolean(buffer);
            final PathFilter pathFilter = getSerializer().readPathFilter(buffer);

            fileSystem.generateSyncItems(baseDir, followSymLinks, pathFilter).subscribe(syncItem -> {
                try {
                    frameProtocol.writeData(channel, buf -> getSerializer().write(buf, syncItem));
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (Exception ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void delete(final SocketChannel channel, final Receiver receiver) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final String relativePath = getSerializer().readString(buffer);
            final boolean followSymLinks = getSerializer().readBoolean(buffer);

            receiver.delete(baseDir, relativePath, followSymLinks);

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    protected String getRemoteAddress(final SelectionKey selectionKey) {
        try {
            return ((SocketChannel) selectionKey.channel()).getRemoteAddress().toString();
        }
        catch (IOException _) {
            return "";
        }
    }

    protected Serializer<ByteBuffer, ByteBuffer> getSerializer() {
        return serializer;
    }

    protected void readFile(final SocketChannel channel, final Sender sender) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final String relativeFile = getSerializer().readString(buffer);
            final long sizeOfFile = getSerializer().readLong(buffer);

            sender.readFile(baseDir, relativeFile, sizeOfFile).subscribe(buf -> {
                try {
                    frameProtocol.writeData(channel, buf);
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
                finally {
                    frameProtocol.getBufferPool().free(buf);
                }
            });

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void update(final SocketChannel channel, final Receiver receiver) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final SyncItem syncItem = getSerializer().readSyncItem(buffer);

            receiver.update(baseDir, syncItem);

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void validate(final SocketChannel channel, final Receiver receiver) {
        final ByteBuffer buffer = frameProtocol.readAll(channel).blockFirst();

        try {
            final String baseDir = getSerializer().readString(buffer);
            final SyncItem syncItem = getSerializer().readSyncItem(buffer);
            final boolean withChecksum = getSerializer().readBoolean(buffer);

            final LongConsumer consumer = checksumBytesRead -> {
                try {
                    frameProtocol.writeData(channel, buf -> getSerializer().writeLong(buf, checksumBytesRead));
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            };

            receiver.validateFile(baseDir, syncItem, withChecksum, consumer);

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }

    protected void writeFile(final SocketChannel channel, final Receiver receiver) throws Exception {
        final ByteBuffer buffer = frameProtocol.readFrame(channel);

        try {
            final String baseDir = getSerializer().readString(buffer);
            final String relativeFile = getSerializer().readString(buffer);
            final long sizeOfFile = getSerializer().readLong(buffer);

            final Flux<ByteBuffer> data = frameProtocol.readAll(channel);

            receiver.writeFile(baseDir, relativeFile, sizeOfFile, data).subscribe(bytesWritten -> {
                try {
                    frameProtocol.writeData(channel, buf -> getSerializer().writeLong(buf, bytesWritten));
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });

            frameProtocol.writeFinish(channel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);

            try {
                frameProtocol.writeError(channel, buf -> getSerializer().write(buf, ex));
            }
            catch (IOException ex2) {
                getLogger().error(ex2.getMessage(), ex2);
            }
        }
        finally {
            frameProtocol.getBufferPool().free(buffer);
        }
    }
}
