// Created: 05.04.2018
package de.freese.jsync.filesystem.destination;

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
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
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
import de.freese.jsync.util.JSyncUtils;

/**
 * {@link Target} für Remote-Filesysteme.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class RemoteTarget extends AbstractTarget
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
            catch (Exception ex)
            {
                if (ex instanceof IOException)
                {
                    throw (IOException) ex;
                }

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
     * Erzeugt eine neue Instanz von {@link RemoteTarget}.
     *
     * @param options {@link Options}
     * @param serverUri {@link URI}
     */
    public RemoteTarget(final Options options, final URI serverUri)
    {
        super(options);

        this.serverUri = Objects.requireNonNull(serverUri, "serverUri required");
        this.base = Paths.get(JSyncUtils.normalizedPath(serverUri));
        this.buffer = ByteBuffer.allocateDirect(getOptions().getBufferSize());
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#connect()
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

        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.CONNECT);

        // Options senden.
        Serializers.writeTo(buffer, getOptions());

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#createDirectory(java.lang.String)
     */
    @Override
    public void createDirectory(final String dir) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_CREATE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#createSyncItems(de.freese.jsync.generator.listener.GeneratorListener)
     */
    @Override
    public Callable<Map<String, SyncItem>> createSyncItems(final GeneratorListener listener)
    {
        return () -> {
            Map<String, SyncItem> syncItems = new TreeMap<>();
            ByteBuffer buffer = getBuffer();

            // JSyncCommand senden.
            JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_CREATE_SYNC_ITEMS);

            byte[] bytes = getBase().toString().getBytes(getCharset());
            buffer.putInt(bytes.length);
            buffer.put(bytes);

            buffer.flip();
            write(buffer);

            buffer.clear();

            // Response lesen.
            Future<Integer> futureResponse = getClient().read(buffer);

            boolean sizeRead = false;
            boolean finished = false;

            // while (getClient().read(buffer) > 0)
            while (futureResponse.get() > 0)
            {
                buffer.flip();

                if (!sizeRead)
                {
                    int size = buffer.getInt();
                    sizeRead = true;

                    if (listener != null)
                    {
                        listener.pathCount(getBase(), size);
                    }
                }

                while (buffer.hasRemaining())
                {
                    SyncItem syncItem = null;

                    byte b = buffer.get();

                    if (b == 0)
                    {
                        syncItem = Serializers.readFrom(buffer, FileSyncItem.class);
                    }
                    else if (b == 1)
                    {
                        syncItem = Serializers.readFrom(buffer, DirectorySyncItem.class);
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

                buffer.clear();
                futureResponse = getClient().read(buffer);
            }

            return syncItems;
        };
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#deleteDirectory(java.lang.String)
     */
    @Override
    public void deleteDirectory(final String dir) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_DELETE_DIRECTORY);

        byte[] bytes = dir.getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#deleteFile(java.lang.String)
     */
    @Override
    public void deleteFile(final String file) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_DELETE_FILE);

        byte[] bytes = file.getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#disconnect()
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
     * @see de.freese.jsync.filesystem.destination.Target#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public WritableByteChannel getChannel(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_WRITEABLE_FILE_CHANNEL);

        buffer.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        // if (syncItem.getChecksum() == null)
        // {
        // buffer.put((byte) 0);
        // }
        // else
        // {
        // buffer.put((byte) 1);
        // bytes = syncItem.getChecksum().getBytes(getCharset());
        // buffer.putInt(bytes.length);
        // buffer.put(bytes);
        // }

        buffer.flip();
        write(buffer);

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
        ByteBuffer buffer = getBuffer();

        Future<Integer> futureResponse = getClient().read(buffer);

        futureResponse.get();

        buffer.flip();

        byte b = buffer.get();

        if (b != Byte.MIN_VALUE)
        {
            // TODO Fehlerhandling
        }
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#updateDirectory(de.freese.jsync.model.DirectorySyncItem)
     */
    @Override
    public void updateDirectory(final DirectorySyncItem syncItem) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_UPDATE_DIRECTORY);

        Serializers.writeTo(buffer, syncItem);

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#updateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void updateFile(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_UPDATE_FILE);

        Serializers.writeTo(buffer, syncItem);

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @see de.freese.jsync.filesystem.destination.Target#validateFile(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public void validateFile(final FileSyncItem syncItem) throws Exception
    {
        ByteBuffer buffer = getBuffer();

        // JSyncCommand senden.
        JSyncCommandSerializer.getInstance().writeTo(buffer, JSyncCommand.TARGET_VALIDATE_FILE);

        buffer.putLong(syncItem.getSize());

        byte[] bytes = syncItem.getRelativePath().getBytes(getCharset());
        buffer.putInt(bytes.length);
        buffer.put(bytes);

        if (syncItem.getChecksum() == null)
        {
            buffer.put((byte) 0);
        }
        else
        {
            buffer.put((byte) 1);
            bytes = syncItem.getChecksum().getBytes(getCharset());
            buffer.putInt(bytes.length);
            buffer.put(bytes);
        }

        buffer.flip();
        write(buffer);

        handleResponse();
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @throws Exception Falls was schief geht.
     */
    protected void write(final ByteBuffer buffer) throws Exception
    {
        // while (buffer.hasRemaining())
        // {
        // getClient().write(buffer);
        // }

        Future<Integer> futureRequest = getClient().write(buffer);

        while (futureRequest.get() > 0)
        {
            futureRequest = getClient().write(buffer);
        }
    }
}
