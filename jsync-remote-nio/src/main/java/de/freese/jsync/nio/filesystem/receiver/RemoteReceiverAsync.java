// Created: 05.04.2018
package de.freese.jsync.nio.filesystem.receiver;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.neu.DefaultSerializer;
import de.freese.jsync.model.serializer.neu.Serializer;
import de.freese.jsync.model.serializer.neu.adapter.ByteBufferAdapter;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.nio.utils.pool.AsynchronousSocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * {@link Receiver} für NIO Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteReceiverAsync extends AbstractReceiver implements RemoteSupport
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
     *
     */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * Erzeugt eine neue Instanz von {@link RemoteReceiverAsync}.
     */
    public RemoteReceiverAsync()
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            createDirectory(baseDir, relativePath, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
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
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            delete(baseDir, relativePath, followSymLinks, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile, final long size)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        return getWritableChannel(baseDir, relativeFile, size, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get(), () -> channel,
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
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            update(baseDir, syncItem, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
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
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            validateFile(baseDir, syncItem, withChecksum, buffer -> write(channel, buffer), buffer -> channel.read(buffer).get());
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
    public void writeChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            writeChunk(baseDir, relativeFile, position, size, buffer, buf -> write(channel, buf), buf -> channel.read(buf).get());
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}
