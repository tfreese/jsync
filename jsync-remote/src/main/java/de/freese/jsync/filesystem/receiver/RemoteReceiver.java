// Created: 05.04.2018
package de.freese.jsync.filesystem.receiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import de.freese.jsync.Options;
import de.freese.jsync.generator.listener.GeneratorListener;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.serializer.JSyncCommandSerializer;
import de.freese.jsync.model.serializer.Serializers;
import de.freese.jsync.server.JSyncCommand;
import de.freese.jsync.utils.JSyncUtils;

/**
 * {@link Receiver} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class RemoteReceiver extends AbstractReceiver
{
    /**
     * @author Thomas Freese
     */
    private class AsyncWritableByteChannelAdapter implements WritableByteChannel
    {
        /**
         *
         */
        private final AsynchronousSocketChannel asynchronousSocketChannel;

        /**
         * Erstellt ein neues {@link AsyncWritableByteChannelAdapter} Object.
         *
         * @param asynchronousSocketChannel {@link AsynchronousSocketChannel}
         */
        public AsyncWritableByteChannelAdapter(final AsynchronousSocketChannel asynchronousSocketChannel)
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
            // Diese Methode wird unmittelbar nach dem Kopieren aufgerufen.
            // Daher muss erst mal auf das Finish vom Server gewartet werden.
            try
            {
                handleResponse();
            }
            catch (IOException iex)
            {
                throw iex;
            }
            catch (Exception ex)
            {
                throw new IOException(ex);
            }
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
         * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
         */
        @Override
        public int write(final ByteBuffer src) throws IOException
        {
            Future<Integer> futureWrite = this.asynchronousSocketChannel.write(src);

            try
            {
                return futureWrite.get();
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
    private final Path base;

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
    private final URI serverUri;

    /**
     * Erzeugt eine neue Instanz von {@link RemoteReceiver}.
     *
     * @param options {@link Options}
     * @param serverUri {@link URI}
     */
    public RemoteReceiver(final Options options, final URI serverUri)
    {
        super(options);

        this.serverUri = Objects.requireNonNull(serverUri, "serverUri required");
        this.base = Paths.get(JSyncUtils.normalizedPath(serverUri));
        this.buffer = ByteBuffer.allocateDirect(getOptions().getBufferSize());
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#connect()
     */
    @Override
    public void connect() throws Exception
    {
        InetSocketAddress hostAddress = new InetSocketAddress(getServerUri().getHost(), getServerUri().getPort());

        // this.client = SocketChannel.open(hostAddress);
        // this.client.configureBlocking(true);

        // int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        // this.channelGroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(poolSize));
        this.channelGroup = AsynchronousChannelGroup.withThreadPool(getOptions().getExecutorService());

        this.client = AsynchronousSocketChannel.open(this.channelGroup);
        // this.client = AsynchronousSocketChannel.open();

        Future<Void> futureConnect = this.client.connect(hostAddress);
        futureConnect.get();

        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.CONNECT);

        // Options senden.
        Serializers.writeTo(buf, getOptions());

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#createDirectory(java.lang.String)
     */
    @Override
    public void createDirectory(final String dir) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_CREATE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#createSyncItems(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public NavigableMap<String, SyncItem> createSyncItems(final GeneratorListener listener)
    {
        NavigableMap<String, SyncItem> syncItems = new TreeMap<>();

        try
        {
            ByteBuffer buf = getBuffer();

            // JSyncCommand senden.
            JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);

            byte[] bytes = getBase().toString().getBytes(getCharset());
            buf.putInt(bytes.length);
            buf.put(bytes);

            buf.flip();
            write(buf);

            buf.clear();

            // Response lesen.
            Future<Integer> futureResponse = getClient().read(buf);

            boolean sizeRead = false;
            boolean finished = false;

            // while (getClient().read(buffer) > 0)
            while (futureResponse.get() > 0)
            {
                buf.flip();

                if (!sizeRead)
                {
                    int size = buf.getInt();
                    sizeRead = true;

                    if (listener != null)
                    {
                        listener.pathCount(getBase(), size);
                    }
                }

                while (buf.hasRemaining())
                {
                    SyncItem syncItem = null;

                    byte b = buf.get();

                    if (b == 0)
                    {
                        syncItem = Serializers.readFrom(buf, FileSyncItem.class);
                    }
                    else if (b == 1)
                    {
                        syncItem = Serializers.readFrom(buf, DirectorySyncItem.class);
                    }
                    else if (b == Byte.MIN_VALUE)
                    {
                        // Finish-Flag.
                        finished = true;
                        break;
                    }

                    if (listener != null)
                    {
                        listener.processingSyncItem(syncItem);
                    }

                    syncItems.put(syncItem.getRelativePath(), syncItem);
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteDirectory(java.lang.String)
     */
    @Override
    public void deleteDirectory(final String dir) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_DELETE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(final String file) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_DELETE_FILE);

        byte[] bytes = file.getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#disconnect()
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
     * @return {@link Path}
     */
    protected Path getBase()
    {
        return this.base;
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);

        buf.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        // if (syncItem.getChecksum() == null)
        // {
        // buf.put((byte) 0);
        // }
        // else
        // {
        // buf.put((byte) 1);
        // bytes = syncItem.getChecksum().getBytes(getCharset());
        // buf.putInt(bytes.length);
        // buf.put(bytes);
        // }

        buf.flip();
        write(buf);

        return new AsyncWritableByteChannelAdapter(getClient());
    }

    /**
     * @return {@link Charset}
     */
    protected Charset getCharset()
    {
        return JSyncCommandSerializer.getInstance().getCharset();
    }

    /**
     * @return {@link AsynchronousSocketChannel}
     */
    protected AsynchronousSocketChannel getClient()
    {
        return this.client;
    }

    /**
     * @return {@link URI}
     */
    protected URI getServerUri()
    {
        return this.serverUri;
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
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateDirectory(de.freese.jsync.model.DirectorySyncItem)
     */
    @Override
    public void updateDirectory(final DirectorySyncItem syncItem) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_UPDATE_DIRECTORY);

        Serializers.writeTo(buf, syncItem);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#updateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void updateFile(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_UPDATE_FILE);

        Serializers.writeTo(buf, syncItem);

        buf.flip();
        write(buf);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.receiver.Receiver#validateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void validateFile(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buf = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buf, JSyncCommand.TARGET_VALIDATE_FILE);

        buf.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        buf.putInt(bytes.length);
        buf.put(bytes);

        if (syncItem.getChecksum() == null)
        {
            buf.put((byte) 0);
        }
        else
        {
            buf.put((byte) 1);
            bytes = syncItem.getChecksum().getBytes(getCharset());
            buf.putInt(bytes.length);
            buf.put(bytes);
        }

        buf.flip();
        write(buf);

        handleResponse();
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
