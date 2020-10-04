// Created: 18.11.2018
package de.freese.jsync.nio.filesystem.sender;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * {@link Sender} für NIO Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteSenderBlocking extends AbstractSender implements RemoteSupport
{
    /**
     *
     */
    private final ByteBufferPool byteBufferPool = ByteBufferPool.getInstance();

    /**
     *
     */
    private SocketChannelPool channelPool;

    /**
     *
     */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Erstellt ein neues {@link RemoteSenderBlocking} Object.
     */
    public RemoteSenderBlocking()
    {
        super();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.channelPool = new SocketChannelPool(uri);

        SocketChannel channel = this.channelPool.get();

        try
        {
            connect(buffer -> write(channel, buffer), channel::read);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        this.channelPool.destroy(channel -> disconnect(buffer -> write(channel, buffer), channel::read, getLogger()));

        this.byteBufferPool.clear();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            generateSyncItems(baseDir, followSymLinks, consumerSyncItem, buffer -> write(channel, buffer), channel::read);
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
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        SocketChannel channel = this.channelPool.get();

        return getReadableChannel(baseDir, relativeFile, sizeOfFile, buffer -> write(channel, buffer), channel::read, () -> channel, this.channelPool::release);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            return getChecksum(baseDir, relativeFile, consumerBytesRead, buffer -> write(channel, buffer), channel::read);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer buffer)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            readChunk(baseDir, relativeFile, position, sizeOfChunk, buffer, buf -> write(channel, buf), channel::read);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}
