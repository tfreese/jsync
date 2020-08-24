// Created: 18.11.2018
package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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
            return this.delegate.read(dst);
        }
    }

    /**
     *
     */
    private final ByteBuffer buffer;

    /**
     *
     */
    private SocketChannel socketChannel;

    /**
     * Erstellt ein neues {@link RemoteSenderBlocking} Object.
     */
    public RemoteSenderBlocking()
    {
        super();

        // this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
        this.buffer = ByteBuffer.allocate(Options.BUFFER_SIZE);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        InetSocketAddress serverAddress = new InetSocketAddress(uri.getHost(), uri.getPort());

        try
        {
            // this.socketChannel = SocketChannel.open();
            // this.socketChannel.connect(serverAddress);
            this.socketChannel = SocketChannel.open(serverAddress);
            this.socketChannel = (SocketChannel) this.socketChannel.configureBlocking(true);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.CONNECT);

        this.buffer.flip();
        write(this.buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.DISCONNECT);

        this.buffer.flip();
        write(this.buffer);

        try
        {
            this.socketChannel.shutdownInput();
            this.socketChannel.shutdownOutput();
            this.socketChannel.close();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        // Callable<Void> callable = () -> {
        try
        {
            this.buffer.clear();
            Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
            Serializers.writeTo(this.buffer, baseDir);
            Serializers.writeTo(this.buffer, followSymLinks);

            this.buffer.flip();
            write(this.buffer);

            // Response lesen.
            this.buffer.clear();

            // ByteArrayOutputStream baos = new ByteArrayOutputStream(Options.BUFFER_SIZE);

            while (this.socketChannel.read(this.buffer) > 0)
            {
                this.buffer.flip();

                while (this.buffer.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(this.buffer, SyncItem.class);
                    consumerSyncItem.accept(syncItem);

                    // byte[] bytes = new byte[this.buffer.remaining()];
                    // baos.writeBytes(bytes);
                }

                // this.buffer.clear();
            }

            // ByteBuffer holeDataBuffer = ByteBuffer.wrap(baos.toByteArray());
            //
            // while (holeDataBuffer.hasRemaining())
            // {
            // SyncItem syncItem = Serializers.readFrom(holeDataBuffer, SyncItem.class);
            // consumerSyncItem.accept(syncItem);
            // }
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

        // return null;
        // };
        //
        // try
        // {
        // callable.call();
        // }
        // catch (Exception ex)
        // {
        // ex.printStackTrace();
        // }

        // Future<Void> future = ForkJoinPool.commonPool().submit(callable);
        //
        // try
        // {
        // future.get();
        // }
        // catch (InterruptedException | ExecutionException ex)
        // {
        // ex.printStackTrace();
        // }
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public ReadableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, relativeFile);

        this.buffer.flip();
        write(this.buffer);

        return new NoCloseReadableByteChannel(this.socketChannel);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.SOURCE_CHECKSUM);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, relativeFile);

        this.buffer.flip();
        write(this.buffer);

        this.buffer.clear();
        read(this.buffer);
        this.buffer.flip();

        String checksum = Serializers.readFrom(this.buffer, String.class);

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
     * @param buf {@link ByteBuffer}
     */
    protected void read(final ByteBuffer buf)
    {
        try
        {
            this.socketChannel.read(this.buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param buf {@link ByteBuffer}
     */
    protected void write(final ByteBuffer buf)
    {
        try
        {
            while (buf.hasRemaining())
            {
                this.socketChannel.write(buf);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
