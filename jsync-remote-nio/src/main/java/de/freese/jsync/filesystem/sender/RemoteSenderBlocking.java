// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.Options;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.RemoteUtils;
import de.freese.jsync.utils.io.SharedByteArrayOutputStream;
import de.freese.jsync.utils.pool.ByteBufferPool;
import de.freese.jsync.utils.pool.SocketChannelPool;

/**
 * {@link Sender} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteSenderBlocking extends AbstractSender
{
    /**
     * @author Thomas Freese
     */
    private class NoCloseReadableByteChannel implements ReadableByteChannel
    {
        /**
         *
         */
        private final ReadableByteChannel delegate;

        /**
         * Erstellt ein neues {@link NoCloseReadableByteChannel} Object.
         *
         * @param delegate {@link ReadableByteChannel}
         */
        public NoCloseReadableByteChannel(final ReadableByteChannel delegate)
        {
            super();

            this.delegate = Objects.requireNonNull(delegate, "delegate required");
        }

        /**
         * @see java.nio.channels.Channel#close()
         */
        @Override
        public void close() throws IOException
        {
            RemoteSenderBlocking.this.channelPool.release((SocketChannel) this.delegate);
        }

        /**
         * @see java.nio.channels.Channel#isOpen()
         */
        @Override
        public boolean isOpen()
        {
            return this.delegate.isOpen();
        }

        /**
         * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
         */
        @Override
        public int read(final ByteBuffer dst) throws IOException
        {
            return this.delegate.read(dst);
        }
    }

    /**
     *
     */
    private final ByteBufferPool byteBufferPool = ByteBufferPool.getInstance();
    /**
     *
     */
    private SocketChannelPool channelPool;

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
        ByteBuffer buffer = this.byteBufferPool.get();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.CONNECT);

        buffer.flip();
        write(channel, buffer);

        this.byteBufferPool.release(buffer);
        this.channelPool.release(channel);

        // Warum funktioniert die weitere Kommunikation nur mit dem Thread.sleep ???
        try
        {
            Thread.sleep(10);
        }
        catch (Exception ex)
        {
            // Empty
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuffer buffer = this.byteBufferPool.get();

        Consumer<SocketChannel> disconnector = channel ->
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.DISCONNECT);

            buffer.flip();
            write(channel, buffer);
        };

        this.channelPool.destroy(disconnector);

        this.byteBufferPool.release(buffer);
        this.byteBufferPool.clear();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            buffer.clear();

            // Wegen Chunked-Data den Response erst mal sammeln.
            SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

            while (channel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.remaining() > RemoteUtils.getLengthOfEOL())
                {
                    sbaos.write(buffer, buffer.remaining() - RemoteUtils.getLengthOfEOL());
                }

                if (RemoteUtils.isEOL(buffer))
                {
                    break;
                }

                sbaos.write(buffer, buffer.remaining());

                buffer.clear();
            }

            ByteBuffer bufferData = sbaos.toByteBuffer();

            if (!RemoteUtils.isResponseOK(buffer))
            {
                Exception exception = Serializers.readFrom(buffer, Exception.class);

                throw exception;
            }
            else
            {
                int itemCount = bufferData.getInt();

                while (bufferData.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(bufferData, SyncItem.class);
                    consumerSyncItem.accept(syncItem);
                }
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
            this.byteBufferPool.release(buffer);
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(channel, buffer);

        this.byteBufferPool.release(buffer);

        return new NoCloseReadableByteChannel(channel);
    }

    /**
     * @return {@link Charset}
     */
    protected Charset getCharset()
    {
        return Options.CHARSET;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativeFile);

            buffer.flip();
            write(channel, buffer);

            buffer.clear();

            // Wegen Chunked-Data den Response erst mal sammeln.
            SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

            while (channel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.remaining() > RemoteUtils.getLengthOfEOL())
                {
                    sbaos.write(buffer, buffer.remaining() - RemoteUtils.getLengthOfEOL());
                }

                if (RemoteUtils.isEOL(buffer))
                {
                    break;
                }

                sbaos.write(buffer, buffer.remaining());

                buffer.clear();
            }

            ByteBuffer bufferData = sbaos.toByteBuffer();

            if (!RemoteUtils.isResponseOK(buffer))
            {
                Exception exception = Serializers.readFrom(buffer, Exception.class);

                throw exception;
            }
            else
            {
                String checksum = Serializers.readFrom(buffer, String.class);

                return checksum;
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
            this.byteBufferPool.release(buffer);
            this.channelPool.release(channel);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer  {@link ByteBuffer}
     */
    protected void read(final SocketChannel channel, final ByteBuffer buffer)
    {
        try
        {
            channel.read(buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        SocketChannel channel = this.channelPool.get();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.READ_CHUNK);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);
        buffer.putLong(position);
        buffer.putLong(size);

        buffer.flip();
        write(channel, buffer);

        buffer.clear();

        while (buffer.position() < size)
        {
            read(channel, buffer);
        }

        this.channelPool.release(channel);
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer  {@link ByteBuffer}
     */
    protected void write(final SocketChannel channel, final ByteBuffer buffer)
    {
        try
        {
            while (buffer.hasRemaining())
            {
                channel.write(buffer);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
