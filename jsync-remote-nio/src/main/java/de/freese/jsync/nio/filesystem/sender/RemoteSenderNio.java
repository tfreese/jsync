// Created: 18.11.2018
package de.freese.jsync.nio.filesystem.sender;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import org.springframework.core.io.Resource;
import de.freese.jsync.filesystem.RemoteSenderResource;
import de.freese.jsync.filesystem.sender.AbstractSender;
import de.freese.jsync.filesystem.sender.Sender;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.nio.utils.pool.SocketChannelPool;

/**
 * {@link Sender} für NIO Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteSenderNio extends AbstractSender implements RemoteSupport
{
    /**
     *
     */
    private SocketChannelPool channelPool;

    /**
     * Erstellt ein neues {@link RemoteSenderNio} Object.
     */
    public RemoteSenderNio()
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
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        this.channelPool.destroy(channel -> disconnect(channel, getLogger()));
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
     * @see de.freese.jsync.filesystem.FileSystem#getResource(java.lang.String, java.lang.String, long)
     */
    @Override
    public Resource getResource(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        SocketChannel channel = this.channelPool.get();

        ReadableByteChannel readableByteChannel = getReadableChannel(channel, this.channelPool::release, baseDir, relativeFile, sizeOfFile);

        try
        {
            return new RemoteSenderResource(baseDir + "/" + relativeFile, sizeOfFile, readableByteChannel);
        }
        finally
        {
            // Channel wird im NoCloseReadableByteChannel freigegeben.
            // this.channelPool.release(channel);
        }
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
            readChunk(channel, baseDir, relativeFile, position, sizeOfChunk, buffer);
        }
        finally
        {
            this.channelPool.release(channel);
        }
    }
}
