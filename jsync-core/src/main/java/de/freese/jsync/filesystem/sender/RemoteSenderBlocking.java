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
    private SocketChannel socketChannel;

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
        // TODO Connection-Pool aufbauen !!!
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

        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.CONNECT);

        buffer.flip();
        write(buffer);

        releaseBuffer(buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect()
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.DISCONNECT);

        buffer.flip();
        write(buffer);

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
        finally
        {
            releaseBuffer(buffer);
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

            int bytesRead = 0;

            while ((bytesRead = this.socketChannel.read(buffer)) != 1)
            {
                buffer.flip();

                while (buffer.remaining() > 3)
                {
                    SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
                    consumerSyncItem.accept(syncItem);
                }

                if (Serializers.isEOL(buffer))
                {
                    break;
                }

                buffer.clear();
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

        return new NoCloseReadableByteChannel(this.socketChannel);
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
        read(buffer);
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
    protected void read(final ByteBuffer buffer)
    {
        try
        {
            this.socketChannel.read(buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    protected void write(final ByteBuffer buffer)
    {
        try
        {
            while (buffer.hasRemaining())
            {
                this.socketChannel.write(buffer);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
