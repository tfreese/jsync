// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.transport.NioFrameProtocol;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public abstract class AbstractNioFileSystem extends AbstractFileSystem
{
    /**
     *
     */
    private SocketChannelPool channelPool;
    /**
     *
     */
    private final NioFrameProtocol frameProtocol = new NioFrameProtocol();
    /**
     *
     */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.channelPool = new SocketChannelPool(uri);

        SocketChannel channel = getChannelPool().obtain();

        try
        {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.CONNECT));

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response lesen
            getFrameProtocol().readAll(channel).doFinally(signal -> getLogger().info("client connected"))
                    .subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            getChannelPool().free(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        SocketChannel channel = getChannelPool().obtain();

        try
        {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, JSyncCommand.DISCONNECT));

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response lesen
            getFrameProtocol().readAll(channel).doFinally(signal -> getLogger().info("client disconnected"))
                    .subscribe(buffer -> getFrameProtocol().getBufferPool().free(buffer));
        }
        catch (Exception ex)
        {
            getLogger().error(ex.getMessage(), ex);
        }
        finally
        {
            getChannelPool().free(channel);

            this.channelPool.clear();
            this.channelPool = null;

            this.frameProtocol.getBufferPool().clear();
        }
    }

    /**
     * @param baseDir String
     * @param relativeFile String
     * @param consumerChecksumBytesRead {@link LongConsumer}
     * @param command {@link JSyncCommand}
     *
     * @return String
     */
    protected String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead, final JSyncCommand command)
    {
        SocketChannel channel = getChannelPool().obtain();

        try
        {
            // MetaData-Frame
            getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, command));

            // Data-Frame
            getFrameProtocol().writeData(channel, buffer ->
            {
                getSerializer().writeTo(buffer, baseDir);
                getSerializer().writeTo(buffer, relativeFile);
            });

            // Finish-Frame
            getFrameProtocol().writeFinish(channel);

            // Response lesen
            return getFrameProtocol().readAll(channel).map(buffer ->
            {
                String value = getSerializer().readFrom(buffer, String.class);
                getFrameProtocol().getBufferPool().free(buffer);

                return value;
            }).doOnNext(value ->
            {
                if (PATTERN_NUMBER.matcher(value).matches())
                {
                    consumerChecksumBytesRead.accept(Long.parseLong(value));
                }
            }).blockLast();
        }
        catch (RuntimeException ex)
        {
            throw ex;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            getChannelPool().free(channel);
        }
    }

    /**
     * @param baseDir String
     * @param followSymLinks boolean
     * @param pathFilter {@link PathFilter}
     * @param command {@link JSyncCommand}
     *
     * @return {@link Flux}
     */
    protected Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter, final JSyncCommand command)
    {
        return Flux.create(sink ->
        {
            SocketChannel channel = getChannelPool().obtain();

            try
            {
                // MetaData-Frame
                getFrameProtocol().writeData(channel, buffer -> getSerializer().writeTo(buffer, command));

                // Data-Frame
                getFrameProtocol().writeData(channel, buffer ->
                {
                    getSerializer().writeTo(buffer, baseDir);
                    getSerializer().writeTo(buffer, followSymLinks);
                    getSerializer().writeTo(buffer, pathFilter);
                });

                // Finish-Frame
                getFrameProtocol().writeFinish(channel);

                // Response lesen
                getFrameProtocol().readAll(channel, buffer ->
                {
                    SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);

                    sink.next(syncItem);

                    getFrameProtocol().getBufferPool().free(buffer);
                });
            }
            catch (Exception ex)
            {
                sink.error(ex);
            }
            finally
            {
                getChannelPool().free(channel);

                sink.complete();
            }
        });
    }

    /**
     * @return {@link SocketChannelPool}
     */
    protected SocketChannelPool getChannelPool()
    {
        return this.channelPool;
    }

    /**
     * @return {@link NioFrameProtocol}
     */
    protected NioFrameProtocol getFrameProtocol()
    {
        return this.frameProtocol;
    }

    /**
     * @return {@link Serializer}<ByteBuffer>
     */
    protected Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }
}
