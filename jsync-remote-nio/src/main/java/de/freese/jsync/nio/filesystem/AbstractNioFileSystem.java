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
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.transport.NioFrameProtocol;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;

/**
 * @author Thomas Freese
 */
public abstract class AbstractNioFileSystem extends AbstractFileSystem {
    private final NioFrameProtocol frameProtocol = new NioFrameProtocol();

    private final Serializer<ByteBuffer, ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    private SocketChannelPool channelPool;

    @Override
    public void connect(final URI uri) {
        this.channelPool = new SocketChannelPool(uri);

        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.CONNECT));

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
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.DISCONNECT));

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
        SocketChannel channel = getChannelPool().obtain();

        try {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, command));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer -> {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativeFile);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response
            return getFrameProtocol().readAll(channel).map(buffer -> {
                String value = getSerializer().readFrom(buffer, String.class);
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
            SocketChannel channel = getChannelPool().obtain();

            try {
                // MetaData-Frame
                getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, command));

                // Data-Frame
                getFrameProtocol().writeData(channel, buffer -> {
                    getSerializer().writeTo(buffer, baseDir);
                    getSerializer().writeTo(buffer, followSymLinks);
                    getSerializer().writeTo(buffer, pathFilter);
                });

                // Finish-Frame
                getFrameProtocol().writeFinish(channel);

                // Response
                getFrameProtocol().readAll(channel, buffer -> {
                    SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);

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
