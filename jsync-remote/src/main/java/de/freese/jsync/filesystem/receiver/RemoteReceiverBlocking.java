// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.server.JSyncCommand;

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
     *
     * @param serverUri {@link URI}
     */
    public RemoteReceiverBlocking(final URI serverUri)
    {
        super(serverUri);

        this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#connect()
     */
    @Override
    public void connect() throws Exception
    {
        InetSocketAddress serverAddress = new InetSocketAddress(getBaseUri().getHost(), getBaseUri().getPort());

        this.socketChannel = SocketChannel.open();
        this.socketChannel.connect(serverAddress);
        this.socketChannel.configureBlocking(true);

        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.CONNECT);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String)
     */
    @Override
    public void createDirectory(final String dir) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String)
     */
    @Override
    public void deleteDirectory(final String dir) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(final String file) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_DELETE_FILE);

        byte[] bytes = file.getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.DISCONNECT);

        this.buffer.flip();
        write(this.buffer);

        this.socketChannel.shutdownInput();
        this.socketChannel.shutdownOutput();
        this.socketChannel.close();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(de.freese.jsync.model.SyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final SyncItem syncItem) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);

        this.buffer.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

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
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String relativePath, final LongConsumer consumerBytesRead) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CHECKSUM);

        byte[] bytes = relativePath.getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

        this.buffer.flip();
        write(this.buffer);

        // Response lesen.
        this.buffer.clear();
        this.socketChannel.read(this.buffer);
        this.buffer.flip();

        bytes = new byte[this.buffer.getInt()];
        this.buffer.get(bytes);
        String checksum = new String(bytes, getCharset());

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getSyncItems(boolean)
     */
    @Override
    public List<SyncItem> getSyncItems(final boolean followSymLinks)
    {
        List<SyncItem> syncItems = new ArrayList<>();

        try
        {
            // JSyncCommand senden.
            this.buffer.clear();
            Serializers.writeTo(this.buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);

            byte[] bytes = getBasePath().toString().getBytes(getCharset());
            this.buffer.putInt(bytes.length);
            this.buffer.put(bytes);

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
                    syncItems.add(syncItem);
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
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return syncItems;
    }

    /**
     * Finish-Flag (Byte.MIN_VALUE) abwarten oder Fehlerhandling.
     *
     * @throws Exception Falls was schief geht.
     */
    protected void handleResponse() throws Exception
    {
        // this.buffer.clear();
        // this.socketChannel.read(this.buffer);
        // this.buffer.flip();
        //
        // byte b = this.buffer.get();
        //
        // if (b != Byte.MIN_VALUE)
        // {
        // // TODO Fehlerhandling
        // }
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final SyncItem syncItem) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_DIRECTORY);

        Serializers.writeTo(this.buffer, syncItem);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final SyncItem syncItem) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_UPDATE_FILE);

        Serializers.writeTo(this.buffer, syncItem);

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(de.freese.jsync.model.SyncItem, boolean)
     */
    @Override
    public void validateFile(final SyncItem syncItem, final boolean withChecksum) throws Exception
    {
        // JSyncCommand senden.
        this.buffer.clear();
        Serializers.writeTo(this.buffer, JSyncCommand.TARGET_VALIDATE_FILE);

        this.buffer.putLong(syncItem.getSize());
        this.buffer.put(withChecksum ? (byte) 1 : (byte) 0);

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        this.buffer.putInt(bytes.length);
        this.buffer.put(bytes);

        if (syncItem.getChecksum() == null)
        {
            this.buffer.put((byte) 0);
        }
        else
        {
            this.buffer.put((byte) 1);
            bytes = syncItem.getChecksum().getBytes(getCharset());
            this.buffer.putInt(bytes.length);
            this.buffer.put(bytes);
        }

        this.buffer.flip();
        write(this.buffer);

        handleResponse();
    }

    /**
     * @param buf {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     */
    protected void write(final ByteBuffer buf) throws Exception
    {
        while (buf.hasRemaining())
        {
            this.socketChannel.write(buf);
        }
    }
}
