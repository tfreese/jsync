// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.pool.AsynchronousSocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/***
 * {@link Sender} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class RemoteSenderAsync extends AbstractSender
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
            RemoteSenderAsync.this.channelPool.releaseChannel(this.delegate);
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
            Future<Integer> futureRead = this.delegate.read(dst);

            try
            {
                return futureRead.get();
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
    private final ByteBufferPool byteBufferPool = new ByteBufferPool();
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

        AsynchronousSocketChannel channel = this.channelPool.getChannel();
        ByteBuffer buffer = this.byteBufferPool.getBuffer();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.CONNECT);

        buffer.flip();
        write(channel, buffer);

        this.byteBufferPool.releaseBuffer(buffer);
        this.channelPool.releaseChannel(channel);

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
     * @see de.freese.jsync.filesystem.sender.Sender#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuffer buffer = this.byteBufferPool.getBuffer();

        Consumer<AsynchronousSocketChannel> disconnector = channel -> {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.DISCONNECT);

            buffer.flip();
            write(channel, buffer);
        };

        this.channelPool.clear(disconnector);

        this.byteBufferPool.releaseBuffer(buffer);
        this.byteBufferPool.clear();

        // AsynchronousSocketChannel channel = this.channelPool.getChannel();
        // ByteBuffer buffer = this.byteBufferPool.getBuffer();
        //
        // buffer.clear();
        // Serializers.writeTo(buffer, JSyncCommand.DISCONNECT);
        //
        // buffer.flip();
        // write(channel, buffer);
        //
        // this.byteBufferPool.releaseBuffer(buffer);
        // this.channelPool.releaseChannel(channel);
        //
        // this.byteBufferPool.clear();
        // this.channelPool.clear();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        AsynchronousSocketChannel channel = this.channelPool.getChannel();
        ByteBuffer buffer = this.byteBufferPool.getBuffer();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(channel, buffer);

            // Response lesen.
            buffer.clear();
            Future<Integer> futureResponse = channel.read(buffer);

            // while (getClient().read(bufferfer) > 0)
            while (futureResponse.get() > 0)
            {
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
                    consumerSyncItem.accept(syncItem);
                }

                buffer.clear();
                futureResponse = channel.read(buffer);
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
            this.byteBufferPool.releaseBuffer(buffer);
            this.channelPool.releaseChannel(channel);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        AsynchronousSocketChannel channel = this.channelPool.getChannel();
        ByteBuffer buffer = this.byteBufferPool.getBuffer();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(channel, buffer);

        this.byteBufferPool.releaseBuffer(buffer);

        return new NoCloseReadableByteChannel(channel);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        AsynchronousSocketChannel channel = this.channelPool.getChannel();
        ByteBuffer buffer = this.byteBufferPool.getBuffer();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(channel, buffer);

        buffer.clear();
        Future<Integer> futureResponse = channel.read(buffer);

        try
        {
            futureResponse.get();
        }
        catch (RuntimeException rex)
        {
            throw rex;
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        buffer.flip();
        String checksum = Serializers.readFrom(buffer, String.class);

        this.byteBufferPool.releaseBuffer(buffer);
        this.channelPool.releaseChannel(channel);

        return checksum;
    }

    /**
     * @return {@link Charset}
     */
    protected Charset getCharset()
    {
        return Options.CHARSET;
    }

    /**
     * @param channel {@link AsynchronousSocketChannel}
     * @param buffer {@link ByteBuffer}
     */
    protected void write(final AsynchronousSocketChannel channel, final ByteBuffer buffer)
    {
        Future<Integer> futureRequest = channel.write(buffer);

        try
        {
            while (futureRequest.get() > 0)
            {
                futureRequest = channel.write(buffer);
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
    }
}
