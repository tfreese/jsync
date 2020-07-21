/**
 * Created: 18.11.2018
 */

package de.freese.jsync.filesystem.sender;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.LongConsumer;
import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.server.JSyncCommand;
import de.freese.jsync.utils.JSyncUtils;

/**
 * {@link Sender} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 * @deprecated Entfällt
 */
@Deprecated
@SuppressWarnings("resource")
class RemoteSenderAsync extends AbstractSender
{
    /**
     * @author Thomas Freese
     */
    private class AsyncReadableByteChannelAdapter implements ReadableByteChannel
    {
        /**
         *
         */
        private final AsynchronousSocketChannel asynchronousSocketChannel;

        /**
         * Erstellt ein neues {@link AsyncReadableByteChannelAdapter} Object.
         *
         * @param asynchronousSocketChannel {@link AsynchronousSocketChannel}
         */
        public AsyncReadableByteChannelAdapter(final AsynchronousSocketChannel asynchronousSocketChannel)
        {
            super();

            this.asynchronousSocketChannel = Objects.requireNonNull(asynchronousSocketChannel, "asynchronousSocketChannel required");
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
            return this.asynchronousSocketChannel.isOpen();
        }

        /**
         * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
         */
        @Override
        public int read(final ByteBuffer dst) throws IOException
        {
            Future<Integer> futureRead = this.asynchronousSocketChannel.read(dst);

            try
            {
                return futureRead.get();
            }
            catch (InterruptedException | ExecutionException ex)
            {
                if (ex instanceof IOException)
                {
                    throw (IOException) ex;
                }

                throw new IOException(ex);
            }
        }
    }

    /**
    *
    */
    private final ByteBuffer buffer;

    /**
    *
    */
    private AsynchronousChannelGroup channelGroup = null;

    /**
    *
    */
    private AsynchronousSocketChannel client = null;

    /**
     *
     */
    private final ExecutorService executorService;

    /**
     * Erstellt ein neues {@link RemoteSenderAsync} Object.
     *
     * @param serverUri {@link URI}
     */
    RemoteSenderAsync(final URI serverUri)
    {
        super(serverUri);

        this.executorService = Executors.newCachedThreadPool();
        this.buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#connect()
     */
    @Override
    public void connect() throws Exception
    {
        InetSocketAddress serverAddress = new InetSocketAddress(getBaseUri().getHost(), getBaseUri().getPort());

        // this.client = SocketChannel.open(serverAddress);
        // this.client.configureBlocking(true);

        // int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        // this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(poolSize));
        this.channelGroup = AsynchronousChannelGroup.withThreadPool(this.executorService);

        this.client = AsynchronousSocketChannel.open(this.channelGroup);
        // this.client = AsynchronousSocketChannel.open();

        Future<Void> futureConnect = this.client.connect(serverAddress);
        futureConnect.get();

        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        Serializers.writeTo(buf, JSyncCommand.CONNECT);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#disconnect()
     */
    @Override
    public void disconnect() throws Exception
    {
        getClient().shutdownInput();
        getClient().shutdownOutput();
        getClient().close();

        if (this.channelGroup != null)
        {
            JSyncUtils.shutdown(this.channelGroup, getLogger());
        }
    }

    /**
     * @return {@link ByteBuffer}
     */
    protected ByteBuffer getBuffer()
    {
        this.buffer.clear();

        return this.buffer;
    }

    /**
     * @see de.freese.jsync.filesystem.sender.Sender#getChannel(de.freese.jsync.model.SyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final SyncItem syncItem) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        Serializers.writeTo(buf, JSyncCommand.SOURCE_READABLE_FILE_CHANNEL);

        buf.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        buf.flip();
        write(buf);

        return new AsyncReadableByteChannelAdapter(getClient());
    }

    /**
     * @return {@link Charset}
     */
    protected Charset getCharset()
    {
        return JSyncCommandSerializer.getInstance().getCharset();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChecksum(java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String getChecksum(final String relativePath, final LongConsumer consumerBytesRead) throws Exception
    {
        String checksum = null;

        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        Serializers.writeTo(buf, JSyncCommand.SOURCE_CHECKSUM);

        byte[] bytes = relativePath.getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);
        buf.flip();
        write(buf);

        // Response lesen.
        buf.clear();
        Future<Integer> futureResponse = getClient().read(buf);
        futureResponse.get();

        buf.flip();

        bytes = new byte[buf.getInt()];
        buf.get(bytes);
        checksum = new String(bytes, getCharset());

        return checksum;
    }

    /**
     * @return {@link AsynchronousSocketChannel}
     */
    protected AsynchronousSocketChannel getClient()
    {
        return this.client;
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
            ByteBuffer buf = getBuffer();

            // JSyncCommand senden.
            Serializers.writeTo(buf, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);

            byte[] bytes = getBasePath().toString().getBytes(getCharset());
            buf.putInt(bytes.length);
            buf.put(bytes);

            buf.put(followSymLinks ? (byte) 1 : (byte) 0);

            buf.flip();
            write(buf);

            // Response lesen.
            buf.clear();
            Future<Integer> futureResponse = getClient().read(buf);

            boolean sizeRead = false;
            boolean finished = false;
            int countSyncItems = -1;
            int counter = 0;

            // while (getClient().read(buffer) > 0)
            while (futureResponse.get() > 0)
            {
                buf.flip();

                if (!sizeRead)
                {
                    countSyncItems = buf.getInt();
                    sizeRead = true;
                }

                while (buf.hasRemaining())
                {
                    SyncItem syncItem = Serializers.readFrom(buf, SyncItem.class);
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

                buf.clear();
                futureResponse = getClient().read(buf);
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
        ByteBuffer buf = getBuffer();

        Future<Integer> futureResponse = getClient().read(buf);

        futureResponse.get();

        buf.flip();

        byte b = buf.get();

        if (b != Byte.MIN_VALUE)
        {
            // TODO Fehlerhandling
        }
    }

    /**
     * @param buf {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     */
    protected void write(final ByteBuffer buf) throws Exception
    {
        // while (buf.hasRemaining())
        // {
        // getClient().write(buf);
        // }

        Future<Integer> futureRequest = getClient().write(buf);

        while (futureRequest.get() > 0)
        {
            futureRequest = getClient().write(buf);
        }
    }
}
