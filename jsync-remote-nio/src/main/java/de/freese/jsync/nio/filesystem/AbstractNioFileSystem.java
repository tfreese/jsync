// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.nio.transport.NioFrameProtocol;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import de.freese.jsync.serialisation.DefaultSerializer;
import de.freese.jsync.serialisation.Serializer;
import de.freese.jsync.serialisation.io.ByteBufferReader;
import de.freese.jsync.serialisation.io.ByteBufferWriter;

/**
 * @author Thomas Freese
 */
public abstract class AbstractNioFileSystem extends AbstractFileSystem {
    private final NioFrameProtocol frameProtocol = new NioFrameProtocol();
    private final Serializer<ByteBuffer, ByteBuffer> serializer = new DefaultSerializer<>(new ByteBufferReader(), new ByteBufferWriter());

    private SocketChannelPool channelPool;

    @Override
    public void connect(final URI uri) {
        this.channelPool = new SocketChannelPool(uri);

        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.CONNECT));

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).doFinally(signal -> getLogger().info("client connected")).subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    @Override
    public void disconnect() {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, JSyncCommand.DISCONNECT));

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            getFrameProtocol().readAll(channel).doFinally(signal -> getLogger().info("client disconnected")).subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
        finally {
            getChannelPool().free(channel);

            this.channelPool.clear();
            this.channelPool = null;

            this.frameProtocol.getBufferPool().clear();
        }
    }

    protected String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead, final JSyncCommand command) {
        final SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, command));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeString(buffer, baseDir);
                getSerializer().writeString(buffer, relativeFile);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            return getFrameProtocol().readAll(channel).map(buffer -> {
                final String value = getSerializer().readString(buffer);
                getFrameProtocol().getBufferPool().free(buffer);

                return value;
            }).doOnNext(value -> {
                if (PATTERN_NUMBER.matcher(value).matches()) {
                    consumerChecksumBytesRead.accept(Long.parseLong(value));
                }
            }).blockLast();
        }
        catch (RuntimeException ex) {
            throw ex;
        }
        catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        finally {
            getChannelPool().free(channel);
        }
    }

    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter, final JSyncCommand command) {
        return Flux.create(sink -> {
            final SocketChannel channel = getChannelPool().obtain();

            try {
                // MetaData-Frame
                getFrameProtocol().writeData(channel, buffer -> getSerializer().write(buffer, command));

                // Data-Frame
                getFrameProtocol().writeData(channel, buffer -> {
                    getSerializer().writeString(buffer, baseDir);
                    getSerializer().writeBoolean(buffer, followSymLinks);
                    getSerializer().write(buffer, pathFilter);
                });

                // Finish-Frame
                getFrameProtocol().writeFinish(channel);

                // Response
                getFrameProtocol().readAll(channel, buffer -> {
                    final SyncItem syncItem = getSerializer().readSyncItem(buffer);

                    sink.next(syncItem);

                    getFrameProtocol().getBufferPool().free(buffer);
                });
            }
            catch (Exception ex) {
                sink.error(ex);
            }
            finally {
                getChannelPool().free(channel);

                sink.complete();
            }
        });
    }

    protected SocketChannelPool getChannelPool() {
        return this.channelPool;
    }

    protected NioFrameProtocol getFrameProtocol() {
        return this.frameProtocol;
    }

    protected Serializer<ByteBuffer, ByteBuffer> getSerializer() {
        return this.serializer;
    }
}
