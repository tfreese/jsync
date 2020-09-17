// Created: 05.04.2018
package de.freese.jsync.nio.filesystem.receiver;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.receiver.AbstractReceiver;
import de.freese.jsync.filesystem.receiver.Receiver;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * {@link Receiver} für NIO Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteReceiverBlocking extends AbstractReceiver implements RemoteSupport
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
     * Erzeugt eine neue Instanz von {@link RemoteReceiverBlocking}.
     */
    public RemoteReceiverBlocking()
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            createDirectory(baseDir, relativePath, buffer -> write(channel, buffer), channel::read);
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
            delete(baseDir, relativePath, followSymLinks, buffer -> write(channel, buffer), channel::read);
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
        this.channelPool.destroy(channel -> disconnect(buffer -> write(channel, buffer), getLogger()));

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
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile, final long size)
    {
        SocketChannel channel = this.channelPool.get();

        return getWritableChannel(baseDir, relativeFile, size, buffer -> write(channel, buffer), channel::read, () -> channel, this.channelPool::release);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        SocketChannel channel = this.channelPool.get();

        try
        {
            update(baseDir, syncItem, buffer -> write(channel, buffer), channel::read);
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
            validateFile(baseDir, syncItem, withChecksum, buffer -> write(channel, buffer), channel::read);
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
        SocketChannel channel = this.channelPool.get();

        try
        {
            writeChunk(baseDir, relativeFile, position, size, buffer, buf -> write(channel, buf), channel::read);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}