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
import de.freese.jsync.utils.io.SharedByteArrayOutputStream;
import de.freese.jsync.utils.pool.AsynchronousSocketChannelPool;
import de.freese.jsync.utils.pool.ByteBufferPool;

/***
 * {@link Sender} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
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

            // Response lesen.
            buffer.clear();

            // Wegen Chunked-Data den Response erst mal sammeln.
            SharedByteArrayOutputStream sbaos = new SharedByteArrayOutputStream(1024);

            Future<Integer> futureResponse = channel.read(buffer);

            // while (getClient().read(bufferfer) > 0)
            while (futureResponse.get() > 0)
            {
                buffer.flip();

                while (buffer.remaining() > Serializers.getLengthOfEOL())
                {
                    sbaos.write(buffer, buffer.remaining() - Serializers.getLengthOfEOL());
                }

                if (Serializers.isEOL(buffer))
                {
                    buffer.clear();
                    break;
                }

                sbaos.write(buffer, buffer.remaining());

                buffer.clear();
                futureResponse = channel.read(buffer);
            }

            ByteBuffer bufferData = sbaos.toByteBuffer();

            while (bufferData.hasRemaining())
            {
                SyncItem syncItem = Serializers.readFrom(bufferData, SyncItem.class);
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
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();
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
        AsynchronousSocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

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

        this.byteBufferPool.release(buffer);
        this.channelPool.release(channel);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#readChunk(java.lang.String, java.lang.String, long, long, java.nio.ByteBuffer)
     */
    @Override
    public void readChunk(final String baseDir, final String relativeFile, final long position, final long size, final ByteBuffer bufferChunk)
    {
        AsynchronousSocketChannel channel = this.channelPool.get();
        ByteBuffer buffer = this.byteBufferPool.get();

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.READ_CHUNK);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);
        Serializers.writeTo(buffer, position);
        Serializers.writeTo(buffer, size);

        buffer.flip();
        write(channel, buffer);

        buffer.clear();
        bufferChunk.clear();

        try
        {
            while (bufferChunk.position() < size)
            {
                Future<Integer> futureResponse = channel.read(buffer);
                futureResponse.get();

                buffer.flip();
                bufferChunk.put(buffer);
                buffer.clear();
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

        this.byteBufferPool.release(buffer);
        this.channelPool.release(channel);
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
