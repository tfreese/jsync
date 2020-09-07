// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.RemoteSupport;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.utils.pool.AsynchronousSocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/***
 * {@link Sender} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteSenderAsync extends AbstractSender implements RemoteSupport
{
    /**
     *
     */
    private final ByteBufferPool byteBufferPool = ByteBufferPool.getInstance();

    /**
     *
     */
    private AsynchronousSocketChannelPool channelPool;

    /**
     * Erstellt ein neues {@link RemoteSenderAsync} Object.
     */
    public RemoteSenderAsync()
    {
        super();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.channelPool = new AsynchronousSocketChannelPool(uri, Executors.newCachedThreadPool());

        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            connect(buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#disconnect()
     */
    @Override
    public void disconnect()
    {
        this.channelPool.destroy(channel -> disconnect(buffer -> write(channel, buffer), getLogger()));

        this.byteBufferPool.clear();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            generateSyncItems(baseDir, followSymLinks, consumerSyncItem, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile, final long size)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        return getReadableChannel(baseDir, relativeFile, size, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get(), () -> channel,
                this.channelPool::release);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            return getChecksum(baseDir, relativeFile, consumerBytesRead, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            readChunk(baseDir, relativeFile, position, size, buffer, buf -> write(channel, buf), buf -> channel.read(buf).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}
