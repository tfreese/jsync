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
@SuppressWarnings("resource")
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
    private final ByteBuffer buffer;

    /**
    *
    */
    private SocketChannel socketChannel = null;

    /**
     * Erzeugt eine neue Instanz von {@link RemoteReceiverBlocking}.
     */
    public RemoteReceiverBlocking()
    {
        super();

        this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
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
            this.socketChannel = SocketChannel.open();
            this.socketChannel.connect(serverAddress);
            this.socketChannel.configureBlocking(true);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteDirectory(final String baseDir, final String relativeDir)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_DIRECTORY);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, relativeDir);

        this.buffer.flip();
        write(this.buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String, java.lang.String)
     */
    @Override
    public void deleteFile(final String baseDir, final String relativeFile)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_FILE);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, relativeFile);

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
        try
        {
            this.buffer.clear();
            Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);
            Serializers.writeTo(this.buffer, baseDir);
            this.buffer.put(followSymLinks ? (byte) 1 : (byte) 0);

            this.buffer.flip();
            write(this.buffer);

            // Response lesen.
            boolean sizeRead = false;
            boolean finished = false;
            int countSyncItems = -1;
            int counter = 0;

            this.buffer.clear();

            while (this.socketChannel.read(this.buffer) > 0)
            {
                this.buffer.flip();

                if (!sizeRead)
                {
                    countSyncItems = this.buffer.getInt();
                    sizeRead = true;
                }

                while (this.buffer.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(this.buffer, SyncItem.class);
                    consumerSyncItem.accept(syncItem);
                    counter++;

                    if (counter == countSyncItems)
                    {
                        // Finish-Flag.
                        finished = true;
                        break;
                    }
                }

                if (finished)
                {
                    break;
                }

                this.buffer.clear();
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
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(java.lang.String, java.lang.String)
     */
    @Override
    public WritableByteChannel getChannel(final String baseDir, final String relativeFile)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, relativeFile);

        this.buffer.flip();
        write(this.buffer);

        return new NoCloseWritableByteChannel(this.socketChannel);
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
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CHECKSUM);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final String baseDir, final SyncItem syncItem)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_DIRECTORY);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, syncItem);

        this.buffer.flip();
        write(this.buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(java.lang.String, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final String baseDir, final SyncItem syncItem)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_FILE);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, syncItem);

        this.buffer.flip();
        write(this.buffer);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(java.lang.String, de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final String baseDir, final SyncItem syncItem, final boolean withChecksum)
    {
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_VALIDATE_FILE);
        Serializers.writeTo(this.buffer, baseDir);
        Serializers.writeTo(this.buffer, syncItem);
        this.buffer.put(withChecksum ? (byte) 1 : (byte) 0);

        this.buffer.flip();
        write(this.buffer);
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
