// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;
import de.freese.jsync.utils.pool.SocketChannelPool;

/**
 * {@link Receiver} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
public class RemoteReceiverBlocking extends AbstractReceiver
{
    /**
     * @author Thomas Freese
     */
    private class NoCloseWritableByteChannel implements WritableByteChannel
    {
        /**
         *
         */
        private final WritableByteChannel delegate;

        /**
         * Erstellt ein neues {@link NoCloseWritableByteChannel} Object.
         *
         * @param delegate {@link WritableByteChannel}
         */
        public NoCloseWritableByteChannel(final WritableByteChannel delegate)
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
            RemoteReceiverBlocking.this.channelPool.release((SocketChannel) this.delegate);
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
         * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
         */
        @Override
        public int write(final ByteBuffer src) throws IOException
        {
            int totalWritten = 0;

            while (src.hasRemaining())
            {
                totalWritten += this.delegate.write(src);
            }

            // return this.delegate.write(src);
            return totalWritten;
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
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.CONNECT);

            buffer.flip();
            write(channel, buffer);

            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativePath);

            buffer.flip();
            write(channel, buffer);

            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.TARGET_DELETE);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativePath);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuffer buffer = this.byteBufferPool.get();

        Consumer<SocketChannel> disconnector = channel -> {
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
            Serializers.writeTo(buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
            }

            @SuppressWarnings("unused")
            int itemCount = byteBufferResponse.getInt();

            while (byteBufferResponse.hasRemaining())
            {
                SyncItem syncItem = Serializers.readFrom(byteBufferResponse, SyncItem.class);
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
            this.byteBufferPool.release(buffer);
            this.channelPool.release(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(channel, buffer);

        this.byteBufferPool.release(buffer);

        return new NoCloseWritableByteChannel(channel);
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
            Serializers.writeTo(buffer, JSyncCommand.TARGET_CHECKSUM);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativeFile);

            buffer.flip();
            write(channel, buffer);

            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
            }

            String checksum = Serializers.readFrom(byteBufferResponse, String.class);

            return checksum;
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
     * @param buffer {@link ByteBuffer}
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.TARGET_UPDATE);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, syncItem);

            buffer.flip();
            write(channel, buffer);

            // Response auslesen.
            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.TARGET_VALIDATE_FILE);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, syncItem);
            Serializers.writeTo(buffer, withChecksum);

            buffer.flip();
            write(channel, buffer);

            // Response auslesen.
            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
     * @param buffer {@link ByteBuffer}
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

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#writeChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void writeChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        SocketChannel channel = this.channelPool.get();
        ByteBuffer bufferCmd = this.byteBufferPool.get();

        try
        {
            bufferCmd.clear();
            Serializers.writeTo(bufferCmd, JSyncCommand.WRITE_CHUNK);
            Serializers.writeTo(bufferCmd, baseDir);
            Serializers.writeTo(bufferCmd, relativeFile);
            bufferCmd.putLong(position);
            bufferCmd.putLong(size);

            bufferCmd.flip();
            write(channel, bufferCmd);

            buffer.flip();
            write(channel, buffer);

            // Response auslesen.
            ByteBuffer byteBufferResponse = RemoteUtils.readUntilEOL(channel);

            if (!RemoteUtils.isResponseOK(byteBufferResponse))
            {
                Exception exception = Serializers.readFrom(byteBufferResponse, Exception.class);

                throw exception;
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
            this.byteBufferPool.release(bufferCmd);
            this.channelPool.release(channel);
        }
    }
}
