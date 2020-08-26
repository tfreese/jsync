// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.utils.JSyncUtils;

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
            // Empty
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
    private AsynchronousSocketChannel asyncSocketChannel;

    /**
    *
    */
    private AsynchronousChannelGroup channelGroup;

    /**
    *
    */
    private final ExecutorService executorService;

    /**
     * Erstellt ein neues {@link RemoteSenderAsync} Object.
     */
    public RemoteSenderAsync()
    {
        super();

        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        // TODO Connection-Pool aufbauen !!!
        InetSocketAddress serverAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
        ByteBuffer buffer = getBuffer();

        try
        {
            // int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            // this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(poolSize));
            this.channelGroup = AsynchronousChannelGroup.withThreadPool(this.executorService);

            this.asyncSocketChannel = AsynchronousSocketChannel.open(this.channelGroup);
            // this.client = AsynchronousSocketChannel.open();

            Future<Void> futureConnect = this.asyncSocketChannel.connect(serverAddress);
            futureConnect.get();

            buffer.clear();

            // JSyncCommand senden.
            Serializers.writeTo(buffer, JSyncCommand.CONNECT);

            buffer.flip();
            write(buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
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
            releaseBuffer(buffer);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#disconnect()
     */
    @Override
    public void disconnect()
    {
        try
        {
            this.asyncSocketChannel.shutdownInput();
            this.asyncSocketChannel.shutdownOutput();
            this.asyncSocketChannel.close();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        if (this.channelGroup != null)
        {
            JSyncUtils.shutdown(this.channelGroup, getLogger());
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        ByteBuffer buffer = getBuffer();

        try
        {
            buffer.clear();
            Serializers.writeTo(buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(buffer);

            // Response lesen.
            buffer.clear();
            Future<Integer> futureResponse = this.asyncSocketChannel.read(buffer);

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
                futureResponse = this.asyncSocketChannel.read(buffer);
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
            releaseBuffer(buffer);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(buffer);
        releaseBuffer(buffer);

        return new NoCloseReadableByteChannel(this.asyncSocketChannel);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.SOURCE_CHECKSUM);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(buffer);

        buffer.clear();
        Future<Integer> futureResponse = this.asyncSocketChannel.read(buffer);

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
        releaseBuffer(buffer);

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
     * @param buffer {@link ByteBuffer}
     */
    protected void write(final ByteBuffer buffer)
    {
        Future<Integer> futureRequest = this.asyncSocketChannel.write(buffer);

        try
        {
            while (futureRequest.get() > 0)
            {
                futureRequest = this.asyncSocketChannel.write(buffer);
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
