// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
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
         * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
         */
        @Override
        public int write(final ByteBuffer src) throws IOException
        {
            return this.delegate.write(src);
        }
    }

    /**
     *
     */
    private SocketChannel socketChannel;

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
        // TODO Connection-Pool aufbauen !!!
        InetSocketAddress serverAddress = new InetSocketAddress(uri.getHost(), uri.getPort());
        ByteBuffer buffer = getBuffer();

        try
        {
            this.socketChannel = SocketChannel.open();
            this.socketChannel.connect(serverAddress);
            this.socketChannel.configureBlocking(true);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.CONNECT);

        buffer.flip();
        write(buffer);

        releaseBuffer(buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void createDirectory(final String baseDir, final String relativePath)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativePath);

        buffer.flip();
        write(buffer);
        releaseBuffer(buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#delete(java.lang.String, java.lang.String, boolean)
     */
    @Override
    public void delete(final String baseDir, final String relativePath, final boolean followSymLinks)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_DELETE);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativePath);
        Serializers.writeTo(buffer, followSymLinks);

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
            Serializers.writeTo(buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
            Serializers.writeTo(buffer, baseDir);
            Serializers.writeTo(buffer, followSymLinks);

            buffer.flip();
            write(buffer);

            // Response lesen.
            buffer.clear();

            while (this.socketChannel.read(buffer) > 0)
            {
                buffer.flip();

                while (buffer.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(buffer, SyncItem.class);
                    consumerSyncItem.accept(syncItem);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, relativeFile);

        buffer.flip();
        write(buffer);
        releaseBuffer(buffer);

        return new NoCloseWritableByteChannel(this.socketChannel);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerBytesRead)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_CHECKSUM);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#update(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final String baseDir, final SyncItem syncItem)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_UPDATE);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, syncItem);

        buffer.flip();
        write(buffer);
        releaseBuffer(buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        ByteBuffer buffer = getBuffer();
        buffer.clear();
        Serializers.writeTo(buffer, JSyncCommand.TARGET_VALIDATE_FILE);
        Serializers.writeTo(buffer, baseDir);
        Serializers.writeTo(buffer, syncItem);
        Serializers.writeTo(buffer, withChecksum);

        buffer.flip();
        write(buffer);
        releaseBuffer(buffer);
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
