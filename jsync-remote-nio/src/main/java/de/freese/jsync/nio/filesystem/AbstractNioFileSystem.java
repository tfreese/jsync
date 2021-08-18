// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.filter.PathFilterTrue;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;
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
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.channelPool = new SocketChannelPool(uri);

        SocketChannel channel = getChannelPool().obtain();
        ByteBuffer buffer = ByteBufferPool.getInstance().obtain();

        try
        {
            getSerializer().writeTo(buffer, JSyncCommand.CONNECT);

            write(buffer, channel);

            long contentLength = readResponseHeader(buffer, channel);
            readResponseBody(buffer, channel, contentLength);

            // String message = getSerializer().readFrom(byteBuffer, String.class);
            // System.out.println(message);
        }
        catch (RuntimeException rex)
        {
            throw rex;
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
            ByteBufferPool.getInstance().free(buffer);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        SocketChannel channel = getChannelPool().obtain();
        ByteBuffer buffer = ByteBufferPool.getInstance().obtain();

        try
        {
            getSerializer().writeTo(buffer, JSyncCommand.DISCONNECT);

            write(buffer, channel);

            long contentLength = readResponseHeader(buffer, channel);
            readResponseBody(buffer, channel, contentLength);

            // String message = getSerializerDataBuffer().readFrom(buffer, String.class);
            // System.out.println(message);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            getChannelPool().free(channel);
            ByteBufferPool.getInstance().free(buffer);
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
        SocketChannel channel = getChannelPool().obtain();
        ByteBuffer buffer = ByteBufferPool.getInstance().obtain();

        try
        {
            getSerializer().writeTo(buffer, command);
            getSerializer().writeTo(buffer, baseDir);
            getSerializer().writeTo(buffer, followSymLinks);
            getSerializer().writeTo(buffer, pathFilter != null ? pathFilter : new PathFilterTrue());

            write(buffer, channel);

            long contentLength = readResponseHeader(buffer, channel);
            readResponseBody(buffer, channel, contentLength);

            int itemCount = getSerializer().readFrom(buffer, int.class);

            // while ((byteBuffer.limit() - byteBuffer.position()) > 0)
            for (int i = 0; i < itemCount; i++)
            {
                SyncItem syncItem = getSerializer().readFrom(buffer, SyncItem.class);
                consumerSyncItem.accept(syncItem);
            }
        }
        catch (RuntimeException rex)
        {
            throw rex;
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
            ByteBufferPool.getInstance().free(buffer);
        }
    }

    /**
     * @return {@link SocketChannelPool}
     */
    protected SocketChannelPool getChannelPool()
    {
        return this.channelPool;
    }

    /**
     * @return {@link Serializer}<ByteBuffer>
     */
    protected Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * Fertig lesen des Bodys.
     *
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     * @param contentLength long
     *
     * @return int
     *
     * @throws Exception Falls was schief geht.
     */
    protected int readResponseBody(final ByteBuffer buffer, final SocketChannel channel, final long contentLength) throws Exception
    {
        buffer.clear();

        int totalRead = channel.read(buffer);

        while (totalRead < contentLength)
        {
            totalRead += channel.read(buffer);
        }

        buffer.flip();

        return totalRead;
    }

    /**
     * Einlesen des Headers und ggf. der Exception.
     *
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     *
     * @return long
     *
     * @throws Exception Falls was schief geht.
     */
    protected long readResponseHeader(final ByteBuffer buffer, final SocketChannel channel) throws Exception
    {
        buffer.clear();

        // Auf keinen Fall mehr lesen als den Header: 12 Bytes
        ByteBuffer byteBufferHeader = buffer.slice(0, 12);

        channel.read(byteBufferHeader);
        byteBufferHeader.flip();

        int status = getSerializer().readFrom(byteBufferHeader, int.class);
        long contentLength = getSerializer().readFrom(byteBufferHeader, long.class);

        buffer.clear();

        if (RemoteUtils.STATUS_ERROR == status)
        {
            // Exception einlesen.
            int totalRead = channel.read(buffer);

            while (totalRead < contentLength)
            {
                int bytesRead = channel.read(buffer);
                totalRead += bytesRead;
            }

            buffer.flip();

            Exception exception = getSerializer().readFrom(buffer, Exception.class);

            throw exception;
        }

        return contentLength;
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @param channel {@link SocketChannel}
     *
     * @throws Exception Falls was schief geht.
     *
     * @return int, Bytes written
     */
    protected int write(final ByteBuffer buffer, final SocketChannel channel) throws Exception
    {
        if (buffer.position() > 0)
        {
            buffer.flip();
        }

        int totalWritten = 0;

        while (buffer.hasRemaining())
        {
            int bytesWritten = channel.write(buffer);

            totalWritten += bytesWritten;
        }

        return totalWritten;
    }
}
