// Created: 05.04.2018
package de.freese.jsync.nio.filesystem.receiver;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.buffer.DataBuffer;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import de.freese.jsync.utils.buffer.DataBufferAdapter;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * {@link Receiver} für NIO Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteReceiverNio extends AbstractReceiver implements RemoteSupport
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
    *
    */
    private final Serializer<DataBuffer> serializerDataBuffer = DefaultSerializer.of(new DataBufferAdapter());

    /**
     * Erzeugt eine neue Instanz von {@link RemoteReceiverNio}.
     */
    public RemoteReceiverNio()
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
            connect(channel);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            createDirectory(channel, baseDir, relativePath);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            delete(channel, baseDir, relativePath, followSymLinks);
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
        this.channelPool.destroy(channel -> disconnect(channel, getLogger()));

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
            generateSyncItems(channel, baseDir, followSymLinks, consumerSyncItem);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        SocketChannel channel = this.channelPool.get();

        return getWritableChannel(channel, this.channelPool::release, baseDir, relativeFile, sizeOfFile);
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
            return getChecksum(channel, baseDir, relativeFile, consumerBytesRead);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getResource(java.lang.String, java.lang.String, long)
     */
    @Override
    public WritableResource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    // /**
    // * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializerDataBuffer()
    // */
    // @Override
    // public Serializer<DataBuffer> getSerializerDataBuffer()
    // {
    // return this.serializerDataBuffer;
    // }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            update(channel, baseDir, syncItem);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            validateFile(channel, baseDir, syncItem, withChecksum);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void writeChunk(final String baseDir, final String relativeFile, final long position, final long sizeOfChunk, final ByteBuffer buffer)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            writeChunk(channel, baseDir, relativeFile, position, sizeOfChunk, buffer);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}
