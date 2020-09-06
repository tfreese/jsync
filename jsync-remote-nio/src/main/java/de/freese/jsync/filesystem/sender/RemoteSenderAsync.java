// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.filesystem.RemoteSupport;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.RemoteUtils;
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
     * @author Thomas Freese
     */
    private class NoCloseReadableByteChannel implements ReadableByteChannel
    {
        /**
         *
         */
        private final AsynchronousSocketChannel delegate;

        /**
         * Erstellt ein neues {@link NoCloseReadableByteChannel} Object.
         *
         * @param delegate {@link AsynchronousSocketChannel}
         */
        public NoCloseReadableByteChannel(final AsynchronousSocketChannel delegate)
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
            RemoteSenderAsync.this.channelPool.release(this.delegate);
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
            Future<Integer> future = this.delegate.read(dst);

            try
            {
                return future.get();
            }
            catch (InterruptedException | ExecutionException ex)
            {
                if (ex.getCause() instanceof IOException)
                {
                    throw (IOException) ex.getCause();
                }

                throw new IOException(ex.getCause());
            }
        }
    }

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
        ByteBuffer buffer = this.byteBufferPool.get();

        Consumer<AsynchronousSocketChannel> disconnector = channel -> {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.DISCONNECT);
            buffer.flip();

            try
            {
                write(channel, buffer);
            }
            catch (Exception ex)
            {
                getLogger().error(null, ex);
            }
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
        AsynchronousSocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
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
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String,long)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile, final long size)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativeFile);
            buffer.putLong(size);

            buffer.flip();
            write(channel, buffer);

            // Nur den Status auslesen.
            ByteBuffer byteBufferStatus = ByteBuffer.allocate(4);
            channel.read(byteBufferStatus).get();
            byteBufferStatus.flip();

            if (!RemoteUtils.isResponseOK(byteBufferStatus))
            {
                channel.read(buffer).get();
                buffer.flip();
                Exception exception = Serializers.readFrom(buffer, Exception.class);

                throw exception;
            }

            return new NoCloseReadableByteChannel(channel);
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
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
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
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer buffer)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.READ_CHUNK);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, relativeFile);
            buffer.putLong(position);
            buffer.putLong(size);

            buffer.flip();
            write(channel, buffer);
            buffer.clear();

            // Nur den Status auslesen.
            ByteBuffer byteBufferStatus = ByteBuffer.allocate(4);
            channel.read(byteBufferStatus).get();
            byteBufferStatus.flip();

            if (!RemoteUtils.isResponseOK(byteBufferStatus))
            {
                channel.read(buffer).get();
                Exception exception = Serializers.readFrom(buffer, Exception.class);

                throw exception;
            }

            while (buffer.position() < size)
            {
                channel.read(buffer).get();
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
            this.channelPool.release(channel);
        }
    }
}
